package microbat.tracerecov.executionsimulator;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;

public class CriticalVarUtils {

	/* Methods */

	public static String getPromptForVarIdentification(TraceNode slicingCriterion, String criticalVar) {
		StringBuilder content = new StringBuilder();
		
		content.append("Given code ```");
		int lineNo = slicingCriterion.getLineNumber();
		String location = slicingCriterion.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils
				.processInputStringForLLM(TraceRecovUtils.getSourceCodeOfALine(location, lineNo).trim());
		content.append(sourceCode);
		content.append("```");
		
		content.append("\n\nIdentify the variable name that is most likely to contain `");
		content.append(criticalVar);
		content.append("`");
		
		content.append("\r\n" + "Chose from the following variable names:\n");
		for (VarValue v : slicingCriterion.getReadVariables()) {
			content.append(v.getVarName() + "\n");
		}
		content.append("Do not include explanation.");
		
		return content.toString();
	}
	
	public static String getPromptForFieldIdentification(VarValue rootVar, TraceNode slicingCriterion, String criticalVar) {

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
