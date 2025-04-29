package microbat.tracerecov.executionsimulator;

import microbat.Activator;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.preference.RecovSlicingPreference;

public class ExecutionSimulationLogger {

	public boolean isLoggingEnabled = Activator.getDefault().getPreferenceStore()
			.getString(RecovSlicingPreference.ENABLE_LOGGING).equals("true");

	public boolean showDebugInfo = Activator.getDefault().getPreferenceStore()
			.getString(RecovSlicingPreference.LOG_DEBUG_INFO).equals("true");

	public ExecutionSimulationLogger() {
	}

	public void printInfoBeforeQuery(String header, VarValue selectedVar, TraceNode step, String request) {
		if (!isLoggingEnabled) {
			return;
		}

		System.out.println("\n***" + header + "***\n");

		if (showDebugInfo) {
			System.out.println("Step Number: " + step.getOrder() + "\nLine Number: " + step.getLineNumber());
			System.out.println("\n\nSelected Variable: `" + selectedVar.getVarName() + "`\nType: "
					+ selectedVar.getType() + "\nAlias ID: " + selectedVar.getAliasVarID() + "\nValue: "
					+ selectedVar.getStringValue() + "\n");
		}

		System.out.println(request);
	}

	public void printResponse(int ithTry, String response) {
		if (isLoggingEnabled) {
			System.out.println();
			System.out.println(ithTry + "th try with LLM to generate response as \n" + response);
		}
	}

	public void printError(String errorMessage) {
		if (isLoggingEnabled) {
			System.out.println();
			System.out.println(errorMessage);
		}
	}
}
