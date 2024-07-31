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
				.getString(TraceRecovPreference.PROMPT_GT_PATH) + File.separator + variableExpansionFile;
	}
	
	/**
	 * Reads variable expansion dataset.
	 * 
	 * var_name, var_type, var_value, class_structure, source_code, ground_truth
	 */
	@Override
	protected ArrayList<HashMap<String, String>> readCompleteDataset() {
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
}
