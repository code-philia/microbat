package microbat.tracerecov.executionsimulator;

import microbat.Activator;
import microbat.preference.TraceRecovPreference;

public class SimulatorConstants {

	/* ChatGPT API */
	public static final String API_URL = "https://api.openai.com/v1/chat/completions";
	public static final String GPT3 = "gpt-3.5-turbo";
	public static final String GPT4 = "gpt-4-turbo";
	public static final String GPT4O = "gpt-4o";
	
	/* API Key */
	public static String API_KEY = Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.API_KEY);

	/* Model constants */
	public static final double TEMPERATURE = 0;
	public static final int MAX_TOKENS = 256;
	public static final double TOP_P = 1;
	public static final double FREQUENCY_PENALTY = 1;
	public static final double PRESENCE_PENALTY = 0;

}
