package microbat.tracerecov.executionsimulator;

import microbat.Activator;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.preference.TraceRecovPreference;

public class ExecutionSimulationLogger {

	public static boolean isLoggingEnabled = Activator.getDefault().getPreferenceStore()
			.getString(TraceRecovPreference.ENABLE_LOGGING).equals("true");

	public static boolean showDebugInfo = Activator.getDefault().getPreferenceStore()
			.getString(TraceRecovPreference.LOG_DEBUG_INFO).equals("true");

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
