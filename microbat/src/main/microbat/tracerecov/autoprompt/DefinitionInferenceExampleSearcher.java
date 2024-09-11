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
	private ArrayList<HashMap<String, String>> trainingDataset;
	private ArrayList<HashMap<String, String>> testingDataset;
	private VarSkeletonParser varSkeletonParser;
	private PromptTemplateFiller promptTemplateFiller;

	public DefinitionInferenceExampleSearcher() {
		DatasetReader datasetReader = new DefinitionInferenceDatasetReader();
		ArrayList<ArrayList<HashMap<String, String>>> datasets = datasetReader.getTrainingAndTestingDataset();
		trainingDataset = datasets.get(0);
		testingDataset = datasets.get(1);
		varSkeletonParser = new VarSkeletonParser();
		promptTemplateFiller = new DefinitionInferencePromptTemplateFiller();
	}

	@Override
	public String searchForExample(HashMap<String, String> datapoint) {
		/* keys */
		String targetVarKey = DatasetReader.TARGET_VAR;
		String groundTruthKey = DatasetReader.GROUND_TRUTH;

		/* datapoint features */
		String targetVar = datapoint.get(targetVarKey);
		JSONObject targetVariable = new JSONObject(targetVar);
		VariableSkeleton targetVarSkeleton = varSkeletonParser.parseVariableValueJSONObject(targetVariable);

		/* search for closest example */
		double minDiffScore = 1;
		int datapointIndex = 0;
		for (int i = 0; i < trainingDataset.size(); i++) {
			HashMap<String, String> example = trainingDataset.get(i);

			String exampleTargetVar = example.get(targetVarKey);
			JSONObject exampleTargetVariable = new JSONObject(exampleTargetVar);
			VariableSkeleton exampleTargetVarSkeleton = varSkeletonParser
					.parseVariableValueJSONObject(exampleTargetVariable);

			double diffScore = targetVarSkeleton.getDifferenceScore(exampleTargetVarSkeleton);
			if (diffScore < minDiffScore) {
				minDiffScore = diffScore;
				datapointIndex = i;
			}
		}

		HashMap<String, String> closestExample = trainingDataset.get(datapointIndex);
		String groundTruth = TraceRecovUtils.processInputStringForLLM(closestExample.get(groundTruthKey));
		return promptTemplateFiller.getExample(closestExample, groundTruth);
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
