package microbat.tracerecov.autoprompt;

import java.util.ArrayList;
import java.util.HashMap;

import microbat.tracerecov.varskeleton.VarSkeletonParser;
import microbat.tracerecov.varskeleton.VariableSkeleton;

/**
 * This class is used to search for the most relevant example from the database.
 */
public class ExampleSearcher {

	private ArrayList<HashMap<String, String>> dataset;
	private VarSkeletonParser varSkeletonParser;
	private PromptTemplateFiller promptTemplateFiller;

	public ExampleSearcher() {
		DatasetReader datasetReader = new DatasetReader();
		dataset = datasetReader.readCompleteDataset();

		varSkeletonParser = new VarSkeletonParser();

		promptTemplateFiller = new PromptTemplateFiller();
	}

	public String searchForExample(HashMap<String, String> datapoint) {
		String classStructureKey = "class_structure";
		String groundTruthKey = "ground_truth";

		String classStructure = datapoint.get(classStructureKey);
		VariableSkeleton varSkeleton = varSkeletonParser.parseClassStructure(classStructure);

		double minDiffScore = 1;
		int datapointIndex = 0;
		for (int i = 0; i < dataset.size(); i++) {
			HashMap<String, String> example = dataset.get(i);
			String exampleClassStructure = example.get(classStructureKey);
			VariableSkeleton exampleVarSkeleton = varSkeletonParser.parseClassStructure(exampleClassStructure);

			double diffScore = varSkeleton.getDifferenceScore(exampleVarSkeleton);
			if (diffScore < minDiffScore) {
				minDiffScore = diffScore;
				datapointIndex = i;
			}
		}

		HashMap<String, String> closestExample = dataset.get(datapointIndex);
		String groundTruth = closestExample.get(groundTruthKey);
		return promptTemplateFiller.getExample(closestExample, groundTruth);
	}
}
