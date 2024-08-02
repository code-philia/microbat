package microbat.tracerecov.autoprompt;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class AliasInferenceLossCalculator extends LossCalculator {

	/**
	 * 1 - intersection/union
	 * 
	 * key and value of JSON have the same weight
	 */
	@Override
	public double computeLoss(JSONObject actualJSON, JSONObject expectedJSON) {
		double diffScore = 0;
		double totalCount = 0;

		Map<String, Integer> itemCount = new HashMap<>();
		for (String key : expectedJSON.keySet()) {
			if (!itemCount.containsKey(key)) {
				itemCount.put(key, 0);
			}
			itemCount.put(key, itemCount.get(key) + 1);
			diffScore += 2; // to be reduced later
			totalCount += 2;
		}

		for (String key : actualJSON.keySet()) {
			if (!itemCount.containsKey(key) || itemCount.get(key) <= 0) {
				diffScore += 2;
				totalCount += 2;
			} else {
				itemCount.put(key, itemCount.get(key) - 1);
				diffScore -= 1; // reduce 1 for correct key
				if (actualJSON.get(key).equals(expectedJSON.get(key))) {
					diffScore -= 1; // reduce 1 for correct value
				}
			}
		}
		return diffScore / totalCount;
	}
}
