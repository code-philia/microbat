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

public class DefinitionInferenceExampleSearcher extends ExampleSearcher {

	private static double DIFF_SCORE_THRESHOLD = 0.3;

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
	public String searchForExample(HashMap<String, String> datapoint) {
		/* keys */
		String targetFieldKey = DatasetReader.TARGET_FIELD;
		String targetVarKey = DatasetReader.TARGET_VAR;
		String groundTruthKey = DatasetReader.GROUND_TRUTH;

		/* datapoint features */
		String targetField = datapoint.get(targetFieldKey);
		int targetFieldLevel = targetField.split("\\.").length;

		String targetVar = datapoint.get(targetVarKey);
		JSONObject targetVariable = new JSONObject(targetVar);
		VariableSkeleton targetVarSkeleton = varSkeletonParser.parseVariableValueJSONObject(targetVariable);

		/* search for closest example */
		double minDiffScore = 1;
		int minFieldLevelScore = Integer.MAX_VALUE;
		int datapointIndex = 0;
		for (int i = 0; i < trainingDataset.size(); i++) {
			HashMap<String, String> example = trainingDataset.get(i);

			// diff score based on variable structure
			String exampleTargetVar = example.get(targetVarKey);
			JSONObject exampleTargetVariable = new JSONObject(exampleTargetVar);
			VariableSkeleton exampleTargetVarSkeleton = varSkeletonParser
					.parseVariableValueJSONObject(exampleTargetVariable);

			double diffScore = targetVarSkeleton.getDifferenceScore(exampleTargetVarSkeleton);
			
			if (diffScore <= minDiffScore) {
				// diff score based on target field level
				String exampleTargetField = example.get(targetFieldKey);
				int exampleTargetFieldLevel = exampleTargetField.split("\\.").length;
				int diffScoreBasedOnTargetField = Math.abs(exampleTargetFieldLevel - targetFieldLevel);
				
				if (diffScore < minDiffScore || diffScoreBasedOnTargetField < minFieldLevelScore) {
					minFieldLevelScore = diffScoreBasedOnTargetField;
					minDiffScore = diffScore;
					datapointIndex = i;
				}
			}
		}

		if (minDiffScore < DIFF_SCORE_THRESHOLD) {
			HashMap<String, String> closestExample = trainingDataset.get(datapointIndex);
			String groundTruth = TraceRecovUtils.processInputStringForLLM(closestExample.get(groundTruthKey));
			return promptTemplateFiller.getExample(closestExample, groundTruth);
		} else {
			return "";
		}
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
