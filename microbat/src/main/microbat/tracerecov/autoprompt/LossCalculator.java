package microbat.tracerecov.autoprompt;

import org.json.JSONObject;

/**
 * This class is used to compute the loss function for automatic prompt
 * engineering.
 */
public abstract class LossCalculator {

	public abstract double computeLoss(JSONObject actualJSON, JSONObject expectedJSON);

}
