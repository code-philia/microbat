package microbat.tracerecov.autoprompt.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import microbat.Activator;
import microbat.preference.TraceRecovPreference;

public class DefinitionInferenceDatasetReader extends DatasetReader {
	
	private String definitionInferencePath;

	public DefinitionInferenceDatasetReader() {
		// TODO: move this to preference page
		String definitionInferenceFile = "definition_inference.txt";

		this.definitionInferencePath = Activator.getDefault().getPreferenceStore()
				.getString(TraceRecovPreference.DEF_FILE_PATH) + File.separator + definitionInferenceFile;
	}

	/**
	 * Reads definition inference dataset.
	 * 
	 * target_field, var_name, target_var, source_code, invoked_methods, vars_in_step, ground_truth
	 */
	@Override
	protected ArrayList<HashMap<String, String>> readCompleteDataset() {
		ArrayList<HashMap<String, String>> dataset = new ArrayList<>();

		try {
			BufferedReader bufferReader = new BufferedReader(new FileReader(definitionInferencePath));

			String line;
			// read content
			while ((line = bufferReader.readLine()) != null) {
				String[] columns = line.split("###");
				if (columns.length != 7) {
					continue;
				}

				HashMap<String, String> datapoint = new HashMap<>();
				datapoint.put(TARGET_FIELD, columns[0]);
				datapoint.put(VAR_NAME, columns[1]);
				datapoint.put(TARGET_VAR, columns[2]);
				datapoint.put(SOURCE_CODE, columns[3]);
				datapoint.put(INVOKED_METHODS, columns[4]);
				datapoint.put(VARS_IN_STEP, columns[5]);
				datapoint.put(GROUND_TRUTH, columns[6]);

				dataset.add(datapoint);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dataset;
	}

}
