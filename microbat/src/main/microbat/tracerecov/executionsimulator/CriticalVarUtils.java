package microbat.tracerecov.executionsimulator;

import java.util.Map;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;

public class CriticalVarUtils {

	private final static GptTaskInfo taskInfo;
	private final static String promptUser;

	static {
		taskInfo = GptTaskInfo.IDENTIFY_CRITICAL_VARIABLE;
		promptUser = taskInfo.loadPromptUser();
	}

	public static String getPromptForVarIdentification(TraceNode slicingCriterion, String criticalVar) {
		int lineNo = slicingCriterion.getLineNumber();
		String location = slicingCriterion.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils.getSourceCodeOfALine(location, lineNo).trim();

		StringBuilder variableNames = new StringBuilder();
		for (VarValue v : slicingCriterion.getReadVariables()) {
			variableNames.append("- ").append(v.getVarName()).append("\n");
		}

		Map<String, String> values = Map.of(
				"line", sourceCode,
				"variableNames", variableNames.toString(),
				"targetName", criticalVar);

		return GptTaskInfo.formatPromptString(promptUser, values);
	}

	public static String getPromptForFieldIdentification(VarValue rootVar, TraceNode slicingCriterion,
			String criticalVar) {

		StringBuilder content = new StringBuilder();

		content.append("Given the following data structure:\n");
		String fullValue = rootVar.isExpansionAbstracted() ? rootVar.getAbstractedValue()
				: (rootVar.isExpanded() ? rootVar.toJSON().toString() : rootVar.getStringValue());
		content.append(fullValue);

		content.append("\r\n" + "The variable is extracted from code ```");
		int lineNo = slicingCriterion.getLineNumber();
		String location = slicingCriterion.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils
				.processInputStringForLLM(TraceRecovUtils.getSourceCodeOfALine(location, lineNo).trim());
		content.append(sourceCode);
		content.append("```");

		content.append("\n\nFrom the above data structure, identify one field name that is most likely to be `");
		content.append(criticalVar);
		content.append("`");

		content.append("\r\n" + "Chose from the following field names:\n");
		for (VarValue v : rootVar.getAllDescedentChildren()) {
			content.append(v.getVarName() + "\n");
		}
		content.append("Do not include explanation.");

		return content.toString();

	}

}
