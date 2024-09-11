package microbat.tracerecov.autoprompt.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import microbat.Activator;
import microbat.preference.TraceRecovPreference;

public class VarExpansionDatasetReader extends DatasetReader {

	private String variableExpansionPath;

	public VarExpansionDatasetReader() {
		// TODO: move this to preference page
		String variableExpansionFile = "var_expansion.txt";

		this.variableExpansionPath = Activator.getDefault().getPreferenceStore()
				.getString(TraceRecovPreference.VAR_EXPAND_FILE_PATH) + File.separator + variableExpansionFile;
	}

	/**
	 * Reads variable expansion dataset.
	 * 
	 * var_name, var_type, var_value, class_structure, source_code, ground_truth
	 */
	@Override
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
				datapoint.put(VAR_NAME, columns[0]);
				datapoint.put(VAR_TYPE, columns[1]);
				datapoint.put(VAR_VALUE, columns[2]);
				datapoint.put(CLASS_STRUCTURE, columns[3]);
				datapoint.put(SOURCE_CODE, columns[4]);
				datapoint.put(GROUND_TRUTH, columns[5]);

				dataset.add(datapoint);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dataset;
	}
}
