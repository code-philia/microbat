package microbat.tracerecov.autoprompt;

import java.util.HashMap;

public abstract class PromptTemplateFiller {

	public abstract String getDefaultPromptExample();

	public abstract String getPromptQuestion(HashMap<String, String> datapoint);

	public abstract String getPrompt(HashMap<String, String> datapoint, String example);

	public abstract String getDefaultPrompt(HashMap<String, String> datapoint);

	public abstract String getExample(HashMap<String, String> datapoint, String groundTruth);

}
