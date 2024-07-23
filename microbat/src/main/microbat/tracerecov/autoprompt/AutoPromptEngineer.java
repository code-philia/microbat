package microbat.tracerecov.autoprompt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import microbat.tracerecov.executionsimulator.ExecutionSimulator;

/**
 * This class is used to adjust prompt automatically.
 */
public class AutoPromptEngineer {

	public String adjustVariableExpansionPromptExample() {
		// read in dataset
		DatasetReader datasetReader = new DatasetReader();
		ArrayList<HashMap<String, String>> dataset = datasetReader.readVariableExpansionDataset();

		ExecutionSimulator executionSimulator = new ExecutionSimulator();
		for (HashMap<String, String> datapoint : dataset) {
			String originalAdjustmentPrompt = PromptTemplateFiller.getVariableExpansionAdjustmentPrompt(datapoint);
			String originalPrompt = PromptTemplateFiller.getVariableExpansionPrompt(datapoint);
			System.out.println();
			try {
				String originalOutput = executionSimulator.sendRequest("", originalPrompt);
				System.out.println();
				String updatedExample = executionSimulator.sendRequest("", originalAdjustmentPrompt);
				PromptTemplateFiller.setVariableExpansionPromptExample(updatedExample);
				System.out.println();
				String updatedPrompt = PromptTemplateFiller.getVariableExpansionPrompt(datapoint);
				String updatedOutput = executionSimulator.sendRequest("", updatedPrompt);
				System.out.println();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return PromptTemplateFiller.getVariableExpansionPromptExample();
	}

	public static void main(String[] args) {
		AutoPromptEngineer autoPromptEngineer = new AutoPromptEngineer();
		String newExample = autoPromptEngineer.adjustVariableExpansionPromptExample();
		System.out.println();
	}

}
