package microbat.tracerecov.autoprompt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

import microbat.tracerecov.autoprompt.dataset.DatasetReader;
import microbat.tracerecov.autoprompt.dataset.VarExpansionDatasetReader;
import microbat.tracerecov.executionsimulator.ExecutionSimulator;

/**
 * This class is used to adjust prompt automatically.
 */
public class AutoPromptEngineer {

	private static final double LOSS_THRESHOLD = 0.25;
	private static final int TRIAL_LIMIT = 3;

	private ArrayList<HashMap<String, String>> trainingDataset;
	private ArrayList<HashMap<String, String>> testingDataset;
	private ExecutionSimulator executionSimulator;
	private PromptTemplateFiller promptTemplateFiller;
	private LossCalculator lossCalculator;
	private TextualLossGenerator textualLossGeneartor;

	public AutoPromptEngineer() {
		// TODO: other types of prompts
		ArrayList<ArrayList<HashMap<String, String>>> datasets = readVarExpansionDatasets();
		trainingDataset = datasets.get(0);
		testingDataset = datasets.get(1);
		executionSimulator = new ExecutionSimulator();
		promptTemplateFiller = new PromptTemplateFiller();
		lossCalculator = new LossCalculator();
		textualLossGeneartor = new TextualLossGenerator();
	}

	public String adjustVariableExpansionPromptExample() {
		String updatedExample = promptTemplateFiller.getDefaultPromptExample();
		for (HashMap<String, String> datapoint : trainingDataset) {
			updatedExample = adjustVariableExpansionPromptExample(datapoint, updatedExample);
			System.out.println();
			System.out.println(updatedExample);
		}

		return updatedExample;
	}

	private String adjustVariableExpansionPromptExample(HashMap<String, String> datapoint, String originalExample) {

		JSONObject groundTruthJSON = new JSONObject(datapoint.get(DatasetReader.GROUND_TRUTH));
		String originalPrompt = promptTemplateFiller.getPrompt(datapoint, originalExample);

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
				return getFineTunedExample(datapoint, originalExample, numericalLoss, textualLoss);
			}
		} else {
			return getFineTunedExample(datapoint, originalExample, numericalLoss, textualLoss);
		}

		return originalExample;
	}

	private String getFineTunedExample(HashMap<String, String> datapoint, String originalExample, double numericalLoss,
			String textualLoss) {
		// generate updated example
		String originalAdjustmentPrompt = promptTemplateFiller.getAdjustmentPrompt(datapoint, originalExample);
		String updatedExample = null;
		try {
			updatedExample = executionSimulator.sendRequest("", originalAdjustmentPrompt);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// compute textual and numerical loss
		String updatedPrompt = promptTemplateFiller.getPrompt(datapoint, updatedExample);
		String updatedOutput = getLLMOutput(updatedPrompt);
		JSONObject updatedOutputJSON;
		try {
			updatedOutputJSON = new JSONObject(updatedOutput);
		} catch (JSONException jsonException) {
			String updatedTextualLoss = textualLossGeneartor.getLossFromException(updatedOutput, jsonException);
			// adjust example
			return adjustExampleGivenTextualLoss(datapoint, updatedExample, updatedOutput, numericalLoss,
					updatedTextualLoss, TRIAL_LIMIT);
		}

		JSONObject groundTruthJSON = new JSONObject(datapoint.get("ground_truth"));
		double updatedNumericalLoss = lossCalculator.computeLoss(updatedOutputJSON, groundTruthJSON);
		String updatedTextualLoss = textualLossGeneartor.getLoss(updatedOutputJSON, groundTruthJSON);

		if (updatedNumericalLoss <= LOSS_THRESHOLD) {
			return updatedExample;
		} else {
			// adjust example
			return adjustExampleGivenTextualLoss(datapoint, updatedExample, updatedOutput, updatedNumericalLoss,
					updatedTextualLoss, TRIAL_LIMIT);
		}
	}

	private String adjustExampleGivenTextualLoss(HashMap<String, String> datapoint, String originalExample,
			String output, double numericalLoss, String textualLoss, int trials) {
		if (trials <= 0) {
			return originalExample;
		}

		// update example
		String adjustmentPromptWithTextualLoss = promptTemplateFiller.getAdjustmentPromptWithLoss(originalExample,
				datapoint, output, textualLoss);
		String updatedExample = null;
		try {
			updatedExample = executionSimulator.sendRequest("", adjustmentPromptWithTextualLoss);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// compute textual and numerical loss
		String updatedPrompt = promptTemplateFiller.getPrompt(datapoint, updatedExample);
		String updatedOutput = getLLMOutput(updatedPrompt);
		JSONObject updatedOutputJSON;
		try {
			updatedOutputJSON = new JSONObject(updatedOutput);
		} catch (JSONException jsonException) {
			String updatedTextualLoss = textualLossGeneartor.getLossFromException(updatedOutput, jsonException);
			// retry with original example and textualLoss
			return adjustExampleGivenTextualLoss(datapoint, updatedExample, updatedOutput, numericalLoss,
					updatedTextualLoss, trials - 1);
		}

		JSONObject groundTruthJSON = new JSONObject(datapoint.get(DatasetReader.GROUND_TRUTH));
		double updatedNumericalLoss = lossCalculator.computeLoss(updatedOutputJSON, groundTruthJSON);
		String updatedTextualLoss = textualLossGeneartor.getLoss(updatedOutputJSON, groundTruthJSON);

		if (updatedNumericalLoss > numericalLoss) {
			// retry with original example
			return adjustExampleGivenTextualLoss(datapoint, originalExample, updatedOutput, numericalLoss, textualLoss,
					trials - 1);
		}
		if (updatedNumericalLoss <= LOSS_THRESHOLD) {
			return updatedExample;
		} else {
			// retry with updated example
			return adjustExampleGivenTextualLoss(datapoint, updatedExample, updatedOutput, updatedNumericalLoss,
					updatedTextualLoss, trials - 1);
		}
	}

	public double getAverageLoss() {
		return getAverageLoss(promptTemplateFiller.getDefaultPromptExample());
	}

	public double getAverageLoss(String example) {
		double loss = 0;

		for (HashMap<String, String> datapoint : testingDataset) {
			JSONObject groundTruthJSON = new JSONObject(datapoint.get(DatasetReader.GROUND_TRUTH));

			String request = promptTemplateFiller.getPrompt(datapoint, example);
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

	private ArrayList<ArrayList<HashMap<String, String>>> readVarExpansionDatasets() {
		DatasetReader datasetReader = new VarExpansionDatasetReader();
		return datasetReader.getTrainingAndTestingDataset();
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
