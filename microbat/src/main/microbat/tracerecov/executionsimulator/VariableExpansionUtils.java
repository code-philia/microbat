package microbat.tracerecov.executionsimulator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import microbat.model.trace.TraceNode;
import microbat.model.value.ArrayValue;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.StringValue;
import microbat.model.value.VarValue;
import microbat.model.variable.FieldVar;
import microbat.model.variable.Variable;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.ExampleSearcher;
import microbat.tracerecov.autoprompt.VarExpansionExampleSearcher;
import microbat.tracerecov.varskeleton.VariableSkeleton;
import sav.common.core.Pair;

public class VariableExpansionUtils {

	/* Request content */

	private static final String VAR_EXPAND_BACKGROUND = 
			"<Background>\r\n"
			+ "When executing a Java third-party library, some of its internal variables are critical for debugging. Please identify the most critical internal variables of a Java data structure for debugging. \r\n"
			+ "\r\n";

	private static final String VAR_EXPAND_EXAMPLE = 
			"<Example>\r\n"
			+ "Class Name: java.util.HashMap<HashMap, ArrayList>\r\n"
			+ "Structure: {\r\n"
			+ "	java.util.HashMap$Node[] table;\r\n"
			+ "	java.util.Set entrySet;\r\n"
			+ "	int size;\r\n"
			+ "	int modCount;\r\n"
			+ "	int threshold;\r\n"
			+ "	float loadFactor;\r\n"
			+ "	java.util.Set keySet;\r\n"
			+ "	java.util.Collection values;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ "Given “map.toString()” has output value:\r\n"
			+ "{{k1=1, k2=2} = [v1, v2], {kA=100, kB=200} = [vA, vB]}\r\n"
			+ "\r\n"
			+ "We can summarize the structure as \r\n"
			+ "Here is the given structure converted to JSON format (every variable shall strictly have a Java type, followed by its value):\r\n"
			+ "{\r\n"
			+ "  \"map| java.util.HashMap<HashMap, ArrayList>\": {\r\n"
			+ "    \"key[0]| java.util.HashMap\": {\r\n"
			+ "      \"map\":{\r\n"
			+ "        \"key[0]| java.lang.String\": \"k1\",\r\n"
			+ "        \"key[1]| java.lang.String\": \"k2\",\r\n"
			+ "        \"value[0]| java.lang.Integer\": 1,\r\n"
			+ "        \"value[1]| java.lang.Integer\": 2,\r\n"
			+ "        \"size| int\": 2	\r\n"
			+ "       }\r\n"
			+ "    },\r\n"
			+ "    \"key[1]| java.util.HashMap\": {\r\n"
			+ "      \"map\": {\r\n"
			+ "        \"key[0]| java.lang.String\": \"kA\",\r\n"
			+ "        \"key[1]| java.lang.String\": \"kB\",\r\n"
			+ "        \"value[0]| java.lang.Integer\": 100,\r\n"
			+ "        \"value[1]| java.lang.Integer\": 200,\r\n"
			+ "         \"size| int\": 2\r\n"
			+ "       }\r\n"
			+ "     },\r\n"
			+ "     \"value[0]| java.util.ArrayList<String>\": { \r\n"
			+ "			\"elementData| java.lang.Object[]\": [ \"v1\", \"v2\"], \r\n"
			+ "  		\"size| int\": 2 \r\n"
			+ "       }, \r\n"
			+ "     \"value[1]| java.util.ArrayList<String>\": { \r\n"
			+ "			\"elementData| java.lang.Object[]\": [ \"vA\", \"vB\"], \r\n"
			+ "  		\"size| int\": 2 \r\n"
			+ "       },\r\n"
			+ "     \"size| int\": 2\r\n"
			+ "  },\r\n"
			+ " }";

	/* Methods */

	public static String getBackgroundContent() {
		return VAR_EXPAND_BACKGROUND + VAR_EXPAND_EXAMPLE;
	}

	public static String getBackgroundContent(VarValue varValue, VariableSkeleton varSkeleton, TraceNode step) {
		return VAR_EXPAND_BACKGROUND + getExample(varValue, varSkeleton, step);
	}

	private static HashMap<String, String> getDatapointFromStep(VarValue varValue, VariableSkeleton varSkeleton,
			TraceNode step) {
		HashMap<String, String> datapoint = new HashMap<>();

		datapoint.put("var_name", varValue.getVarName());
		datapoint.put("var_type", varValue.getType());
		datapoint.put("var_value", TraceRecovUtils.processInputStringForLLM(varValue.getStringValue()));
		datapoint.put("class_structure", varSkeleton.toString());
		datapoint.put("source_code", getSourceCode(step));
		datapoint.put("ground_truth", ""); // not available yet

		return datapoint;
	}

	private static String getExample(VarValue varValue, VariableSkeleton varSkeleton, TraceNode step) {
		HashMap<String, String> datapoint = getDatapointFromStep(varValue, varSkeleton, step);

		ExampleSearcher exampleSearcher = new VarExpansionExampleSearcher();
		String closestExample = exampleSearcher.searchForExample(datapoint);

		if (closestExample == null || closestExample.equals("")) {
			return VAR_EXPAND_EXAMPLE;
		}
		return closestExample;
	}

	private static String getSourceCode(TraceNode step) {
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils
				.processInputStringForLLM(TraceRecovUtils.getSourceCodeOfALine(location, lineNo).trim());
		return sourceCode;
	}

	public static String getQuestionContent(VarValue selectedVariable, List<VariableSkeleton> variableSkeletons,
			TraceNode step, Pair<String, String> preValueResponse) {
		/* source code */
		String sourceCode = getSourceCode(step);

		/* type of selected variable */
		String variableType = selectedVariable.getType();
		// assume var layer == 1, then only elementArray will be recorded in ArrayList
		if (!selectedVariable.getChildren().isEmpty()) {
			VarValue child = selectedVariable.getChildren().get(0);
			String childType = child.getType();
			if (childType.contains("[]")) {
				childType = childType.substring(0, childType.length() - 2); // remove [] at the end
			}
			variableType = variableType.concat("\\<" + childType + "\\>");
		}

		/* variable properties */
		String variableValue = TraceRecovUtils.processInputStringForLLM(selectedVariable.getStringValue());
		String variableName = selectedVariable.getVarName();

		StringBuilder question = new StringBuilder("\n\n<Question>\n" + "Given the following data structure:\n");

		for (VariableSkeleton v : variableSkeletons) {
			if (v != null) {
				question.append(v.toString() + "\n");
			}
		}


//		question.append("*BEFORE* executing ```");
//		question.append(sourceCode + "```, ");
		question.append("we have the value of *" + selectedVariable.getVarName() + "* of type `");
		question.append(variableType);
		question.append("`, value \"");
		question.append(variableValue);
		question.append("\", strictly return in JSON format for *" + variableName
				+ "* as the above example, each key must has a value and a type. "
				+ "The JSON object must start with variable *" + variableName
				+ "* as the root. Do not include explanation in your response.\n");

		question.append("You must follow the JSON format as \"var_name|var_type\": var_value. "
				+ "Do not include duplicate keys. Do not include extra characters like `\\t`, `\\n` or `\\r`. You must infer all var_value.");

//		/*
//		 * Added to enforce identical variable structure in buggy and correct trace
//		 */
//		if (preValueResponse != null) {
//			question.append("\n For example, you may return\n" + preValueResponse.second() + " when *"
//					+ selectedVariable.getVarName() + "* has the value: \"" + preValueResponse.first() + "\".");
//		}

		return question.toString();
	}

	/**
	 * Recursively parse JSON into the input VarValue.
	 */
	public static void processResponse(VarValue selectedVariable, String response) {
		int begin = response.indexOf("{");
		int end = response.lastIndexOf("}");
		response = response.substring(begin, end + 1);

		JSONObject variable = new JSONObject(response);

		processResponseRecur(true, variable, selectedVariable);
	}

	private static void processResponseRecur(boolean isRoot, JSONObject jsonObject, VarValue selectedVariable) {
		Iterator<String> keys = jsonObject.keys();
		while (keys.hasNext()) {
			String key = keys.next();

			if (isRoot) {
				Object value = jsonObject.get(key);
				if (value instanceof JSONObject) {
					processResponseRecur(false, (JSONObject) value, selectedVariable);
				} else if (value instanceof JSONArray) {
					processResponseRecur((JSONArray) value, selectedVariable);
				}
				break;
			} else {
				String[] nameAndType = key.split("\\|");
				String varName = nameAndType[0].trim();
				String varType = "";
				if (nameAndType.length == 2) {
					varType = nameAndType[1].trim();
				}

				Variable var = new FieldVar(false, varName, varType, varType);

				String headAddress = selectedVariable.getAliasVarID().equals("0") ? selectedVariable.getVarID()
						: selectedVariable.getAliasVarID();
				var.setVarID(Variable.concanateFieldVarID(headAddress, varName));

				Object value = jsonObject.get(key);
				VarValue varValue = null;

				if (value instanceof JSONArray || (value instanceof String && varType.contains("[]"))) {
					if (value instanceof String && varType.contains("[]")) {
						value = TraceRecovUtils.parseJSONArrayFromString((String) value);
					}

					varValue = new ArrayValue(false, false, var);
					varValue.setStringValue(String.valueOf(value));

					processResponseRecur((JSONArray) value, varValue);
				} else if (value instanceof JSONObject) {
					varValue = new ReferenceValue(false, false, var);
					varValue.setStringValue(String.valueOf(value));

					processResponseRecur(false, (JSONObject) value, varValue);
				} else if (value instanceof String) {
					varValue = new StringValue(String.valueOf(value), false, var);
				} else if (value instanceof Integer) {
					varValue = new PrimitiveValue(String.valueOf(value), false, var);
				} else if (value == JSONObject.NULL) {
					varValue = new ReferenceValue(true, false, var);
				}

				if (varValue != null) {
					selectedVariable.updateChild(varValue);
				}
			}
		}
	}

	private static void processResponseRecur(JSONArray jsonArray, VarValue selectedVariable) {
		int index = 0;
		Iterator<Object> iterator = jsonArray.iterator();

		while (iterator.hasNext()) {
			Object value = iterator.next();

			String varName = selectedVariable.getVarName().concat("[" + index + "]");
			String varType = "";

			String headAddress = selectedVariable.getAliasVarID().equals("0") ? selectedVariable.getVarID()
					: selectedVariable.getAliasVarID();
			String varID = Variable.concanateFieldVarID(headAddress, varName);

			Variable var = new FieldVar(false, varName, varType, varType);
			var.setVarID(varID);

			VarValue varValue = null;

			if (value instanceof JSONArray) {
				varValue = new ArrayValue(false, false, var);
				varValue.setStringValue(String.valueOf(value));

				processResponseRecur((JSONArray) value, varValue);
			} else if (value instanceof JSONObject) {
				varValue = new ReferenceValue(false, false, var);
				varValue.setStringValue(String.valueOf(value));

				processResponseRecur(false, (JSONObject) value, varValue);
			} else if (value instanceof String) {
				varType = value.getClass().toString();
				var.setType(varType);

				varValue = new StringValue(String.valueOf(value), false, var);
			} else if (value instanceof Integer) {
				varType = value.getClass().toString();
				var.setType(varType);

				varValue = new PrimitiveValue(String.valueOf(value), false, var);
			} else if (value == null) {
				varValue = new ReferenceValue(true, false, var);
			}

			if (varValue != null) {
				selectedVariable.updateChild(varValue);
			}

			index++;
		}
	}

}