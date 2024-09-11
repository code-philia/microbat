package microbat.tracerecov.autoprompt;

import java.util.HashMap;

import org.json.JSONObject;

import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;

// TODO: update later (not used for now)
public class AliasInferencePromptTemplateFiller extends PromptTemplateFiller {

	private static String aliasInferencePromptBackground = "<Background>\n"
			+ "You are a Java expert, you need to analyze the alias relationships through static analysis. Given a variable and a method call, your task is to identify any alias relationship between (*Set 1*) the listed fields of the given variable and (*Set 2*) the variables involved in the method call and the return value of the method call.";

	private static String aliasInferencePromptExample = "\n\n<Example>\n"
			+ "Given code:\n"
			+ "```list.add(item);```\n"
			+ "\n"
			+ "Given the source code of function calls in the code:\n"
			+ "public boolean add(E e) {\n"
			+ "modCount++;\n"
			+ "add(e, elementData, size);\n"
			+ "return true;\n"
			+ "}\n"
			+ "\n"
			+ "Variables involved:\n"
			+ "`list` is of type `java.util.ArrayList`,\n"
			+ "`item` is of type `Integer`,\n"
			+ "\n"
			+ "We know that another variable not in the code, `list`, with the following structure:\n"
			+ "{\"list:java.util.ArrayList\":{\"elementData:java.lang.Object[]\":\"[]\",\"size:int\":\"0\"}}\n"
			+ "\n"
			+ "We are interested in the fields `list.elementData.elementData[0]`\n"
			+ "\n"
			+ "Your response should be:\n"
			+ "{\n"
			+ "\"list.elementData.elementData[0]\":\"item\"\n"
			+ "}\n\n";
	
	// TODO: prompt engineering
	private static String aliasInferenceAdjustmentPromptPrefix = "";

	/* Prompt to be adjusted */

	@Override
	public String getDefaultPromptExample() {
		return aliasInferencePromptExample;
	}

	@Override
	public String getPromptQuestion(HashMap<String, String> datapoint) {
		/* datapoint features */
		String sourceCode = datapoint.get(DatasetReader.SOURCE_CODE);
		JSONObject variablesInStep = new JSONObject(datapoint.get(DatasetReader.VARS_IN_STEP));

		JSONObject fieldsOfVariablesInStep = new JSONObject(datapoint.get(DatasetReader.FIELDS_OF_VARS_IN_STEP));

		String targetVar = datapoint.get(DatasetReader.TARGET_VAR);
		String[] targetVarNameAndValue = targetVar.split(":", 2);
		String rootVarName = targetVarNameAndValue[0];
		String jsonString = targetVarNameAndValue[1];

		String[] criticalVariables = TraceRecovUtils.parseArrayFromString(datapoint.get(DatasetReader.CRITICAL_VARS));

		JSONObject currentAliases = new JSONObject(datapoint.get(DatasetReader.CURRENT_ALIASES));
		String[] invokedMethods = TraceRecovUtils.parseArrayFromString(datapoint.get(DatasetReader.INVOKED_METHODS));

		// source code
		StringBuilder question = new StringBuilder("<Question>\n" + "Given the code as:\n```");
		question.append(sourceCode);
		question.append("```");

		// variables information (name, type, value)
		for (String key : variablesInStep.keySet()) {
			String[] nameAndType = key.split(":");
			String value = variablesInStep.getString(key);

			question.append("\n`");
			question.append(nameAndType[0]);
			question.append("` is of type `");
			question.append(nameAndType[1]);
			question.append("`, of runtime value \"");
			question.append(value);
			question.append("\",");
		}

		// target variable structure
		question.append("\nWe know that `");
		question.append(rootVarName);
		question.append("` has the following structure:\n");
		question.append(jsonString);
		question.append("\n");

		// existing alias relations
		boolean isFirstVar = true;
		for (String key : currentAliases.keySet()) {
			question.append(isFirstVar ? "where\n`" : "`");
			question.append(key);
			question.append("` has the same memory address as `");
			question.append(currentAliases.get(key));
			question.append("`,\n");
			isFirstVar = false;
		}

		// fields in other variables
		for (String key : fieldsOfVariablesInStep.keySet()) {
			question.append("\n`");
			question.append(key);
			question.append("` has the following fields:\n");
			question.append(fieldsOfVariablesInStep.get(key));
			question.append("\n");
		}

		question.append("\nIdentify all the variable pairs such that each pair has the same memory address. "
				+ "The variable names must be chosen from the above names, not values.\n");

		// invoked methods
		if (invokedMethods.length != 0) {
			question.append("\nOnly analyse variables or fields involved in the following functions:\n");
			for (String methodSig : invokedMethods) {
				question.append(methodSig);
				question.append("\n");
			}
			question.append("Do not analyse other functions.\n");
		}

		// keys (critical variables)
		question.append("Your response should be in JSON format, where keys must be chosen from:");
		for (String criticalVar : criticalVariables) {
			question.append("`" + criticalVar + "`,");
		}

		// description
		question.append(
				" do not include other keys. Values in JSON are the names of other variables listed above, not variable values.\n\n"
						+ "If a field is an element in an array, use `array_name[element_index]` as its name.\n"
						+ "If a variable has name of format `<TYPE>_instance`, it refers to the instance created by calling the constructor of `<TYPE>`.\n\n"
						+ "In your response, strictly follow this format. Do not include explanation. Each key must be included exactly once.");

		return question.toString();
	}

	@Override
	public String getPrompt(HashMap<String, String> datapoint, String example) {
		return aliasInferencePromptBackground + example + getPromptQuestion(datapoint);
	}

	@Override
	public String getDefaultPrompt(HashMap<String, String> datapoint) {
		return aliasInferencePromptBackground + aliasInferencePromptExample + getPromptQuestion(datapoint);
	}

	@Override
	public String getExample(HashMap<String, String> datapoint, String groundTruth) {
		/* datapoint features */
		String sourceCode = datapoint.get(DatasetReader.SOURCE_CODE);
		JSONObject variablesInStep = new JSONObject(datapoint.get(DatasetReader.VARS_IN_STEP));

		String targetVar = datapoint.get(DatasetReader.TARGET_VAR);
		String[] targetVarNameAndValue = targetVar.split(":", 2);
		String rootVarName = targetVarNameAndValue[0];
		String jsonString = targetVarNameAndValue[1];

		// source code
		StringBuilder stringBuilder = new StringBuilder("<Example>\n" + "Given the code as:\n```");
		stringBuilder.append(sourceCode);
		stringBuilder.append("```");

		// variables information (name, type, value)
		for (String key : variablesInStep.keySet()) {
			String[] nameAndType = key.split(":");
			String value = variablesInStep.getString(key);

			stringBuilder.append("\n`");
			stringBuilder.append(nameAndType[0]);
			stringBuilder.append("` is of type `");
			stringBuilder.append(nameAndType[1]);
			stringBuilder.append("`, of runtime value \"");
			stringBuilder.append(value);
			stringBuilder.append("\",");
		}

		// target variable structure
		stringBuilder.append("\nWe know that `");
		stringBuilder.append(rootVarName);
		stringBuilder.append("` has the following structure:\n");
		stringBuilder.append(jsonString);
		stringBuilder.append("\n");

		stringBuilder.append("Your response should be:\n```json" + groundTruth + "\n```");

		return stringBuilder.toString();
	}

	/* Adjustment Prompt */

	@Override
	public String getAdjustmentPrompt(HashMap<String, String> datapoint, String example) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDefaultAdjustmentPrompt(HashMap<String, String> datapoint) {
		// TODO Auto-generated method stub
		return null;
	}

	/* Adjustment Prompt Incorporating Textual Loss */

	@Override
	public String getAdjustmentPromptWithLoss(String example, HashMap<String, String> datapoint, String output,
			String textualLoss) {
		// TODO Auto-generated method stub
		return null;
	}
}
