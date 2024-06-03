package microbat.tracerecov;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author hongshuwang
 */
public class VariableOfInterest {
	private static JSONObject variableOfInterest;

	public static void setVariableOfInterest(JSONObject variableOfInterest) {
		VariableOfInterest.variableOfInterest = variableOfInterest;
	}

	public static JSONObject getVariableOfInterestForAliasInferencing(String aliasID) {
		return getJSONObjectForAliasInfer(aliasID, variableOfInterest);
	}

	private static JSONObject getJSONObjectForAliasInfer(String aliasID, JSONObject originalJson) {
		JSONObject variable = new JSONObject();

		String id = " &" + aliasID + "&";
		Iterator<String> keys = originalJson.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			String newKey = key + id;

			Object value = originalJson.get(key);
			if (value instanceof JSONObject) {
				variable.put(newKey, getJSONObjectForAliasInfer("?", (JSONObject) value));
			} else if (value instanceof JSONArray) {
				variable.put(newKey, getJSONArrayForAliasInfer((JSONArray) value));
			} else {
				// base case
				variable.put(newKey, value);
			}
		}

		return variable;
	}

	private static JSONArray getJSONArrayForAliasInfer(JSONArray originalJson) {
		JSONArray variable = new JSONArray();

		originalJson.forEach(v -> {
			if (v instanceof JSONObject) {
				variable.put(getJSONObjectForAliasInfer("?", (JSONObject) v));
			} else if (v instanceof JSONArray) {
				variable.put(getJSONArrayForAliasInfer((JSONArray) v));
			} else {
				// base case
				variable.put(v);
			}
		});

		return variable;
	}
}
