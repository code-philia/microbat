package microbat.tracerecov.executionsimulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringSubstitutor;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import microbat.Activator;
import microbat.model.trace.TraceNode;
import microbat.model.value.ArrayValue;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.StringValue;
import microbat.model.value.VarValue;
import microbat.model.variable.FieldVar;
import microbat.model.variable.Variable;
import microbat.preference.RecovSlicingPreference;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.VarExpansionExampleSearcher;
import microbat.tracerecov.autoprompt.VarExpansionPromptTemplateFiller;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;
import microbat.tracerecov.autoprompt.incontextlearning.FailToExtractMethodException;
import microbat.tracerecov.varskeleton.VariableSkeleton;
import sav.common.core.Pair;

@Slf4j
public class VariableExpansionUtils {
	private static final Gson gson;

	private static final GptTaskInfo taskInfo;
	private static final String promptUser;
	private static final JsonElement defaultExampleJson;
	private static final VariableExpansionExample defaultExample;

	static {
		gson = new GsonBuilder().setPrettyPrinting().create();

		taskInfo = GptTaskInfo.VARIABLE_EXPANSION;
		promptUser = taskInfo.loadPromptUser();
		defaultExampleJson = taskInfo.loadJson("default_example");
		defaultExample = new VariableExpansionExample(defaultExampleJson);
	}

	@Getter
	@AllArgsConstructor
	@ToString
	public static class VariableExpansionExample {
		private final String value;
		private final String type;
		private final String structures;
		private final String expanded;

		public VariableExpansionExample(JsonElement example) {
			try {
				JsonObject jsonObject = example.getAsJsonObject();

				JsonArray structures = jsonObject.get("structures").getAsJsonArray();
				StringBuilder sb = new StringBuilder();
				for (JsonElement s : structures) {
					sb.append("- `");
					sb.append(s.getAsString());
					sb.append("`\n");
				}
				String structuresStr = sb.toString();
				String type = jsonObject.get("type").getAsString();
				String value = jsonObject.get("value").getAsString();
				String expanded = gson.toJson(jsonObject.get("expanded"));

				this.value = value;
				this.type = type;
				this.structures = structuresStr;
				this.expanded = expanded;

			} catch (Exception e) {
				log.error("Failed to load example from JSON: {}", example, e);
				RuntimeException ex = new RuntimeException("Failed to load Example", e);
				throw ex;
			}
		}
	}

	private static HashMap<String, String> getDatapointFromStep(VarValue varValue, VariableSkeleton varSkeleton,
			TraceNode step) {
		HashMap<String, String> datapoint = new HashMap<>();

		Object[] methodSourceCodeAndLine;
		String methodSourceCode = "";
		int lineNoInMethod = -1;
		try {
			methodSourceCodeAndLine = getMethodSourceCode(step);
			methodSourceCode = (String) methodSourceCodeAndLine[0];
			lineNoInMethod = (int) methodSourceCodeAndLine[1];
		} catch (FailToExtractMethodException e) {
			e.printStackTrace();
		}

		List<String> importStatements = getImportStatements(step);
		int lineNo = lineNoInMethod;
		// if (importStatements.size() != 0) {
		// lineNo = lineNoInMethod + importStatements.size() + 1;
		// }

		StringBuilder imports = new StringBuilder();
		importStatements.stream().forEach(i -> imports.append(i + "\n"));

		datapoint.put(DatasetReader.VAR_NAME, varValue.getVarName());
		datapoint.put(DatasetReader.VAR_TYPE, varValue.getType());
		datapoint.put(DatasetReader.VAR_VALUE, TraceRecovUtils.processInputStringForLLM(varValue.getStringValue()));
		datapoint.put(DatasetReader.CLASS_STRUCTURE, varSkeleton.toString());
		datapoint.put(DatasetReader.LINE_SOURCE_CODE, getLineSourceCode(step));
		datapoint.put(DatasetReader.METHOD_SOURCE_CODE, methodSourceCode);
		datapoint.put(DatasetReader.IMPORTS, imports.toString());
		datapoint.put(DatasetReader.LINE_NO, String.valueOf(lineNo));
		datapoint.put(DatasetReader.GROUND_TRUTH, ""); // not available yet

		return datapoint;
	}

	public static VariableExpansionExample formatGivenExample(VarValue exampleVar, VariableSkeleton varSkeleton,
			TraceNode step) {
		if (isEnabledInContextLearning()) {
			HashMap<String, String> datapoint = getDatapointFromStep(exampleVar, varSkeleton, step);
			String expanded = exampleVar.isExpansionAbstracted() ? exampleVar.getFullExpandedValue()
					: exampleVar.toJSON().toString();
			return new VarExpansionPromptTemplateFiller().getExampleStructured(datapoint, expanded);
		} else {
			return defaultExample;
		}
	}

	public static VariableExpansionExample getExample(VarValue varValue, VariableSkeleton varSkeleton,
			TraceNode step) {
		if (isEnabledInContextLearning()) {
			HashMap<String, String> datapoint = getDatapointFromStep(varValue, varSkeleton, step);

			VarExpansionExampleSearcher exampleSearcher = new VarExpansionExampleSearcher(true);
			VariableExpansionExample closestExample = exampleSearcher.searchForExampleStructured(datapoint,
					step.getTrace().getAppJavaClassPath());

			if (closestExample == null) {
				return defaultExample;
			}
			return closestExample;
		} else {
			return defaultExample;
		}
	}

	private static boolean isEnabledInContextLearning() {
		String isEnabledInContextLearningStr = Activator.getDefault().getPreferenceStore()
				.getString(RecovSlicingPreference.ENABLE_IN_CONTEXT_LEARNING);
		return isEnabledInContextLearningStr != null && isEnabledInContextLearningStr.equals("true");
	}

	private static String getLineSourceCode(TraceNode step) {
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils
				.processInputStringForLLM(TraceRecovUtils.getSourceCodeOfALine(location, lineNo).trim());
		return sourceCode;
	}

	/**
	 * Get method code and the relative line number of the given line within the
	 * method.
	 * 
	 * @param filePath
	 * @param lineNumber
	 * @return Object[] {String MethodSourceCode, Integer RelativeLineNumber}
	 * @throws FailToExtractMethodException
	 */
	private static Object[] getMethodSourceCode(TraceNode step) throws FailToExtractMethodException {
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		return TraceRecovUtils.getSourceCodeOfMethodContainingLine(location, lineNo);
	}

	private static List<String> getImportStatements(TraceNode step) {
		String location = step.getBreakPoint().getFullJavaFilePath();
		return TraceRecovUtils.getImportStatements(location);
	}

	public static final Pattern ALL_PATTERNS = Pattern
			.compile("([a-zA-Z_][a-zA-Z0-9_]*)|(\\.[a-zA-Z_][a-zA-Z0-9_]*)|(\\[\\d+\\])");

	public static String pruneOtherFields(String example, String fieldName) {
		List<String> components = new ArrayList<>();
		Matcher matcher = ALL_PATTERNS.matcher(example);
		while (matcher.find()) {
			String component = matcher.group();
			components.add(component);
		}

		JsonElement jsonElement = gson.fromJson(example, JsonElement.class);
		JsonElement prunedElement = pruneFieldsRecur(jsonElement, components, 0);
		return gson.toJson(prunedElement);
	}

	private static JsonElement pruneFieldsRecur(JsonElement jsonElement, List<String> components, int beginIndex) {
		if (beginIndex >= components.size()) {
			return jsonElement;
		}
		String component = components.get(beginIndex);
		if (component.charAt(0) == '[') {
			if (!jsonElement.isJsonArray()) {
				return jsonElement;
			}
			JsonArray jsonArray = jsonElement.getAsJsonArray();
			JsonArray mapped = new JsonArray();
			for (JsonElement element : jsonArray) {
				JsonElement pruned = pruneFieldsRecur(element, components, beginIndex + 1);
				mapped.add(pruned);
			}
			return mapped;
		} else {
			if (!jsonElement.isJsonObject()) {
				return jsonElement;
			}

			String name;
			if (component.charAt(0) == '.') {
				name = component.substring(1);
			} else {
				name = component;
			}

			JsonObject jsonObject = jsonElement.getAsJsonObject();
			for (String key : jsonObject.keySet()) {
				if (!key.contains("|")) {
					continue;
				}
				String keyName = key.split("\\|")[0];
				if (keyName.equals(name)) {
					JsonElement value = jsonObject.get(key);
					JsonElement pruned = pruneFieldsRecur(value, components, beginIndex + 1);
					JsonObject mapped = new JsonObject();
					mapped.add(name, pruned);
					// mapped.addProperty("...", "...");
					return mapped;
				}
			}
			return jsonElement;
		}
	}

	public static String getQuestionContent(
			VariableExpansionExample example,
			VarValue selectedVariable,
			List<VariableSkeleton> variableSkeletons,
			TraceNode step,
			Pair<String, String> preValueResponse,
			String focalPath) {

		String exampleGroundTruth = example.getExpanded();
		if (focalPath == null || focalPath.isEmpty()) {
			focalPath = "#all_fields#";
		} else {
			exampleGroundTruth = pruneOtherFields(exampleGroundTruth, focalPath);
		}
		log.error("focalPath: {}", focalPath);

		String code = getLineSourceCode(step);
		String name = selectedVariable.getVarName();
		String value = TraceRecovUtils.processInputStringForLLM(selectedVariable.getStringValue());

		String type = selectedVariable.getType();
		if (!selectedVariable.getChildren().isEmpty()) {
			VarValue child = selectedVariable.getChildren().get(0);
			String childType = child.getType();
			if (childType.contains("[]")) {
				childType = childType.substring(0, childType.length() - 2);
			}
			type = type.concat("\\<" + childType + "\\>");
		}

		StringBuilder classStructures = new StringBuilder();
		for (VariableSkeleton v : variableSkeletons) {
			if (v != null) {
				classStructures.append("- `").append(v.toString()).append("`\n");
			}
		}

		Map<String, String> values = new HashMap<>();
		values.put("exampleValue", example.getValue());
		values.put("exampleType", example.getType());
		values.put("exampleClassStructures", example.getStructures());
		values.put("exampleExpanded", exampleGroundTruth);
		values.put("name", name);
		values.put("type", type);
		values.put("value", value);
		values.put("classStructures", classStructures.toString());
		values.put("code", code);
		values.put("exampleFocalPath", focalPath);
		values.put("focalPath", focalPath);
		StringSubstitutor sub = new StringSubstitutor(values);
		return sub.replace(promptUser);
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
			if (key.equals("...")) {
				continue;
			}

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
