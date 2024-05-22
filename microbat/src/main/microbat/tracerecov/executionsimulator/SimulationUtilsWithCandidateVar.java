package microbat.tracerecov.executionsimulator;

import java.util.List;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;
import microbat.tracerecov.VariableGraph;

/**
 * This class contains constants and methods used in execution simulation (with
 * candidate variables).
 * 
 * @author hongshuwang
 */
public class SimulationUtilsWithCandidateVar {

	/* Request content */
	private static final String REQUEST_BACKGROUND_WITH_CANDIDATE_VAR = "When a segment of code is executed, a "
			+ "candidate variable is a variable that might have been read or written. The variable of interest "
			+ "(VOI) is identified in each step, when we talk about candidate variables, we refer to the VOI.\n"
			+ "In each step, the variable values ***before*** execution are given, you need to provide the value "
			+ "of the VOI ***before*** and ****after*** the execution of each step respectively. You should include "
			+ "all changed candidate variables in your response. Don't return the value of variables other than the "
			+ "candidate variables of the VOI.\n";

	private static final String QUESTION_SUFFIX_WITH_CANDIDATE_VAR = "Return the values of the candidate variables "
			+ "***before and after*** the execution. Your response should be in this format without explanation: "
			+ "<step_No>#<candidate_variable_name>#<candidate_variable_value_BEFORE>#<candidate_variable_value_AFTER>";

	/* Methods */

	public static String getBackgroundContent() {
		return REQUEST_BACKGROUND_WITH_CANDIDATE_VAR;
	}

	public static String getQuestionContent(String variableID, List<TraceNode> relevantSteps) {
		StringBuilder stringBuilder = new StringBuilder();

		VarValue criticalVar = relevantSteps.get(0).getReadVariables().stream()
				.filter(v -> Variable.truncateSimpleID(v.getAliasVarID()).equals(variableID)).findFirst().orElse(null);
		String type = criticalVar.getType();
		List<String> candidateVariables = VariableGraph.getCandidateVariables(variableID);
		stringBuilder.append(getContentForVariableOfInterest(type, candidateVariables));

		stringBuilder.append("In the following steps: \n");
		for (TraceNode step : relevantSteps) {
			criticalVar = step.getReadVariables().stream()
					.filter(v -> Variable.truncateSimpleID(v.getAliasVarID()).equals(variableID)).findFirst()
					.orElse(null);
			stringBuilder.append(SimulationUtils.getContentForRelevantStep(step, criticalVar));
		}

		stringBuilder.append(QUESTION_SUFFIX_WITH_CANDIDATE_VAR);

		return stringBuilder.toString();
	}

	/**
	 * The VOI in each of the following steps has type "TYPE", and it has candidate
	 * variables with the following names: "C1", "C2", ..., "Cn"
	 */
	private static StringBuilder getContentForVariableOfInterest(String type, List<String> candidateVariables) {
		StringBuilder stringBuilder = new StringBuilder("The VOI in each of the following steps has type \"");
		stringBuilder.append(type);
		stringBuilder.append("\", and it has candidate variables with the following names:");

		if (candidateVariables != null) {
			for (String v : candidateVariables) {
				stringBuilder.append("\"");
				stringBuilder.append(v);
				stringBuilder.append("\",");
			}
		}

		stringBuilder.append("The <candidate_variable_name> in your response can only take from the above names.\n");

		return stringBuilder;
	}

	public static void processResponse(String response, String variableID, List<TraceNode> relevantSteps) {
		String[] lines = response.split("\n");
		for (String line : lines) {
			String[] entries = line.split("#");
			if (entries.length != 4) {
				continue;
			}
			int stepNo = Integer.valueOf(entries[0].trim());
			String varName = entries[1].trim();
			String valBefore = entries[2].trim();
			String valAfter = entries[3].trim();

			TraceNode step = relevantSteps.stream().filter(s -> {
				return s.getOrder() == stepNo;
			}).findFirst().orElse(null);
			if (step == null) {
				continue;
			}

			VarValue readVar = step.getReadVariables().stream().filter(v -> v.getAliasVarID().equals(variableID))
					.findFirst().orElse(null);
			if (readVar == null) {
				continue;
			}

			VarValue criticalReadVar = readVar.getAllDescedentChildren().stream()
					.filter(v -> v.getVarName().equals(varName)).findFirst().orElse(null);
			if (criticalReadVar == null) {
				continue;
			}
			// set value before
			criticalReadVar.setStringValue(valBefore);

			if (valBefore == valAfter || valBefore.equals(valAfter)) {
				continue;
			}

			VarValue writtenVar = step.getWrittenVariables().stream().filter(v -> v.getAliasVarID().equals(variableID))
					.findFirst().orElse(null);

			// set value after
			if (writtenVar == null) {
				VarValue criticalWrittenVar = criticalReadVar.clone();
				criticalWrittenVar.setStringValue(valAfter);
				step.addWrittenVariable(criticalWrittenVar);
			} else {
				VarValue criticalWrittenVar = writtenVar.getAllDescedentChildren().stream()
						.filter(v -> v.getVarName().equals(varName)).findFirst().orElse(null);
				if (criticalWrittenVar == null) {
					criticalWrittenVar = criticalReadVar.clone();
					criticalWrittenVar.setStringValue(valAfter);
					writtenVar.addChild(criticalWrittenVar);
				} else {
					criticalWrittenVar.setStringValue(valAfter);
				}
			}

		}
	}
}
