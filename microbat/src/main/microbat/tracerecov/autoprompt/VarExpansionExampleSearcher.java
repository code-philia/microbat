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
import microbat.tracerecov.executionsimulator.LLMResponseType;
import microbat.tracerecov.varskeleton.VarSkeletonParser;
import microbat.tracerecov.varskeleton.VariableSkeleton;

public class VarExpansionExampleSearcher extends ExampleSearcher {

	private static double[] WEIGHTS = new double[] { 1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0 };

	private ArrayList<HashMap<String, String>> trainingDataset;
	private ArrayList<HashMap<String, String>> testingDataset;
	private VarSkeletonParser varSkeletonParser;
	private PromptTemplateFiller promptTemplateFiller;
	private SimilarityScoreCalculator simScoreCalculator;

	public VarExpansionExampleSearcher() {
		this(false);
	}

	public VarExpansionExampleSearcher(boolean useFullDataset) {
		DatasetReader datasetReader = new VarExpansionDatasetReader();
		if (useFullDataset) {
			trainingDataset = datasetReader.readCompleteDataset();
			testingDataset = new ArrayList<>();
		} else {
			ArrayList<ArrayList<HashMap<String, String>>> datasets = datasetReader.getTrainingAndTestingDataset();
			trainingDataset = datasets.get(0);
			testingDataset = datasets.get(1);
		}

		varSkeletonParser = new VarSkeletonParser();
		promptTemplateFiller = new VarExpansionPromptTemplateFiller();
		simScoreCalculator = new SimilarityScoreCalculator();
	}

	@Override
	public String searchForExample(HashMap<String, String> datapoint) {
		/* keys */
		String classStructureKey = DatasetReader.CLASS_STRUCTURE;
		String sourceCodeKey = DatasetReader.SOURCE_CODE;
		String varValueKey = DatasetReader.VAR_VALUE;
		String groundTruthKey = DatasetReader.GROUND_TRUTH;

		/* datapoint features */
		// class
		String classStructure = datapoint.get(classStructureKey);
		VariableSkeleton varSkeleton = varSkeletonParser.parseClassStructure(classStructure);
		// source code
		String sourceCode = datapoint.get(sourceCodeKey);
		// variable value
		String varValue = datapoint.get(varValueKey);

		/* search for closest example */
		double maxSimScore = 0;
		int datapointIndex = 0;
		for (int i = 0; i < trainingDataset.size(); i++) {
			HashMap<String, String> example = trainingDataset.get(i);
			// class
			String exampleClassStructure = example.get(classStructureKey);
			VariableSkeleton exampleVarSkeleton = varSkeletonParser.parseClassStructure(exampleClassStructure);
			// source code
			String exampleSourceCode = example.get(sourceCodeKey);
			// variable value
			String exampleVarValue = example.get(varValueKey);

			double codeSimScore = simScoreCalculator.getSimilarityRatioBasedOnLCS(sourceCode, exampleSourceCode);
			double varValueSimScore = simScoreCalculator.getSimilarityRatioBasedOnLCS(varValue, exampleVarValue);
			double classSimScore = simScoreCalculator.getJaccardCoefficient(varSkeleton, exampleVarSkeleton);

			double simScore = simScoreCalculator
					.getCombinedScore(new double[] { codeSimScore, varValueSimScore, classSimScore }, WEIGHTS);

			if (simScore > maxSimScore) {
				maxSimScore = simScore;
				datapointIndex = i;
			}
		}

		HashMap<String, String> closestExample = trainingDataset.get(datapointIndex);
		String groundTruth = TraceRecovUtils.processInputStringForLLM(closestExample.get(groundTruthKey));
		return promptTemplateFiller.getExample(closestExample, groundTruth);
	}

	@Override
	public void recordLoss() {
		LossDataCollector lossDataCollector = new LossDataCollector();

		for (HashMap<String, String> datapoint : testingDataset) {
			System.out.println("Baseline:\n");
			double baselineLoss = getLoss(datapoint, x -> promptTemplateFiller.getDefaultPrompt(x));

			System.out.println("Experiment:\n");
			double experimentLoss = getLoss(datapoint,
					x -> promptTemplateFiller.getPrompt(x, this.searchForExample(x)));

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

		String output = getLLMOutput(request, LLMResponseType.JSON);
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
		LossCalculator lossCalculator = new VarExpansionLossCalculator();
		return lossCalculator.computeLoss(outputJSON, groundTruthJSON);
	}

}
