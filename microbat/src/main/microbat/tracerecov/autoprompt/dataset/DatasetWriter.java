package microbat.tracerecov.autoprompt.dataset;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class writes to dataset that is used in automatic prompt engineering.
 */
public abstract class DatasetWriter {

	protected String path;
	protected ArrayList<HashMap<String, String>> dataset;

	public abstract void addToDataset(HashMap<String, String> datapoint);

	public abstract String getLine(HashMap<String, String> datapoint);

	public abstract void loadDataset();

}
