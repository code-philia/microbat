package microbat.tracerecov.executionsimulator;

/**
 * This class is used to create an {@code ExecutionSimulator} given
 * {@code LLMModel} type.
 */
public class ExecutionSimulatorFactory {

	public static ExecutionSimulator getExecutionSimulator() {
		if (isGPT(SimulatorConstants.modelType)) {
			return new GPTExecutionSimulator();
		} else if (isGemini(SimulatorConstants.modelType)) {
			return new GeminiExecutionSimulator();
		}
		return new GPTExecutionSimulator(); // default model
	}

	private static boolean isGPT(LLMModel modelType) {
		return modelType == LLMModel.GPT3 || modelType == LLMModel.GPT4 || modelType == LLMModel.GPT4O
				|| modelType == LLMModel.GPT4O_MINI;
	}

	private static boolean isGemini(LLMModel modelType) {
		return modelType == LLMModel.GEMINI;
	}
}
