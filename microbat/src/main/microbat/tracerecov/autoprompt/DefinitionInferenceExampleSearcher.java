package microbat.tracerecov.autoprompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import org.json.JSONObject;

import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;
import microbat.tracerecov.autoprompt.dataset.DefinitionInferenceDatasetReader;
import microbat.tracerecov.varskeleton.VarSkeletonParser;
import microbat.tracerecov.varskeleton.VariableSkeleton;
import sav.strategies.dto.AppJavaClassPath;

public class DefinitionInferenceExampleSearcher extends ExampleSearcher {

	private static double[] WEIGHTS = new double[] { 0.5, 0.5 };
	private static double SIM_SCORE_THRESHOLD = 0.7;

	private ArrayList<HashMap<String, String>> trainingDataset;
	private ArrayList<HashMap<String, String>> testingDataset;
	private VarSkeletonParser varSkeletonParser;
	private PromptTemplateFiller promptTemplateFiller;
	private SimilarityScoreCalculator simScoreCalculator;

	public DefinitionInferenceExampleSearcher() {
		this(false);
	}

	public DefinitionInferenceExampleSearcher(boolean useFullDataset) {
		DatasetReader datasetReader = new DefinitionInferenceDatasetReader();
		if (useFullDataset) {
			trainingDataset = datasetReader.readCompleteDataset();
			testingDataset = new ArrayList<>();
		} else {
			ArrayList<ArrayList<HashMap<String, String>>> datasets = datasetReader.getTrainingAndTestingDataset();
			trainingDataset = datasets.get(0);
			testingDataset = datasets.get(1);
		}

		varSkeletonParser = new VarSkeletonParser();
		promptTemplateFiller = new DefinitionInferencePromptTemplateFiller();
		simScoreCalculator = new SimilarityScoreCalculator();
	}
	
	@Override
	public Object[] searchForExample(HashMap<String, String> datapoint) {
		/* keys */
		String targetVarKey = DatasetReader.TARGET_VAR;
		String sourceCodeKey = DatasetReader.LINE_SOURCE_CODE;
		String groundTruthKey = DatasetReader.GROUND_TRUTH;

		/* datapoint features */
		String sourceCode = datapoint.get(sourceCodeKey);

		String targetVar = datapoint.get(targetVarKey);
		JSONObject targetVariable = new JSONObject(targetVar);
		VariableSkeleton targetVarSkeleton = varSkeletonParser.parseVariableValueJSONObject(targetVariable);

		/* search for closest example */
		double maxSimScore = 0;
		int datapointIndex = 0;
		for (int i = 0; i < trainingDataset.size(); i++) {
			HashMap<String, String> example = trainingDataset.get(i);

			String exampleSourceCode = example.get(sourceCodeKey);

			String exampleTargetVar = example.get(targetVarKey);
			JSONObject exampleTargetVariable = new JSONObject(exampleTargetVar);
			VariableSkeleton exampleTargetVarSkeleton = varSkeletonParser
					.parseVariableValueJSONObject(exampleTargetVariable);

			double codeSimScore = simScoreCalculator.getSimilarityRatioBasedOnLCS(sourceCode, exampleSourceCode);
			double classSimScore = simScoreCalculator.getJaccardCoefficient(targetVarSkeleton,
					exampleTargetVarSkeleton);

			double simScore = simScoreCalculator.getCombinedScore(new double[] { codeSimScore, classSimScore },
					WEIGHTS);

			if (simScore > maxSimScore) {
				maxSimScore = simScore;
				datapointIndex = i;
			}
		}

		HashMap<String, String> closestExample = trainingDataset.get(datapointIndex);
		String groundTruth = TraceRecovUtils.processInputStringForLLM(closestExample.get(groundTruthKey));
		
		Object[] outputArray = new Object[2];
		outputArray[0] = promptTemplateFiller.getExample(closestExample, groundTruth);
		outputArray[1] = maxSimScore;
		return outputArray;
	}

	@Override
	public String searchForExample(HashMap<String, String> datapoint, AppJavaClassPath appJavaClassPath) {
		Object[] existingExample = searchForExample(datapoint);
		String closestExample = (String) existingExample[0];
		double maxSimScore = (double) existingExample[1];

		if (maxSimScore <= SIM_SCORE_THRESHOLD) {
			// TODO
		} else {
			return closestExample;
		}
		
		return null;
	}

	// TODO: update later (not used for now)

	/**
	 * For testing purpose
	 */
	@Override
	public void recordLoss() {
	}

	@Override
	protected double getLoss(HashMap<String, String> datapoint, Function<HashMap<String, String>, String> operation) {
		return 0;
	}

}
