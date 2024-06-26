package microbat.tracerecov.executionsimulator;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

public class ExecutionSimulationLogger {

	public static boolean isLoggingEnabled;

	public static boolean showDebugInfo;
	public static boolean showRequest;
	public static boolean showResponse;

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

		if (showRequest) {
			System.out.println(request);
		}
	}

	public void printResponse(int ithTry, String response) {
		if (isLoggingEnabled && showResponse) {
			System.out.println();
			System.out.println(ithTry + "th try with LLM to generate response as \n" + response);
		}
	}

	public void printError(String errorMessage) {
		if (isLoggingEnabled && showResponse) {
			System.out.println();
			System.out.println(errorMessage);
		}
	}
}
