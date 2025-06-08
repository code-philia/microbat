package microbat.tracerecov.executionsimulator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;
import microbat.model.value.ArrayValue;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.StringValue;
import microbat.model.value.VarValue;
import microbat.model.variable.FieldVar;
import microbat.model.variable.Variable;
import microbat.tracerecov.TraceRecovUtils;

@Slf4j
public class DataStructureAbstractionUtils {
	private static final GptTaskInfo taskInfo;
	private static final String promptUser;
	private static final String promptUserFeedback;
	private static final String promptExampleMap;
	private static final String promptExampleTemplate;

	static {
		taskInfo = GptTaskInfo.DATA_STRUCTURE_ABSTRACTION;
		promptUser = taskInfo.loadPromptUser();
		promptUserFeedback = taskInfo.loadPromptUserwithfeedback();
		promptExampleMap = taskInfo.loadPrompt("example_map");
		promptExampleTemplate = taskInfo.loadPrompt("example_template");
	}

	public static String generatePrompt(VarValue exampleVar, VarValue var, String errorMessage, String previousResponse) {
		String example;
		if (exampleVar == null) {
			example = promptExampleMap;
		} else {
			example = generateExample(exampleVar);
		}

		String typeName = processVarType(var);
		String toStringValue = var.getStringValue();
		String concreteValueJson = var.toJSON().toString();
		if (errorMessage == null || previousResponse == null) {	
			Map<String, String> valuesMap = Map.of(
					"typeName", typeName,
					"toStringValue", toStringValue,
					"concreteValueJson", concreteValueJson,
					"example", example);
			return GptTaskInfo.formatPromptString(promptUser, valuesMap);
		} else {
			String jsonString;
			try {
				jsonString = GptTaskInfo.findPatternIn("json", previousResponse);
			}
			catch (Exception e) {
				jsonString = "No JSON found in previous response. Possibly malformed response.";
			}
			Map<String, String> valuesMap = Map.of(
					"typeName", typeName,
					"toStringValue", toStringValue,
					"concreteValueJson", concreteValueJson,
					"example", example,
					"previousErrorMessage", errorMessage,
					"previousOutputJson", jsonString);
			return GptTaskInfo.formatPromptString(promptUserFeedback, valuesMap);
		}

		// return GptTaskInfo.formatPromptString(promptUser, valuesMap);
	}

	private static String generateExample(VarValue var) {
		String typeName = processVarType(var);
		String toStringValue = var.getStringValue();
		String concreteValueJson = var.getFullExpandedValue();
		String abstractValueJson = var.getAbstractedValue();

		Map<String, String> valuesMap = Map.of(
				"typeName", typeName,
				"toStringValue", toStringValue,
				"concreteValueJson", concreteValueJson,
				"abstractValueJson", abstractValueJson);
		return GptTaskInfo.formatPromptString(promptExampleTemplate, valuesMap);
	}

	private static String processVarType(VarValue var) {
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
		return variableType;
	}

	/**
	 * Recursively parse JSON into the input VarValue.
	 */
	public static void processResponse(VarValue selectedVariable, String response) {
		String jsonValue = GptTaskInfo.findPatternIn("json", response);
		JSONObject variable = new JSONObject(jsonValue);

		selectedVariable.setAbstractedValue(jsonValue);
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
			// String varID = Variable.concanateFieldVarID(headAddress, varName);

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
				// varType = value.getClass().toString();
				var.setType("String");

				varValue = new StringValue(String.valueOf(value), false, var);
			} else if (value instanceof Integer) {
				// varType = value.getClass().toString();
				var.setType("int");

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
