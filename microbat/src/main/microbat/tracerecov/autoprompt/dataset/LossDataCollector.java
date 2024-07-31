package microbat.tracerecov.autoprompt.dataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import microbat.Activator;
import microbat.preference.TraceRecovPreference;

/**
 * This class is used to save loss data to CSV files.
 */
public class LossDataCollector {
	private String resultPath;

	public LossDataCollector() {
		// TODO: move this to preference page
		String resultFile = "var_expansion_result.txt";
		this.resultPath = Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.PROMPT_GT_PATH)
				+ File.separator + resultFile;
	}

	public void saveDataToFile(double baselineLoss, double experimentLoss) {
		try {
			FileWriter fileWriter = new FileWriter(resultPath, true);
			PrintWriter printWriter = new PrintWriter(fileWriter);
			// write content
			printWriter.println(String.valueOf(baselineLoss) + "," + String.valueOf(experimentLoss));

			printWriter.close();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
