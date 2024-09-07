package microbat.tracerecov.autoprompt;

import java.util.HashMap;

public abstract class PromptTemplateFiller {

	/* Prompt to be adjusted */

	public abstract String getDefaultPromptExample();

	public abstract String getPromptQuestion(HashMap<String, String> datapoint);

	public abstract String getPrompt(HashMap<String, String> datapoint, String example);

	public abstract String getDefaultPrompt(HashMap<String, String> datapoint);

	public abstract String getExample(HashMap<String, String> datapoint, String structure);

	/* Adjustment Prompt */

	public abstract String getAdjustmentPrompt(HashMap<String, String> datapoint, String example);

	public abstract String getDefaultAdjustmentPrompt(HashMap<String, String> datapoint);

	/* Adjustment Prompt Incorporating Textual Loss */

	public abstract String getAdjustmentPromptWithLoss(String example, HashMap<String, String> datapoint, String output,
			String textualLoss);
}
