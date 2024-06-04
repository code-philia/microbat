package microbat.tracerecov.executionsimulator;

import org.json.JSONObject;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.VariableOfInterest;

public class DefinitionInferenceUtils {

	/* Request content */

	private static final String DEFINITION_INFERENCE_BACKGROUND = "<Background>\n"
			+ "You are a Java expert, you need to analyze the runtime execution of a Java program.";

	/* Methods */

	public static String getBackgroundContent() {
		return DEFINITION_INFERENCE_BACKGROUND;
	}

	public static String getQuestionContent(TraceNode step, VarValue rootVar, VarValue targetVar) {
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils.getSourceCode(location, lineNo).trim();

		/* variable properties */
		String rootVarName = rootVar.getVarName();
		String targetVarName = targetVar.getVarName();

		/* type structure */
		JSONObject typeStructure = VariableOfInterest.getVariableOfInterestForDefinitionInferencing();
		String jsonString = typeStructure.toString();

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

		question.append("we know that `");
		question.append(rootVarName);
		question.append("` has the following structure:\n");
		question.append(jsonString);
		question.append("\nThis variable has a field called `");
		question.append(targetVarName);
		question.append("`, does the code change the value of this field?"
				+ "\nIn your response, return T for true and F for false. Do not include explanation.");

		return question.toString();
	}

	public static boolean isModified(String response) {
		response = response.trim();
		return response.equals("T") ? true : false;
	}

}
