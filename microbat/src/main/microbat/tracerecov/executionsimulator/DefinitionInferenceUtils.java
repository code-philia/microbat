package microbat.tracerecov.executionsimulator;

import java.util.List;
import java.util.Set;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;

public class DefinitionInferenceUtils {

	/* Request content */

	private static final String DEFINITION_INFERENCE_BACKGROUND = "<Background>\n"
			+ "You are a Java expert, you need to analyze the runtime execution of a Java program.";

	/* Methods */

	public static String getBackgroundContent() {
		return DEFINITION_INFERENCE_BACKGROUND;
	}

	public static String getQuestionContent(TraceNode step, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables) {
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils
				.processInputStringForLLM(TraceRecovUtils.getSourceCodeOfALine(location, lineNo).trim());

		/* variable properties */
		String rootVarName = rootVar.getVarName();
		String targetVarName = targetVar.getVarName();

		/* type structure */
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		/* all variables */
		Set<VarValue> variablesInStep = step.getAllVariables();

		StringBuilder question = new StringBuilder("<Question>\n" + "Given the code as:\n```");
		question.append(sourceCode);
		question.append("```\n");

		for (VarValue var : step.getReadVariables()) {
			question.append("`");
			question.append(var.getVarName());
			question.append("` is of type `");
			question.append(var.getType());
			question.append("`, of runtime value \"");
			question.append(var.getStringValue());
			question.append("\",");
		}

		question.append("we know that `" + rootVarName + "` has the following structure:\n");
		question.append(jsonString);
		question.append("\n");

		boolean isFirstVar = true;
		for (VarValue var : variablesInStep) {
			VarValue criticalVariable = null;
			if (var.getAliasVarID() != null) {
				criticalVariable = criticalVariables.stream().filter(v -> var.getAliasVarID().equals(v.getAliasVarID()))
						.findFirst().orElse(null);
			}
			if (criticalVariable == null) {
				continue;
			}

			String cascadeFieldName = "";
			int splitIndex = criticalVariable.getVarID().indexOf(".");
			if (splitIndex >= 0) {
				cascadeFieldName = rootVar.getVarName() + criticalVariable.getVarID().substring(splitIndex);
			} else {
				cascadeFieldName = rootVar.getVarName();
			}

			if (!cascadeFieldName.equals(var.getVarName())) {
				question.append(isFirstVar ? "where\n`" : "`");
				question.append(var.getVarName());
				question.append("` has the same memory address as `");
				question.append(cascadeFieldName);
				question.append("`,\n");
				isFirstVar = false;
			}
		}

		question.append("`" + rootVarName + "` has a field called `");

		String cascadeFieldName = "";
		int stopIndex = criticalVariables.size() - 1;
		for (int i = 0; i < stopIndex; i++) {
			VarValue criticalVar = criticalVariables.get(i);
			cascadeFieldName += criticalVar.getVarName() + ".";
		}
		cascadeFieldName += targetVarName;

		question.append(cascadeFieldName);
		question.append("`, does the code directly or indirectly write this field?"
				+ "\nIn your response, return T for true and F for false. Do not include explanation.");

		return question.toString();
	}

	public static boolean isModified(String response) {
		response = response.trim();
		return response.equals("T") ? true : false;
	}

}
