package microbat.tracerecov.executionsimulator;

import java.util.List;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;

/**
 * This class contains constants and methods used in execution simulation.
 * 
 * @author hongshuwang
 */
public class SimulationUtils {

	/* ChatGPT API */
	public static final String API_URL = "https://api.openai.com/v1/chat/completions";
	public static final String GPT3 = "gpt-3.5-turbo";
	public static final String GPT4 = "gpt-4-turbo";
	public static final String GPT4O = "gpt-4o";

	/* Model constants */
	public static final double TEMPERATURE = 0.2;

	/* Request content */
	private static final String REQUEST_BACKGROUND = "When a segment of code is executed, "
			+ "a candidate variable is a variable that might have been read or written.";
	private static final String QUESTION_SUFFIX = "Return the values of the candidate variables "
			+ "***after execution***. Your response should be in this format without explanation: "
			+ "<step number>#<candidate variable>#<value>";

	/* Methods */

	public static String getBackgroundContent() {
		return REQUEST_BACKGROUND;
	}

	public static String getQuestionContent(TraceNode currentStep, VarValue currentVar,
			List<TraceNode> relevantSteps, String aliasID) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(getContentForCurrentStep(currentStep, currentVar));

		stringBuilder.append("In the following steps: \n");
		for (TraceNode step : relevantSteps) {
			VarValue criticalVar = step.getReadVariables()
				.stream()
				.filter(v -> Variable.truncateSimpleID(v.getAliasVarID()).equals(aliasID))
				.findFirst()
				.orElse(null);
			stringBuilder.append(getContentForRelevantStep(step, criticalVar));
		}

		stringBuilder.append(QUESTION_SUFFIX);

		return stringBuilder.toString();
	}

	/**
	 * Given step STEP_NO with signature METHOD_SIGNATURE; "VAR_NAME" has candidate
	 * variables: <C1>;<C2>;...<Cn>;
	 */
	private static StringBuilder getContentForCurrentStep(TraceNode node, VarValue varValue) {
		StringBuilder stringBuilder = new StringBuilder("Given step ");

		stringBuilder.append(node.getOrder());
		stringBuilder.append(" with signature ");
		stringBuilder.append(node.getInvokingMethod().split("%")[0]);
		stringBuilder.append(" \"");
		stringBuilder.append(varValue.getVarName());
		stringBuilder.append("\" has candidate variables: ");
		List<String> candidateVariables = varValue.getCandidateVariables();
		if (candidateVariables != null) {
			for (String v : candidateVariables) {
				stringBuilder.append("<");
				stringBuilder.append(v);
				stringBuilder.append(">;");
			}
		}
		stringBuilder.append("\n");

		return stringBuilder;
	}

	/**
	 * STEP_NO: METHOD_SIGNATURE; the candidate variable has value <VAR_VALUE>
	 * ***before execution***
	 */
	private static StringBuilder getContentForRelevantStep(TraceNode node, VarValue varValue) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(node.getOrder());
		stringBuilder.append(": ");
		stringBuilder.append(node.getInvokingMethod().split("%")[0]);
		stringBuilder.append(" the candidate variable has value \"");
		stringBuilder.append(varValue.getStringValue());
		stringBuilder.append("\" ***before execution***");
		stringBuilder.append("\n");

		return stringBuilder;
	}

}
