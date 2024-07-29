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

	public ExampleSearcher() {
		DatasetReader datasetReader = new DatasetReader();
		dataset = datasetReader.readCompleteDataset();

		varSkeletonParser = new VarSkeletonParser();
	}

	public String searchForExample(HashMap<String, String> datapoint) {
		String classStructureKey = "class_structure";
		String classStructure = datapoint.get(classStructureKey);
		VariableSkeleton varSkeleton = varSkeletonParser.parseClassStructure(classStructure);

		for (HashMap<String, String> example : dataset) {
			String exampleClassStructure = example.get(classStructureKey);
			if (!classStructure.equals(exampleClassStructure)) {
				VariableSkeleton exampleVarSkeleton = varSkeletonParser.parseClassStructure(exampleClassStructure);
				System.out.println();
			}
		}

		return "";
	}

	public static void main(String[] args) {
		ExampleSearcher exampleSearcher = new ExampleSearcher();

		DatasetReader datasetReader = new DatasetReader();
		ArrayList<HashMap<String, String>> dataset = datasetReader.readCompleteDataset();

		exampleSearcher.searchForExample(dataset.get(0));
	}
}
