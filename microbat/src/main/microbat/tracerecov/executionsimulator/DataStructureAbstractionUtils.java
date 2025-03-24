package microbat.tracerecov.executionsimulator;

import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;

import microbat.model.value.ArrayValue;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.StringValue;
import microbat.model.value.VarValue;
import microbat.model.variable.FieldVar;
import microbat.model.variable.Variable;
import microbat.tracerecov.TraceRecovUtils;
import sav.common.core.Pair;

public class DataStructureAbstractionUtils {

	/* Request content */

	private static final String VAR_EXPAND_BACKGROUND = "<Background>\r\n"
			+ "You are an expert in java who wants to extract out the semantic meanings of a given data structure."
			+ "\r\n";

	/* Methods */

	public static String getBackgroundContent(VarValue exampleVar) {
		if (exampleVar == null) {
			return VAR_EXPAND_BACKGROUND;
		}
		return VAR_EXPAND_BACKGROUND + getContent(exampleVar, true);
	}

	private static String getContent(VarValue var, boolean isExample) {
		/* type of selected variable */
		String variableType = var.getType();
		// assume var layer == 1, then only elementArray will be recorded in ArrayList
		if (!var.getChildren().isEmpty()) {
			VarValue child = var.getChildren().get(0);
			String childType = child.getType();
			if (childType.contains("[]")) {
				childType = childType.substring(0, childType.length() - 2); // remove [] at the end
			}
			variableType = variableType.concat("\\<" + childType + "\\>");
		}

		StringBuilder content = new StringBuilder();

		content.append(isExample ? "\n\n<Example>\n" : "\n\n<Question>\n");

		content.append("Given `");
		content.append(variableType);
		content.append("` with structure:\n");
		String originalStructure = isExample ? var.getFullExpandedValue()
				: TraceRecovUtils.processInputStringForLLM(var.toJSON().toString());
		content.append(originalStructure);

		content.append("\n\nReturn a simplified abstracted version in JSON.");

		if (isExample) {
			content.append("\n\nYour response should be:\n");
			content.append(var.getAbstractedValue());
		}

		return content.toString();

	}

	public static String getQuestionContent(VarValue selectedVariable) {
		return getContent(selectedVariable, false);
	}

	/**
	 * Recursively parse JSON into the input VarValue.
	 */
	public static void processResponse(VarValue selectedVariable, String response) {
		int begin = response.indexOf("{");
		int end = response.lastIndexOf("}");
		response = response.substring(begin, end + 1);

		JSONObject variable = new JSONObject(response);

		selectedVariable.setAbstractedValue(response);
		selectedVariable.setChildren(new ArrayList<>());

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
				String varName = key;

				Variable var = new FieldVar(false, varName, "", "");

				String headAddress = selectedVariable.getAliasVarID().equals("0") ? selectedVariable.getVarID()
						: selectedVariable.getAliasVarID();
				var.setVarID(Variable.concanateFieldVarID(headAddress, varName));

				Object value = jsonObject.get(key);
				VarValue varValue = null;

				if (value instanceof JSONArray || value instanceof String) {
					if (value instanceof String) {
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
					var.setType("String");
					varValue = new StringValue(String.valueOf(value), false, var);
				} else if (value instanceof Integer) {
					var.setType("int");
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
			String varID = headAddress + "[" + index + "]";
//			String varID = Variable.concanateFieldVarID(headAddress, varName);

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
