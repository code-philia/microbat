package microbat.tracerecov.executionsimulator;

import microbat.Activator;
import microbat.preference.TraceRecovPreference;

public class SimulatorConstants {
	
	public static LLMModel modelType = LLMModel
			.valueOf(Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.MODEL_TYPE));
	public static String API_KEY = Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.API_KEY);
	
	public static final String GPT3 = "gpt-3.5-turbo";
	public static final String GPT4 = "gpt-4-turbo";
	public static final String GPT4O = "gpt-4o";
	public static final String GPT4O_MINI = "gpt-4o-mini";

	public static final String CLAUDE3 = "claude-3";
	public static final String CLAUDE35 = "claude-3.5";
	public static final String GEMINI = "gemini-1.5-flash";

	public static final String GPT_API_ENDPOINT = "https://api.openai.com/v1/chat/completions";
	public static final String CLAUDE3_API_ENDPOINT = "https://api.claude.ai/v3";
	public static final String CLAUDE35_API_ENDPOINT = "https://api.claude.ai/v3.5";
	public static final String GEMINI_API_ENDPOINT = "https://generativelanguage.googleapis.com/v1/models/";

	public static String getSelectedModel() {
		switch (modelType) {
		case GPT3:
			return SimulatorConstants.GPT3;
		case GPT4:
			return SimulatorConstants.GPT4;
		case GPT4O:
			return SimulatorConstants.GPT4O;
		case GPT4O_MINI:
			return SimulatorConstants.GPT4O_MINI;
		case CLAUDE3:
			return SimulatorConstants.CLAUDE3;
		case CLAUDE35:
			return SimulatorConstants.CLAUDE35;
		case GEMINI:
			return SimulatorConstants.GEMINI;
		default:
			return "";
		}
	}

	/* common parameters */
	public static final double TEMPERATURE = 0;
	public static final int MAX_TOKENS = 4096;
	
	/* GPT parameters */
	public static final double TOP_P = 1;
	public static final double FREQUENCY_PENALTY = 1;
	public static final double PRESENCE_PENALTY = 0;
	
	/* Gemini parameters */
	public static final int GEMINI_TOP_P = 0;
	public static final int GEMINI_TOP_K = 0;
}
