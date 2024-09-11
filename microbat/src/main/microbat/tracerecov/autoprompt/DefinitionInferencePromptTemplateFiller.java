package microbat.tracerecov.autoprompt;

import java.util.HashMap;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;

public class DefinitionInferencePromptTemplateFiller extends PromptTemplateFiller {

	// TODO: add default example
	private static String definitionInferencePromptExample = "";

	@Override
	public String getDefaultPromptExample() {
		return definitionInferencePromptExample;
	}

	// TODO: update later (not used for now)
	@Override
	public String getPromptQuestion(HashMap<String, String> datapoint) {
		return null;
	}

	// TODO: update later (not used for now)
	@Override
	public String getPrompt(HashMap<String, String> datapoint, String example) {
		return null;
	}

	// TODO: update later (not used for now)
	@Override
	public String getDefaultPrompt(HashMap<String, String> datapoint) {
		return null;
	}

	@Override
	public String getExample(HashMap<String, String> datapoint, String groundTruth) {
		/* datapoint features */
		String targetField = datapoint.get(DatasetReader.TARGET_FIELD);
		String rootVarName = datapoint.get(DatasetReader.VAR_NAME);
		String classStructure = datapoint.get(DatasetReader.TARGET_VAR);
		String sourceCode = datapoint.get(DatasetReader.SOURCE_CODE);
		String invokedMethods = datapoint.get(DatasetReader.INVOKED_METHODS).strip().replace("\\n", "\n")
				.replace("\\r", "\r").replace("***", "\n");
		String varsInStep = datapoint.get(DatasetReader.VARS_IN_STEP);

		StringBuilder stringBuilder = new StringBuilder("\n\n<Example>\r\n");

		stringBuilder.append("Given the code as:\n```" + sourceCode + "```");

		if (!invokedMethods.isEmpty()) {
			stringBuilder.append("\n\nGiven the source code of function calls in the code:\n" + invokedMethods);
		}

		stringBuilder.append("\nVariables involved:" + varsInStep);

		stringBuilder.append("\n\nwe know that later `" + rootVarName + "` has the following structure and value:\n");
		stringBuilder.append(classStructure);
		stringBuilder.append("\nBut we don't know which step during the execution modified the value.\n");

		stringBuilder.append("`" + rootVarName + "` has a field called `" + targetField + "`");

		stringBuilder.append("\n\nIn this example, the result is: " + groundTruth + "\n");
		stringBuilder.append("In the actual question, you need to analyse and get an answer, which might be T or F.\n");

		return stringBuilder.toString();
	}

	/*
	 * Adjustment Prompt TODO: update later (not used for now)
	 */

	@Override
	public String getAdjustmentPrompt(HashMap<String, String> datapoint, String example) {
		return null;
	}

	@Override
	public String getDefaultAdjustmentPrompt(HashMap<String, String> datapoint) {
		return null;
	}

	@Override
	public String getAdjustmentPromptWithLoss(String example, HashMap<String, String> datapoint, String output,
			String textualLoss) {
		return null;
	}

}
