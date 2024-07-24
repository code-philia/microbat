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
	private static final double LOSS_THRESHOLD = 0.4;

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

				// compute loss
				LossCalculator lossCalculator = new LossCalculator();
				double originalLoss = lossCalculator.computeLoss(outputJSON, groundTruthJSON);
				if (originalLoss < LOSS_THRESHOLD) {
					continue;
				}

				// update example
				String originalExample = PromptTemplateFiller.getVariableExpansionPromptExample();
				String updatedExample = executionSimulator.sendRequest("", originalAdjustmentPrompt);
				PromptTemplateFiller.setVariableExpansionPromptExample(updatedExample);

				// compute new loss
				String updatedPrompt = PromptTemplateFiller.getVariableExpansionPrompt(datapoint);
				String updatedOutput = executionSimulator.sendRequest("", updatedPrompt);
				begin = updatedOutput.indexOf("{");
				end = updatedOutput.lastIndexOf("}");
				updatedOutput = updatedOutput.substring(begin, end + 1);
				JSONObject updatedOutputJSON;
				try {
					updatedOutputJSON = new JSONObject(updatedOutput);
				} catch (JSONException jsonException) {
					// TODO: adjust prompt to generate valid JSON
					// change back to original example
					PromptTemplateFiller.setVariableExpansionPromptExample(originalExample);
					continue;
				}
				double updatedLoss = lossCalculator.computeLoss(updatedOutputJSON, groundTruthJSON);
				if (updatedLoss > originalLoss) {
					// change back to original example
					PromptTemplateFiller.setVariableExpansionPromptExample(originalExample);
				}
				System.out.println();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return PromptTemplateFiller.getVariableExpansionPromptExample();
	}

	/**
	 * For testing purpose.
	 */
	private double getAverageLoss(String example) {
		double loss = 0;
		PromptTemplateFiller.setVariableExpansionPromptExample(example);

		// read in dataset
		DatasetReader datasetReader = new DatasetReader();
		ArrayList<HashMap<String, String>> dataset = datasetReader.readVariableExpansionDataset();

		ExecutionSimulator executionSimulator = new ExecutionSimulator();

		for (HashMap<String, String> datapoint : dataset) {
			String prompt = PromptTemplateFiller.getVariableExpansionPrompt(datapoint);

			try {
				// get output
				String output = executionSimulator.sendRequest("", prompt);
				int begin = output.indexOf("{");
				int end = output.lastIndexOf("}");
				output = output.substring(begin, end + 1);

				JSONObject outputJSON;
				JSONObject groundTruthJSON;
				try {
					outputJSON = new JSONObject(output);
					groundTruthJSON = new JSONObject(datapoint.get("ground_truth"));
				} catch (JSONException jsonException) {
					loss += 1;
					continue;
				}

				// compute loss
				LossCalculator lossCalculator = new LossCalculator();
				loss += lossCalculator.computeLoss(outputJSON, groundTruthJSON);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return loss / (double) dataset.size();
	}

	public static void main(String[] args) {
		AutoPromptEngineer autoPromptEngineer = new AutoPromptEngineer();
		double originalAvgLoss = autoPromptEngineer
				.getAverageLoss(PromptTemplateFiller.getVariableExpansionPromptExample());

		String newExample = autoPromptEngineer.adjustVariableExpansionPromptExample();
		double updatedAvgLoss = autoPromptEngineer.getAverageLoss(newExample);

		System.out.println();
	}

}
