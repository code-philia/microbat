package microbat.tracerecov.executionsimulator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

public class ExecutionSimulationFileLogger extends ExecutionSimulationLogger {

	private String filePath;

	public ExecutionSimulationFileLogger() {
		this.filePath = "../log.txt";
	}

	private PrintWriter createWriter() throws IOException {
		FileWriter fileWriter = new FileWriter(this.filePath, true);
		PrintWriter writer = new PrintWriter(fileWriter);
		return writer;
	}

	@Override
	public void printInfoBeforeQuery(String header, VarValue selectedVar, TraceNode step, String request) {
		if (!isLoggingEnabled) {
			return;
		}
		
		PrintWriter writer;
		try {
			writer = createWriter();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		writer.println("\n***" + header + "***\n");

		if (showDebugInfo) {
			writer.println("Step Number: " + step.getOrder() + "\nLine Number: " + step.getLineNumber());
			writer.println("\n\nSelected Variable: `" + selectedVar.getVarName() + "`\nType: "
					+ selectedVar.getType() + "\nAlias ID: " + selectedVar.getAliasVarID() + "\nValue: "
					+ selectedVar.getStringValue() + "\n");
		}

		writer.println(request);
		
		writer.close();
	}

	@Override
	public void printResponse(int ithTry, String response) {
		if (isLoggingEnabled) {
			PrintWriter writer;
			try {
				writer = createWriter();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			writer.println();
			writer.println(ithTry + "th try with LLM to generate response as \n" + response);
			
			writer.close();
		}
	}

	@Override
	public void printError(String errorMessage) {
		if (isLoggingEnabled) {
			PrintWriter writer;
			try {
				writer = createWriter();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			writer.println();
			writer.println(errorMessage);
			
			writer.close();
		}
	}
}
