package microbat.tracerecov.autoprompt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

import microbat.tracerecov.executionsimulator.ExecutionSimulator;

/**
 * This class is used to adjust prompt automatically.
 */
public class AutoPromptEngineer {

	// TODO: update this, threshold must be in range (0,1)
	private static final double LOSS_THRESHOLD = 0.5;

	public String adjustVariableExpansionPromptExample() {
		// read in dataset
		DatasetReader datasetReader = new DatasetReader();
		ArrayList<HashMap<String, String>> dataset = datasetReader.readVariableExpansionDataset();

		ExecutionSimulator executionSimulator = new ExecutionSimulator();

		for (HashMap<String, String> datapoint : dataset) {
			String originalPrompt = PromptTemplateFiller.getVariableExpansionPrompt(datapoint);
			String originalAdjustmentPrompt = PromptTemplateFiller.getVariableExpansionAdjustmentPrompt(datapoint);

			try {
				// get output of the original prompt
				String originalOutput = executionSimulator.sendRequest("", originalPrompt);
				int begin = originalOutput.indexOf("{");
				int end = originalOutput.lastIndexOf("}");
				originalOutput = originalOutput.substring(begin, end + 1);

				JSONObject outputJSON;
				JSONObject groundTruthJSON;
				try {
					outputJSON = new JSONObject(originalOutput);
					groundTruthJSON = new JSONObject(datapoint.get("ground_truth"));
				} catch (JSONException jsonException) {
					// TODO: adjust prompt to generate valid JSON
					continue;
				}

				LossCalculator lossCalculator = new LossCalculator();
				double loss = lossCalculator.computeLoss(outputJSON, groundTruthJSON);
				if (loss < LOSS_THRESHOLD) {
					continue;
				}

//				System.out.println();
//				String updatedExample = executionSimulator.sendRequest("", originalAdjustmentPrompt);
//				PromptTemplateFiller.setVariableExpansionPromptExample(updatedExample);
//				System.out.println();
//				String updatedPrompt = PromptTemplateFiller.getVariableExpansionPrompt(datapoint);
//				String updatedOutput = executionSimulator.sendRequest("", updatedPrompt);
//				System.out.println();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return PromptTemplateFiller.getVariableExpansionPromptExample();
	}

	public static void main(String[] args) {
		AutoPromptEngineer autoPromptEngineer = new AutoPromptEngineer();
		String newExample = autoPromptEngineer.adjustVariableExpansionPromptExample();
	}

}
