package microbat.tracerecov.executionsimulator;

import java.util.Map;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;

public class CriticalVarUtils {

	private final static GptTaskInfo taskInfoVar;
	private final static String promptUserVar;
	private final static GptTaskInfo taskInfoField;
	private final static String promptUserField;

	static {
		taskInfoVar = GptTaskInfo.IDENTIFY_CRITICAL_VARIABLE;
		promptUserVar = taskInfoVar.loadPromptUser();
		taskInfoField = GptTaskInfo.IDENTIFY_CRITICAL_FIELD;
		promptUserField = taskInfoField.loadPromptUser();
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

		return GptTaskInfo.formatPromptString(promptUserVar, values);
	}

	public static String getPromptForFieldIdentification(VarValue rootVar, TraceNode slicingCriterion,
			String criticalVar) {
		int lineNo = slicingCriterion.getLineNumber();
		String location = slicingCriterion.getBreakPoint().getFullJavaFilePath();
		String code = TraceRecovUtils
				.processInputStringForLLM(TraceRecovUtils.getSourceCodeOfALine(location, lineNo).trim());

		String value = rootVar.isExpansionAbstracted() ? rootVar.getAbstractedValue()
				: (rootVar.isExpanded() ? rootVar.toJSON().toString() : rootVar.getStringValue());

		String target = criticalVar;
		String name = rootVar.getVarName();

		StringBuilder options = new StringBuilder();
		for (VarValue v : rootVar.getAllDescedentChildren()) {
			options.append("- ").append(v.getVarName()).append("\n");
		}

		Map<String, String> values = Map.of(
				"code", code,
				"target", target,
				"name", name,
				"value", value,
				"options", options.toString());
		return GptTaskInfo.formatPromptString(promptUserField, values);
	}

}
