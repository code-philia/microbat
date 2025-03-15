package microbat.tracerecov.autoprompt.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import microbat.Activator;
import microbat.preference.TraceRecovPreference;

public class VarExpansionDatasetWriter extends DatasetWriter {

	public VarExpansionDatasetWriter() {
		// TODO: move this to preference page
		String variableExpansionFile = "var_expansion.txt";

		this.path = Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.INCONTEXT_FILE_PATH)
				+ File.separator + variableExpansionFile;

		this.loadDataset();
	}

	@Override
	public void addToDataset(HashMap<String, String> datapoint) {
		dataset.add(datapoint);

		try (FileWriter fw = new FileWriter(path, true); PrintWriter pw = new PrintWriter(fw)) {
			pw.println(getLine(datapoint));
			fw.close();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getLine(HashMap<String, String> datapoint) {
		String varName = datapoint.get(DatasetReader.VAR_NAME);
		String varType = datapoint.get(DatasetReader.VAR_TYPE);
		String varValue = datapoint.get(DatasetReader.VAR_VALUE);
		String classStructure = datapoint.get(DatasetReader.CLASS_STRUCTURE);
		String lineSourceCode = datapoint.get(DatasetReader.LINE_SOURCE_CODE);
		String groundTruth = datapoint.get(DatasetReader.GROUND_TRUTH);

		String delimiter = "###";
		StringBuilder content = new StringBuilder();
		content.append(varName + delimiter);
		content.append(varType + delimiter);
		content.append(varValue + delimiter);
		content.append(classStructure + delimiter);
		content.append(lineSourceCode + delimiter);
		content.append(groundTruth);

		return content.toString();
	}

	@Override
	public void loadDataset() {
		if (dataset != null) {
			return;
		}

		ArrayList<HashMap<String, String>> dataset = new ArrayList<>();

		try {
			BufferedReader bufferReader = new BufferedReader(new FileReader(path));

			String line;
			// read content
			while ((line = bufferReader.readLine()) != null) {
				String[] columns = line.split("###");
				if (columns.length != 6) {
					continue;
				}

				HashMap<String, String> datapoint = new HashMap<>();
				datapoint.put(DatasetReader.VAR_NAME, columns[0]);
				datapoint.put(DatasetReader.VAR_TYPE, columns[1]);
				datapoint.put(DatasetReader.VAR_VALUE, columns[2]);
				datapoint.put(DatasetReader.CLASS_STRUCTURE, columns[3]);
				datapoint.put(DatasetReader.LINE_SOURCE_CODE, columns[4]);
				datapoint.put(DatasetReader.GROUND_TRUTH, columns[5]);

				dataset.add(datapoint);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.dataset = dataset;
	}

}
