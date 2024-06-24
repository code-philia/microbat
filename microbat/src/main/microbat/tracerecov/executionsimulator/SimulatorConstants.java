package microbat.tracerecov.executionsimulator;

import microbat.Activator;
import microbat.preference.TraceRecovPreference;

public class SimulatorConstants {

	/* ChatGPT API */
	public static final String API_URL = "https://api.openai.com/v1/chat/completions";
	public static String API_KEY = Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.API_KEY);

	/* Model */
	public static LLMModel modelType = LLMModel
			.valueOf(Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.MODEL_TYPE));
	public static final String GPT3 = "gpt-3.5-turbo";
	public static final String GPT4 = "gpt-4-turbo";
	public static final String GPT4O = "gpt-4o";

	public static String getSelectedModel() {
		if (modelType == LLMModel.GPT3) {
			return SimulatorConstants.GPT3;
		} else if (modelType == LLMModel.GPT4) {
			return SimulatorConstants.GPT4;
		} else if (modelType == LLMModel.GPT4O) {
			return SimulatorConstants.GPT4O;
		}

		return "";
	}

	/* Model parameters */
	public static final double TEMPERATURE = 0;
	public static final int MAX_TOKENS = 256;
	public static final double TOP_P = 1;
	public static final double FREQUENCY_PENALTY = 1;
	public static final double PRESENCE_PENALTY = 0;

}
