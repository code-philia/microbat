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
	private static final double LOSS_THRESHOLD = 0.25;

	private ArrayList<HashMap<String, String>> dataset;
	private ExecutionSimulator executionSimulator;
	private LossCalculator lossCalculator;
	private TextualLossGenerator textualLossGeneartor;

	public AutoPromptEngineer() {
		dataset = readDataset();
		executionSimulator = new ExecutionSimulator();
		lossCalculator = new LossCalculator();
		textualLossGeneartor = new TextualLossGenerator();
	}

	public String adjustVariableExpansionPromptExample() {
		for (HashMap<String, String> datapoint : dataset) {
			JSONObject groundTruthJSON = new JSONObject(datapoint.get("ground_truth"));

			String originalExample = PromptTemplateFiller.getVariableExpansionPromptExample();
			String originalPrompt = PromptTemplateFiller.getVariableExpansionPrompt(datapoint);

			try {
				// get original output
				String originalOutput = getLLMOutput(originalPrompt);
				String textualLoss = null;
				JSONObject outputJSON = null;
				try {
					outputJSON = new JSONObject(originalOutput);
				} catch (JSONException jsonException) {
					textualLoss = textualLossGeneartor.getLossFromException(originalOutput, jsonException);
				}

				// compute original loss
				double originalLoss = 1;
				if (outputJSON != null && groundTruthJSON != null) {
					originalLoss = lossCalculator.computeLoss(outputJSON, groundTruthJSON);
					// TODO: get textual loss
					if (originalLoss <= LOSS_THRESHOLD) {
						continue;
					}
				}

				// update example
				String originalAdjustmentPrompt = PromptTemplateFiller.getVariableExpansionAdjustmentPrompt(datapoint,
						textualLoss);
				String updatedExample = executionSimulator.sendRequest("", originalAdjustmentPrompt);
				PromptTemplateFiller.setVariableExpansionPromptExample(updatedExample);

				// compute new loss
				String updatedPrompt = PromptTemplateFiller.getVariableExpansionPrompt(datapoint);
				String updatedOutput = getLLMOutput(updatedPrompt);
				JSONObject updatedOutputJSON;
				try {
					updatedOutputJSON = new JSONObject(updatedOutput);
				} catch (JSONException jsonException) {
					// change back to original example
					PromptTemplateFiller.setVariableExpansionPromptExample(originalExample);
					continue;
				}
				double updatedLoss = lossCalculator.computeLoss(updatedOutputJSON, groundTruthJSON);
				if (updatedLoss > originalLoss) {
					// change back to original example
					PromptTemplateFiller.setVariableExpansionPromptExample(originalExample);
				}
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

		for (HashMap<String, String> datapoint : dataset) {
			JSONObject groundTruthJSON = new JSONObject(datapoint.get("ground_truth"));

			String request = PromptTemplateFiller.getVariableExpansionPrompt(datapoint);
			String output = getLLMOutput(request);

			JSONObject outputJSON;
			try {
				outputJSON = new JSONObject(output);
			} catch (JSONException jsonException) {
				loss += 1;
				continue;
			}

			// compute loss
			LossCalculator lossCalculator = new LossCalculator();
			loss += lossCalculator.computeLoss(outputJSON, groundTruthJSON);
		}

		return loss / (double) dataset.size();
	}

	private ArrayList<HashMap<String, String>> readDataset() {
		DatasetReader datasetReader = new DatasetReader();
		return datasetReader.readVariableExpansionDataset();
	}

	private String getLLMOutput(String request) {
		String output;
		try {
			output = executionSimulator.sendRequest("", request);
			int begin = output.indexOf("{");
			int end = output.lastIndexOf("}");
			return output.substring(begin, end + 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		AutoPromptEngineer autoPromptEngineer = new AutoPromptEngineer();
		double originalAvgLoss = autoPromptEngineer
				.getAverageLoss(PromptTemplateFiller.getVariableExpansionPromptExample());
		String newExample = autoPromptEngineer.adjustVariableExpansionPromptExample();
		double updatedAvgLoss = autoPromptEngineer.getAverageLoss(newExample);

		System.out.println("Original Average Loss: " + originalAvgLoss);
		System.out.println("Updated Average Loss: " + updatedAvgLoss);
		System.out.println("New Example: ");
		System.out.println(newExample);
	}

}
