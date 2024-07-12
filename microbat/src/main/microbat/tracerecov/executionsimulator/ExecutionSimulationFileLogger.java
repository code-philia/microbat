package microbat.tracerecov.executionsimulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONObject;

import microbat.Activator;
import microbat.codeanalysis.runtime.Condition;
import microbat.model.trace.Trace;
import microbat.preference.TraceRecovPreference;

public class ExecutionSimulationFileLogger extends ExecutionSimulationLogger {

	private String filePath;

	public ExecutionSimulationFileLogger() {
		String fileName = "gt.csv";
		this.filePath = Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.PROMPT_GT_PATH)
				+ File.separator + fileName;
	}

	private PrintWriter createWriter() throws IOException {
		FileWriter fileWriter = new FileWriter(this.filePath, true);
		PrintWriter writer = new PrintWriter(fileWriter);
		return writer;
	}

	public void collectGT(Condition condition, Trace trace) {
		JSONObject groundTruthJSON = condition.getMatchedGroundTruth(trace);
		if (groundTruthJSON == null) {
			return;
		}

		// write to results file
		PrintWriter writer;
		try {
			writer = createWriter();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		writer.println(getCsvContent(condition, groundTruthJSON));
		writer.close();
	}

	private String getCsvContent(Condition condition, JSONObject groundTruthJSON) {
		String variableName = condition.getVariableName();
		String variableType = condition.getVariableType();
		String variableValue = condition.getVariableValue();
		String groundTruth = groundTruthJSON.toString();

		StringBuilder stringBuilder = new StringBuilder();
		String delimiter = "#";
		stringBuilder.append(variableName);
		stringBuilder.append(delimiter);
		stringBuilder.append(variableType);
		stringBuilder.append(delimiter);
		stringBuilder.append(variableValue);
		stringBuilder.append(delimiter);
		stringBuilder.append(groundTruth);
		stringBuilder.append("\n");

		return stringBuilder.toString();
	}

}
