package microbat.tracerecov.autoprompt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import org.json.JSONException;
import org.json.JSONObject;

import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;
import microbat.tracerecov.autoprompt.dataset.LossDataCollector;
import microbat.tracerecov.autoprompt.dataset.VarExpansionDatasetReader;
import microbat.tracerecov.executionsimulator.ExecutionSimulator;
import microbat.tracerecov.varskeleton.VarSkeletonParser;
import microbat.tracerecov.varskeleton.VariableSkeleton;

/**
 * This class is used to search for the most relevant example from the database.
 */
public abstract class ExampleSearcher {

	public abstract String searchForExample(HashMap<String, String> datapoint);

	public abstract void recordLoss();

	protected abstract double getLoss(HashMap<String, String> datapoint,
			Function<HashMap<String, String>, String> operation);

	/**
	 * Copied from {@code AutoPromptEngineer}
	 */
	protected String getLLMOutput(String request) {
		String output;
		try {
			ExecutionSimulator executionSimulator = new ExecutionSimulator();
			output = executionSimulator.sendRequest("", request,"json_object");
			int begin = output.indexOf("{");
			int end = output.lastIndexOf("}");
			return output.substring(begin, end + 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
