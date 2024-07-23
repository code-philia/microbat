package microbat.tracerecov.autoprompt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import microbat.Activator;
import microbat.preference.TraceRecovPreference;

public class DatasetReader {
	private String variableExpansionPath;

	public DatasetReader() {
		// TODO: move this to preference page
		String variableExpansionFile = "var_expansion_ground_truth.csv";
//		this.variableExpansionPath = Activator.getDefault().getPreferenceStore()
//				.getString(TraceRecovPreference.PROMPT_GT_PATH) + File.separator + variableExpansionFile;
		this.variableExpansionPath = "/Users/hongshuwang/Desktop/var_expansion_ground_truth.csv";
	}

	/**
	 * Reads variable expansion dataset.
	 * 
	 * var_name, var_type, var_value, class_structure, source_code, ground_truth
	 */
	public ArrayList<HashMap<String, String>> readVariableExpansionDataset() {
		ArrayList<HashMap<String, String>> dataset = new ArrayList<>();

		try {
			BufferedReader bufferReader = new BufferedReader(new FileReader(variableExpansionPath));

			// read headers
			String[] headers;
			String line = bufferReader.readLine();
			if (line != null) {
				headers = parseLine(line);
			}

			// read content
			while ((line = bufferReader.readLine()) != null) {
				String[] columns = parseLine(line);

				HashMap<String, String> datapoint = new HashMap();
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

	private static String[] parseLine(String line) {
		// Split the line by commas, but handle quoted values
		String[] columns = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		for (int i = 0; i < columns.length; i++) {
			columns[i] = columns[i].trim().replaceAll("^\"|\"$", "").replaceAll("\"\"", "\"");
		}
		return columns;
	}

	public static void main(String[] args) {
		DatasetReader datasetReader = new DatasetReader();
		ArrayList<HashMap<String, String>> dataset = datasetReader.readVariableExpansionDataset();
		System.out.println();
	}
}
