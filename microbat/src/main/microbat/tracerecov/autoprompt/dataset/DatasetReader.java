package microbat.tracerecov.autoprompt.dataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * This class reads in dataset that is used in automatic prompt engineering.
 */
public abstract class DatasetReader {
	
	private long seed = 2024L;

	protected abstract ArrayList<HashMap<String, String>> readCompleteDataset();

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
