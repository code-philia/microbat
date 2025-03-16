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
import microbat.tracerecov.autoprompt.dataset.VarExpansionDatasetWriter;
import microbat.tracerecov.autoprompt.incontextlearning.CompilationFailureException;
import microbat.tracerecov.autoprompt.incontextlearning.InContextEgGenerator;
import microbat.tracerecov.autoprompt.incontextlearning.InContextEgGenerator.InContextLearningCode;
import microbat.tracerecov.autoprompt.incontextlearning.InContextEgGenerator.InContextLearningVariables;
import microbat.tracerecov.autoprompt.incontextlearning.InContextLearning.InContextLearningType;
import microbat.tracerecov.executionsimulator.ExecutionSimulatorFactory;
import microbat.tracerecov.executionsimulator.LLMResponseType;
import microbat.tracerecov.varskeleton.VarSkeletonBuilder;
import microbat.tracerecov.varskeleton.VarSkeletonParser;
import microbat.tracerecov.varskeleton.VariableSkeleton;
import sav.strategies.dto.AppJavaClassPath;

public class VarExpansionExampleSearcher extends ExampleSearcher {

	private static double[] WEIGHTS = new double[] { 1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0 };
	private static double SIM_SCORE_THRESHOLD = 0.75;

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
			// datapoint keys
			String sourceCodeKey = DatasetReader.METHOD_SOURCE_CODE;
			String lineSourceCodeKey = DatasetReader.LINE_SOURCE_CODE;
			String importsKey = DatasetReader.IMPORTS;
			String lineNoKey = DatasetReader.LINE_NO;
			String varTypeKey = DatasetReader.VAR_TYPE;
			String varNameKey = DatasetReader.VAR_NAME;
			String varValueKey = DatasetReader.VAR_VALUE;
			String classStructureKey = DatasetReader.CLASS_STRUCTURE;
			String groundTruthKey = DatasetReader.GROUND_TRUTH;

			// generate in-context learning examples
			InContextLearningVariables recordedVariables = null;
			String loc = null;
			for (int i = 0; i < 2; i++) {
				InContextEgGenerator egGenerator = new InContextEgGenerator();
				egGenerator.setExecutionSimulator(ExecutionSimulatorFactory.getExecutionSimulator());
				InContextLearningType type = InContextLearningType.VAR_EXPANSION;
				InContextLearningCode generatedCode = egGenerator.getGeneratedExampleCode(datapoint.get(importsKey),
						datapoint.get(sourceCodeKey), Integer.valueOf(datapoint.get(lineNoKey)),
						datapoint.get(varNameKey), datapoint.get(varValueKey), type,
						InContextEgGenerator.defaultToString(), 2);
				if (generatedCode == null) {
					continue;
				}
				loc = TraceRecovUtils.getLoc(generatedCode.getCode(), generatedCode.getMarkerLine());

				try {
					recordedVariables = egGenerator.getGeneratedExampleVars(appJavaClassPath, generatedCode, type);
				} catch (CompilationFailureException | IllegalStateException e) {
					e.printStackTrace();
				}
				if (recordedVariables != null) {
					break;
				}
			}

			if (recordedVariables == null) {
				return "";
			}

			List<VarValue> variables = recordedVariables.getOuterReadVariables();
			variables.addAll(recordedVariables.getOuterWrittenVariables());
			VarValue mostSuitableVar = null;
			double classSimScore = 0;
			VariableSkeleton varSkeleton = varSkeletonParser
					.parseClassStructure(datapoint.get(DatasetReader.CLASS_STRUCTURE));
			VariableSkeleton mostSuitableVarSkeleton = null;
			for (VarValue var : variables) {
				VariableSkeleton otherVarSkeleton = VarSkeletonBuilder.getVariableStructure(var.getType(),
						appJavaClassPath);
				double newScore = simScoreCalculator.getJaccardCoefficient(varSkeleton, otherVarSkeleton);
				if (newScore > classSimScore) {
					mostSuitableVar = var;
					classSimScore = newScore;
					mostSuitableVarSkeleton = otherVarSkeleton;
				}
			}
			if (mostSuitableVar == null) {
				return "";
			}

			// create new datapoint
			HashMap<String, String> newDP = new HashMap<>();
			newDP.put(lineSourceCodeKey, loc);
			newDP.put(varTypeKey, mostSuitableVar.getType());
			newDP.put(varNameKey, mostSuitableVar.getVarName());
			newDP.put(varValueKey, mostSuitableVar.getStringValue());
			newDP.put(classStructureKey, mostSuitableVarSkeleton.toString());
			String gt = mostSuitableVar.toJSON().toString();
			newDP.put(groundTruthKey, gt);

			String generatedExample = promptTemplateFiller.getExample(newDP, gt);

			double codeSimScore = simScoreCalculator.getSimilarityRatioBasedOnLCS(datapoint.get(lineSourceCodeKey),
					newDP.get(lineSourceCodeKey));
			double valueSimScore = simScoreCalculator.getSimilarityRatioBasedOnLCS(datapoint.get(varValueKey),
					newDP.get(varValueKey));

			double simScore = simScoreCalculator
					.getCombinedScore(new double[] { codeSimScore, valueSimScore, classSimScore }, WEIGHTS);

			String varType = mostSuitableVar.getType();
			if (simScore > maxSimScore && !varType.contains("StringBuilder") && !varType.contains("StringWriter")
					&& !varType.contains("StringBuffer")) {
				VarExpansionDatasetWriter datasetWriter = new VarExpansionDatasetWriter();
				datasetWriter.addToDataset(newDP);
				return generatedExample;
			} else {
				return closestExample;
			}
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
