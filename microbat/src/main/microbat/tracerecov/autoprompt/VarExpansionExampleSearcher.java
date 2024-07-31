package microbat.tracerecov.autoprompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import org.json.JSONException;
import org.json.JSONObject;

import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;
import microbat.tracerecov.autoprompt.dataset.LossDataCollector;
import microbat.tracerecov.autoprompt.dataset.VarExpansionDatasetReader;
import microbat.tracerecov.varskeleton.VarSkeletonParser;
import microbat.tracerecov.varskeleton.VariableSkeleton;

public class VarExpansionExampleSearcher extends ExampleSearcher {
	private ArrayList<HashMap<String, String>> trainingDataset;
	private ArrayList<HashMap<String, String>> testingDataset;
	private VarSkeletonParser varSkeletonParser;
	private PromptTemplateFiller promptTemplateFiller;

	public VarExpansionExampleSearcher() {
		DatasetReader datasetReader = new VarExpansionDatasetReader();
		ArrayList<ArrayList<HashMap<String, String>>> datasets = datasetReader.getTrainingAndTestingDataset();
		trainingDataset = datasets.get(0);
		testingDataset = datasets.get(1);
		varSkeletonParser = new VarSkeletonParser();
		promptTemplateFiller = new PromptTemplateFiller();
	}

	@Override
	public String searchForExample(HashMap<String, String> datapoint) {
		String classStructureKey = DatasetReader.CLASS_STRUCTURE;
		String groundTruthKey = DatasetReader.GROUND_TRUTH;

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
	@Override
	public void recordLoss() {
		LossDataCollector lossDataCollector = new LossDataCollector();

		for (HashMap<String, String> datapoint : testingDataset) {
			System.out.println("Baseline:\n");
			double baselineLoss = getLoss(datapoint, x -> promptTemplateFiller.getDefaultVariableExpansionPrompt(x));

			System.out.println("Experiment:\n");
			double experimentLoss = getLoss(datapoint,
					x -> promptTemplateFiller.getVariableExpansionPrompt(x, this.searchForExample(x)));

			System.out.println("baseline loss: " + baselineLoss);
			System.out.println("experiment loss: " + experimentLoss);
			lossDataCollector.saveDataToFile(baselineLoss, experimentLoss);
		}
	}
	
	@Override
	protected double getLoss(HashMap<String, String> datapoint, Function<HashMap<String, String>, String> operation) {
		JSONObject groundTruthJSON = new JSONObject(datapoint.get(DatasetReader.GROUND_TRUTH));

		String request = operation.apply(datapoint);
		System.out.println(request);
		System.out.println();

		String output = getLLMOutput(request);
		System.out.println(output);
		System.out.println();

		JSONObject outputJSON;
		try {
			outputJSON = new JSONObject(output);
		} catch (JSONException jsonException) {
			System.out.println(jsonException.getMessage());
			System.out.println();

			return 1;
		}

		// compute loss
		LossCalculator lossCalculator = new LossCalculator();
		return lossCalculator.computeLoss(outputJSON, groundTruthJSON);
	}
}
