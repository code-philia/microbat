package microbat.tracerecov.autoprompt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import microbat.Activator;
import microbat.preference.TraceRecovPreference;

/**
 * This class reads in dataset that is used in automatic prompt engineering.
 */
public class DatasetReader {
	private String variableExpansionPath;

	public DatasetReader() {
		// TODO: move this to preference page
		String variableExpansionFile = "var_expansion.txt";
		this.variableExpansionPath = Activator.getDefault().getPreferenceStore()
				.getString(TraceRecovPreference.PROMPT_GT_PATH) + File.separator + variableExpansionFile;
	}

	/**
	 * Reads variable expansion dataset.
	 * 
	 * var_name, var_type, var_value, class_structure, source_code, ground_truth
	 */
	public ArrayList<HashMap<String, String>> readCompleteDataset() {
		ArrayList<HashMap<String, String>> dataset = new ArrayList<>();

		try {
			BufferedReader bufferReader = new BufferedReader(new FileReader(variableExpansionPath));

			String line;
			// read content
			while ((line = bufferReader.readLine()) != null) {
				String[] columns = line.split("###");
				if (columns.length != 6) {
					continue;
				}

				HashMap<String, String> datapoint = new HashMap<>();
				datapoint.put("var_name", columns[0]);
				datapoint.put("var_type", columns[1]);
				datapoint.put("var_value", columns[2]);
				datapoint.put("class_structure", columns[3]);
				datapoint.put("source_code", columns[4]);
				datapoint.put("ground_truth", columns[5]);

				dataset.add(datapoint);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dataset;
	}

	/**
	 * Split the complete dataset randomly into training and testing sets.
	 * 
	 * @return [trainingSet, testingSet]
	 */
	public ArrayList<ArrayList<HashMap<String, String>>> getTrainingAndTestingDataset() {
		ArrayList<HashMap<String, String>> completeDataset = readCompleteDataset();
		int size = completeDataset.size();
		Set<Integer> testingSetIndices = selectTestingSetIndices(size);

		ArrayList<HashMap<String, String>> trainingSet = new ArrayList<>();
		ArrayList<HashMap<String, String>> testingSet = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			if (testingSetIndices.contains(i)) {
				testingSet.add(completeDataset.get(i));
			} else {
				trainingSet.add(completeDataset.get(i));
			}
		}

		ArrayList<ArrayList<HashMap<String, String>>> splitSets = new ArrayList<ArrayList<HashMap<String, String>>>();
		splitSets.add(trainingSet);
		splitSets.add(testingSet);

		return splitSets;
	}

	private Set<Integer> selectTestingSetIndices(int totalSize) {
		long seed = 2024L;
		Random random = new Random(seed);
		Set<Integer> randomNumbers = new HashSet<>();

		int testingSetSize = totalSize * 3 / 10;

		while (randomNumbers.size() <= testingSetSize) {
			int number = random.nextInt(totalSize);
			randomNumbers.add(number);
		}

		return randomNumbers;
	}
}
