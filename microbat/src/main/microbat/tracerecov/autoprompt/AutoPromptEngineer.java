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
	private static final int TRIAL_LIMIT = 3;

	private ArrayList<HashMap<String, String>> trainingDataset;
	private ArrayList<HashMap<String, String>> testingDataset;
	private ExecutionSimulator executionSimulator;
	private PromptTemplateFiller promptTemplateFiller;
	private LossCalculator lossCalculator;
	private TextualLossGenerator textualLossGeneartor;

	public AutoPromptEngineer() {
		trainingDataset = readTrainingDataset();
		testingDataset = readTestingDataset();
		executionSimulator = new ExecutionSimulator();
		promptTemplateFiller = new PromptTemplateFiller();
		lossCalculator = new LossCalculator();
		textualLossGeneartor = new TextualLossGenerator();
	}

	public String adjustVariableExpansionPromptExample() {
		String updatedExample = promptTemplateFiller.getDefaultVariableExpansionPromptExample();
		for (HashMap<String, String> datapoint : trainingDataset) {
			updatedExample = adjustVariableExpansionPromptExample(datapoint, updatedExample);
			System.out.println();
			System.out.println(updatedExample);
		}

		return updatedExample;
	}

	private String adjustVariableExpansionPromptExample(HashMap<String, String> datapoint, String originalExample) {

		JSONObject groundTruthJSON = new JSONObject(datapoint.get("ground_truth"));
		String originalPrompt = promptTemplateFiller.getVariableExpansionPrompt(datapoint, originalExample);

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
		double numericalLoss = 1;
		if (outputJSON != null && groundTruthJSON != null) {
			numericalLoss = lossCalculator.computeLoss(outputJSON, groundTruthJSON);
			textualLoss = textualLossGeneartor.getLoss(outputJSON, groundTruthJSON);
			if (numericalLoss > LOSS_THRESHOLD) {
				return getFineTunedExample(datapoint, originalExample, numericalLoss, textualLoss, TRIAL_LIMIT);
			}
		} else {
			return getFineTunedExample(datapoint, originalExample, numericalLoss, textualLoss, TRIAL_LIMIT);
		}

		return originalExample;
	}

	private String getFineTunedExample(HashMap<String, String> datapoint, String originalExample, double numericalLoss,
			String textualLoss, int trials) {
		if (trials <= 0) {
			return originalExample;
		}

		// update example
		String originalAdjustmentPrompt = promptTemplateFiller.getVariableExpansionAdjustmentPrompt(datapoint,
				textualLoss, originalExample);
		String updatedExample = null;
		try {
			updatedExample = executionSimulator.sendRequest("", originalAdjustmentPrompt);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// compute textual and numerical loss
		String updatedPrompt = promptTemplateFiller.getVariableExpansionPrompt(datapoint, updatedExample);
		String updatedOutput = getLLMOutput(updatedPrompt);
		JSONObject updatedOutputJSON;
		try {
			updatedOutputJSON = new JSONObject(updatedOutput);
		} catch (JSONException jsonException) {
			String updatedTextualLoss = textualLossGeneartor.getLossFromException(updatedOutput, jsonException);
			// retry with original example and textualLoss
			return getFineTunedExample(datapoint, updatedExample, numericalLoss, updatedTextualLoss, trials - 1);
		}

		JSONObject groundTruthJSON = new JSONObject(datapoint.get("ground_truth"));
		double updatedNumericalLoss = lossCalculator.computeLoss(updatedOutputJSON, groundTruthJSON);
		String updatedTextualLoss = textualLossGeneartor.getLoss(updatedOutputJSON, groundTruthJSON);

		if (updatedNumericalLoss > numericalLoss) {
			// retry with original example
			return getFineTunedExample(datapoint, originalExample, numericalLoss, textualLoss, trials - 1);
		}
		if (updatedNumericalLoss <= LOSS_THRESHOLD) {
			return updatedExample;
		} else {
			// retry with updated example
			return getFineTunedExample(datapoint, updatedExample, updatedNumericalLoss, updatedTextualLoss, trials - 1);
		}
	}

	public double getAverageLoss() {
		return getAverageLoss(promptTemplateFiller.getDefaultVariableExpansionPromptExample());
	}

	public double getAverageLoss(String example) {
		double loss = 0;

		for (HashMap<String, String> datapoint : testingDataset) {
			JSONObject groundTruthJSON = new JSONObject(datapoint.get("ground_truth"));

			String request = promptTemplateFiller.getVariableExpansionPrompt(datapoint, example);
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

		return loss / (double) testingDataset.size();
	}

	private ArrayList<HashMap<String, String>> readTrainingDataset() {
		DatasetReader datasetReader = new DatasetReader();
		return datasetReader.readVariableExpansionTrainingDataset();
	}

	private ArrayList<HashMap<String, String>> readTestingDataset() {
		DatasetReader datasetReader = new DatasetReader();
		return datasetReader.readVariableExpansionTestingDataset();
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

}
