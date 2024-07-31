package microbat.tracerecov.autoprompt.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import microbat.Activator;
import microbat.preference.TraceRecovPreference;

public class AliasInferenceDatasetReader extends DatasetReader {

	private String aliasInferencePath;

	public AliasInferenceDatasetReader() {
		// TODO: move this to preference page
		String aliasInferenceFile = "alias_inference.txt";

		this.aliasInferencePath = Activator.getDefault().getPreferenceStore()
				.getString(TraceRecovPreference.ALIAS_FILE_PATH) + File.separator + aliasInferenceFile;
	}

	/**
	 * Reads alias inference dataset.
	 * 
	 * step_no, source_code, vars_in_step, target_var, current_aliases,
	 * fields_in_step, invoked_methods, target_fields, ground_truth, source_file
	 */
	@Override
	protected ArrayList<HashMap<String, String>> readCompleteDataset() {
		ArrayList<HashMap<String, String>> dataset = new ArrayList<>();

		try {
			BufferedReader bufferReader = new BufferedReader(new FileReader(aliasInferencePath));

			String line;
			// read content
			while ((line = bufferReader.readLine()) != null) {
				String[] columns = line.split("###");
				if (columns.length != 6) {
					continue;
				}

				HashMap<String, String> datapoint = new HashMap<>();
				datapoint.put(STEP_NO, columns[0]);
				datapoint.put(SOURCE_CODE, columns[1]);
				datapoint.put(VARS_IN_STEP, columns[2]);
				datapoint.put(TARGET_VAR, columns[3]);
				datapoint.put(CURRENT_ALIASES, columns[4]);
				datapoint.put(FIELDS_OF_VARS_IN_STEP, columns[5]);
				datapoint.put(INVOKED_METHODS, columns[6]);
				datapoint.put(FIELDS_OF_TARGET_VAR, columns[7]);
				datapoint.put(GROUND_TRUTH, columns[8]);
				datapoint.put(SOURCE_FILE, columns[9]);

				dataset.add(datapoint);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dataset;
	}
}
