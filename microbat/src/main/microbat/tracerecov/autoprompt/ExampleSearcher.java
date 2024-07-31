package microbat.tracerecov.autoprompt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import org.json.JSONException;
import org.json.JSONObject;

import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.executionsimulator.ExecutionSimulator;
import microbat.tracerecov.varskeleton.VarSkeletonParser;
import microbat.tracerecov.varskeleton.VariableSkeleton;

/**
 * This class is used to search for the most relevant example from the database.
 */
public class ExampleSearcher {

	private ArrayList<HashMap<String, String>> trainingDataset;
	private ArrayList<HashMap<String, String>> testingDataset;
	private VarSkeletonParser varSkeletonParser;
	private PromptTemplateFiller promptTemplateFiller;
	private ExecutionSimulator executionSimulator;

	public ExampleSearcher() {
		DatasetReader datasetReader = new DatasetReader();
		ArrayList<ArrayList<HashMap<String, String>>> datasets = datasetReader.getTrainingAndTestingDataset();
		trainingDataset = datasets.get(0);
		testingDataset = datasets.get(1);
		varSkeletonParser = new VarSkeletonParser();
		promptTemplateFiller = new PromptTemplateFiller();
		executionSimulator = new ExecutionSimulator();
	}

	public String searchForExample(HashMap<String, String> datapoint) {
		String classStructureKey = "class_structure";
		String groundTruthKey = "ground_truth";

		String classStructure = datapoint.get(classStructureKey);
		VariableSkeleton varSkeleton = varSkeletonParser.parseClassStructure(classStructure);

		double minDiffScore = 1;
		int datapointIndex = 0;
		for (int i = 0; i < trainingDataset.size(); i++) {
			HashMap<String, String> example = trainingDataset.get(i);
			String exampleClassStructure = example.get(classStructureKey);
			VariableSkeleton exampleVarSkeleton = varSkeletonParser.parseClassStructure(exampleClassStructure);

			double diffScore = varSkeleton.getDifferenceScore(exampleVarSkeleton);
			if (diffScore < minDiffScore) {
				minDiffScore = diffScore;
				datapointIndex = i;
			}
		}

		HashMap<String, String> closestExample = trainingDataset.get(datapointIndex);
		String groundTruth = TraceRecovUtils.processInputStringForLLM(closestExample.get(groundTruthKey));
		return promptTemplateFiller.getExample(closestExample, groundTruth);
	}

	/**
	 * For testing purpose
	 */
	public void recordLoss() {
		LossDataCollector lossDataCollector = new LossDataCollector();

		for (HashMap<String, String> datapoint : testingDataset) {
			double baselineLoss = getLoss(datapoint, x -> promptTemplateFiller.getDefaultVariableExpansionPrompt(x));
			double experimentLoss = getLoss(datapoint,
					x -> promptTemplateFiller.getVariableExpansionPrompt(x, this.searchForExample(x)));
			lossDataCollector.saveDataToFile(baselineLoss, experimentLoss);
			break;
		}
	}

	/**
	 * Copied from {@code AutoPromptEngineer}
	 */
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

	private double getLoss(HashMap<String, String> datapoint, Function<HashMap<String, String>, String> operation) {
		JSONObject groundTruthJSON = new JSONObject(datapoint.get("ground_truth"));

		String request = operation.apply(datapoint);
		String output = getLLMOutput(request);

		JSONObject outputJSON;
		try {
			outputJSON = new JSONObject(output);
		} catch (JSONException jsonException) {
			return 1;
		}

		// compute loss
		LossCalculator lossCalculator = new LossCalculator();
		return lossCalculator.computeLoss(outputJSON, groundTruthJSON);
	}

	public static void main(String[] args) {
		ExampleSearcher exampleSearcher = new ExampleSearcher();
		exampleSearcher.recordLoss();
	}
}
