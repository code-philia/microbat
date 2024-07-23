package microbat.tracerecov.autoprompt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import microbat.tracerecov.executionsimulator.ExecutionSimulator;

public class AutoPromptEngineer {

	public static void main(String[] args) {
		DatasetReader datasetReader = new DatasetReader();
		ArrayList<HashMap<String, String>> dataset = datasetReader.readVariableExpansionDataset();

		ExecutionSimulator executionSimulator = new ExecutionSimulator();
		for (HashMap<String, String> datapoint : dataset) {
			String prompt = PromptTemplateFiller.getVariableExpansionAdjustmentPrompt(datapoint);

			try {
				String updatedExample = executionSimulator.sendRequest("", prompt);
				PromptTemplateFiller.setVariableExpansionPromptExample(updatedExample);
				System.out.println();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
