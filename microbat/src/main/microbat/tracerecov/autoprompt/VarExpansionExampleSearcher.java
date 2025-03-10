package microbat.tracerecov.autoprompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import org.json.JSONException;
import org.json.JSONObject;

import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;
import microbat.tracerecov.autoprompt.dataset.LossDataCollector;
import microbat.tracerecov.autoprompt.dataset.VarExpansionDatasetReader;
import microbat.tracerecov.autoprompt.incontextlearning.InContextEgGenerator;
import microbat.tracerecov.autoprompt.incontextlearning.InContextEgGenerator.InContextLearningCode;
import microbat.tracerecov.autoprompt.incontextlearning.InContextEgGenerator.InContextLearningVariables;
import microbat.tracerecov.autoprompt.incontextlearning.InContextLearning.InContextLearningType;
import microbat.tracerecov.executionsimulator.ExecutionSimulatorFactory;
import microbat.tracerecov.executionsimulator.LLMResponseType;
import microbat.tracerecov.varskeleton.VarSkeletonParser;
import microbat.tracerecov.varskeleton.VariableSkeleton;
import sav.strategies.dto.AppJavaClassPath;

public class VarExpansionExampleSearcher extends ExampleSearcher {

	private static double[] WEIGHTS = new double[] { 1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0 };
	private static double SIM_SCORE_THRESHOLD = 100;

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
	public Object[] searchForExample(HashMap<String, String> datapoint) {
		/* keys */
		String classStructureKey = DatasetReader.CLASS_STRUCTURE;
		String sourceCodeKey = DatasetReader.LINE_SOURCE_CODE;
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
			// generate example
			String sourceCodeKey = DatasetReader.METHOD_SOURCE_CODE;
			String lineSourceCodeKey = DatasetReader.LINE_SOURCE_CODE;
			String importsKey = DatasetReader.IMPORTS;
			String lineNoKey = DatasetReader.LINE_NO;
			String varTypeKey = DatasetReader.VAR_TYPE;
			String varNameKey = DatasetReader.VAR_NAME;
			String varValueKey = DatasetReader.VAR_VALUE;
			String classStructureKey = DatasetReader.CLASS_STRUCTURE;

			InContextEgGenerator egGenerator = new InContextEgGenerator();
			egGenerator.setExecutionSimulator(ExecutionSimulatorFactory.getExecutionSimulator());
			InContextLearningType type = InContextLearningType.VAR_EXPANSION;
			InContextLearningCode generatedCode = egGenerator.getGeneratedExampleCode(datapoint.get(importsKey),
					datapoint.get(sourceCodeKey), Integer.valueOf(datapoint.get(lineNoKey)), type,
					InContextEgGenerator.defaultToString());
			String loc = TraceRecovUtils.getLoc(generatedCode.getCode(), generatedCode.getMarkerLine());

			InContextLearningVariables recordedVariables = egGenerator.getGeneratedExampleVars(appJavaClassPath,
					generatedCode, type);
			List<VarValue> variables = recordedVariables.getOuterReadVariables();
			variables.addAll(recordedVariables.getOuterWrittenVariables());
			VarValue mostSuitableVar = null;
			for (VarValue var : variables) {
				if (datapoint.get(varTypeKey).equals(var.getType())) {
					if (mostSuitableVar == null) {
						mostSuitableVar = var;
					} else if (datapoint.get(varNameKey).equals(var.getVarName())) {
						mostSuitableVar = var;
						break;
					}
				}
			}
			if (mostSuitableVar == null) {
				return "";
			}
			
			HashMap<String, String> newDP = new HashMap<>();
			newDP.put(lineSourceCodeKey, loc); // TODO: extract code
			newDP.put(varTypeKey, mostSuitableVar.getType());
			newDP.put(varNameKey, mostSuitableVar.getVarName());
			newDP.put(varValueKey, mostSuitableVar.getStringValue());
			newDP.put(classStructureKey, datapoint.get(classStructureKey));
			String gt = mostSuitableVar.toJSON().toString();
			
			String generatedExample = promptTemplateFiller.getExample(newDP, gt);
			// TODO: add example to database
			return generatedExample;
		} else {
			return closestExample;
		}
	}

	@Override
	public void recordLoss() {
		LossDataCollector lossDataCollector = new LossDataCollector();

		for (HashMap<String, String> datapoint : testingDataset) {
			System.out.println("Baseline:\n");
			double baselineLoss = getLoss(datapoint, x -> promptTemplateFiller.getDefaultPrompt(x));

			System.out.println("Experiment:\n");
			double experimentLoss = getLoss(datapoint,
					x -> promptTemplateFiller.getPrompt(x, (String) this.searchForExample(x)[0]));

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
