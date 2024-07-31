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

	/* common keys */
	public static final String STEP_NO = "step_no";
	public static final String VAR_NAME = "var_name";
	public static final String VAR_TYPE = "var_type";
	public static final String VAR_VALUE = "var_value";
	public static final String CLASS_STRUCTURE = "class_structure";
	public static final String SOURCE_CODE = "source_code";
	public static final String GROUND_TRUTH = "ground_truth";

	/* alias inference keys */
	public static final String VARS_IN_STEP = "vars_in_step";
	public static final String TARGET_VAR = "target_var";
	public static final String CURRENT_ALIASES = "current_aliases";
	public static final String FIELDS_OF_VARS_IN_STEP = "fields_in_step";
	public static final String INVOKED_METHODS = "invoked_methods";
	public static final String FIELDS_OF_TARGET_VAR = "target_fields";
	public static final String SOURCE_FILE = "source_file";

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
