package microbat.tracerecov.autoprompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.json.JSONException;
import org.json.JSONObject;

import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.dataset.AliasInferenceDatasetReader;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;
import microbat.tracerecov.autoprompt.dataset.LossDataCollector;
import microbat.tracerecov.varskeleton.VarSkeletonParser;
import microbat.tracerecov.varskeleton.VariableSkeleton;

public class AliasInferenceExampleSearcher extends ExampleSearcher {
	private ArrayList<HashMap<String, String>> trainingDataset;
	private ArrayList<HashMap<String, String>> testingDataset;
	private VarSkeletonParser varSkeletonParser;
	private PromptTemplateFiller promptTemplateFiller;

	public AliasInferenceExampleSearcher() {
		DatasetReader datasetReader = new AliasInferenceDatasetReader();
		ArrayList<ArrayList<HashMap<String, String>>> datasets = datasetReader.getTrainingAndTestingDataset();
		trainingDataset = datasets.get(0);
		testingDataset = datasets.get(1);
		varSkeletonParser = new VarSkeletonParser();
		promptTemplateFiller = new AliasInferencePromptTemplateFiller();
	}

	@Override
	public String searchForExample(HashMap<String, String> datapoint) {
		/* keys */
		String varsInStepKey = DatasetReader.VARS_IN_STEP;
		String targetVarKey = DatasetReader.TARGET_VAR;
		String invokedMethodsKey = DatasetReader.INVOKED_METHODS;

		/* datapoint features */
		// feature 1
		JSONObject variablesInStep = new JSONObject(datapoint.get(varsInStepKey));

		// feature 2
		String targetVar = datapoint.get(targetVarKey);
		JSONObject targetVariable = new JSONObject(targetVar.split(":", 2)[1]);
		VariableSkeleton targetVarSkeleton = varSkeletonParser.parseVariableValueJSONObject(targetVariable);

		// feature 3
		String[] invokedMethods = TraceRecovUtils.parseArrayFromString(datapoint.get(invokedMethodsKey));

		/* search for closest example */
		double minDiffScore = 1;
		double numOfFeatures = 3;
		int datapointIndex = 0;
		for (int i = 0; i < trainingDataset.size(); i++) {
			HashMap<String, String> example = trainingDataset.get(i);

			/* example features */
			// feature 1
			JSONObject exampleVariablesInStep = new JSONObject(example.get(varsInStepKey));

			// feature 2
			String exampleTargetVar = example.get(targetVarKey);
			JSONObject exampleTargetVariable = new JSONObject(exampleTargetVar.split(":", 2)[1]);
			VariableSkeleton exampleTargetVarSkeleton = varSkeletonParser
					.parseVariableValueJSONObject(exampleTargetVariable);

			// feature 3
			String[] exampleInvokedMethods = TraceRecovUtils.parseArrayFromString(example.get(invokedMethodsKey));

			/* calculate diffScore */
			double diffScore = 0;
			diffScore += getDiffScoreBetweenTypesInJSON(variablesInStep, exampleVariablesInStep);
			diffScore += targetVarSkeleton.getDifferenceScore(exampleTargetVarSkeleton);
			diffScore += getDiffScoreBetweenArrays(invokedMethods, exampleInvokedMethods);

			diffScore /= numOfFeatures;
			if (diffScore < minDiffScore) {
				minDiffScore = diffScore;
				datapointIndex = i;
			}
		}

		HashMap<String, String> closestExample = trainingDataset.get(datapointIndex);
		String groundTruth = TraceRecovUtils.processInputStringForLLM(closestExample.get(DatasetReader.GROUND_TRUTH));
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
		LossCalculator lossCalculator = new AliasInferenceLossCalculator();
		return lossCalculator.computeLoss(outputJSON, groundTruthJSON);
	}

	/**
	 * 1 - intersection/union
	 */
	private double getDiffScoreBetweenTypesInJSON(JSONObject object1, JSONObject object2) {
		double diffScore = 0;
		double totalCount = 0;

		Map<String, Integer> typeCount = new HashMap<>();
		for (String key : object1.keySet()) {
			String[] nameAndType = key.split(":");
			String type = nameAndType[1];
			if (!typeCount.containsKey(type)) {
				typeCount.put(type, 0);
			}
			typeCount.put(type, typeCount.get(type) + 1);
			diffScore += 1; // to be reduced later
			totalCount += 1;
		}

		for (String key : object2.keySet()) {
			String[] nameAndType = key.split(":");
			String type = nameAndType[1];
			if (!typeCount.containsKey(type) || typeCount.get(type) <= 0) {
				diffScore += 1;
				totalCount += 1;
			} else {
				typeCount.put(type, typeCount.get(type) - 1);
				diffScore -= 1; // reduced
			}
		}
		return diffScore / totalCount;
	}

	/**
	 * 1 - intersection/union
	 */
	private double getDiffScoreBetweenArrays(String[] array1, String[] array2) {
		double diffScore = 0;
		double totalCount = 0;

		Map<String, Integer> itemCount = new HashMap<>();
		for (String item : array1) {
			if (!itemCount.containsKey(item)) {
				itemCount.put(item, 0);
			}
			itemCount.put(item, itemCount.get(item) + 1);
			diffScore += 1; // to be reduced later
			totalCount += 1;
		}

		for (String item : array2) {
			if (!itemCount.containsKey(item) || itemCount.get(item) <= 0) {
				diffScore += 1;
				totalCount += 1;
			} else {
				itemCount.put(item, itemCount.get(item) - 1);
				diffScore -= 1; // reduced
			}
		}
		return diffScore / totalCount;
	}

	public static void main(String[] args) {
		AliasInferenceExampleSearcher aliasInferenceExampleSearcher = new AliasInferenceExampleSearcher();
		aliasInferenceExampleSearcher.recordLoss();
	}
}
