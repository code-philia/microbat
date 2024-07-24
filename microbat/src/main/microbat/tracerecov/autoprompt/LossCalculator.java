package microbat.tracerecov.autoprompt;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class is used to compute the loss function for automatic prompt
 * engineering.
 */
public class LossCalculator {

	String emptyKey = ":";

	public LossCalculator() {
	}

	/**
	 * Compute loss from root of the variable.
	 */
	public double computeLoss(JSONObject var1, JSONObject var2) {
		// Assume there is only one key in the root.
		String key1 = null;
		for (String entry : var1.keySet()) {
			key1 = entry;
			break;
		}
		String key2 = null;
		for (String entry : var2.keySet()) {
			key2 = entry;
			break;
		}

		if (key1 == null || key2 == null) {
			return 1;
		}

		Object value1 = var1.get(key1);
		Object value2 = var2.get(key2);
		if (value1 instanceof JSONObject && value2 instanceof JSONObject) {
			return computeLossForCompositeTypes(key1, key2, (JSONObject) value1, (JSONObject) value2);
		} else if (value1 instanceof JSONArray && value2 instanceof JSONArray) {
			return computeLossForArrays(key1, key2, (JSONArray) value1, (JSONArray) value2);
		} else {
			return computeLossForOtherTypes(key1, key2, value1, value2);
		}
	}

	private double computeLossForCompositeTypes(String key1, String key2, JSONObject value1, JSONObject value2) {
		double individualScoreSum = 0;
		int componentCount = 0;

		// Loss based on type
		if (!emptyKey.equals(key1)) {
			componentCount += 1;
			individualScoreSum += computeLossBetweenTypes(key1, key2);
		}

		// Loss based on common fields
		Set<String> keySet1 = value1.keySet();
		Set<String> keySet2 = value2.keySet();
		for (String fieldSignature1 : keySet1) {
			String fieldSignature2 = getComparableKey(fieldSignature1, keySet2);
			if (fieldSignature2 != null) {
				// found common field
				componentCount += 1;

				// compute individual score for field
				Object fieldValue1 = value1.get(fieldSignature1);
				Object fieldValue2 = value2.get(fieldSignature2);
				if (fieldValue1 instanceof JSONObject && fieldValue2 instanceof JSONObject) {
					individualScoreSum += computeLossForCompositeTypes(fieldSignature1, fieldSignature2,
							(JSONObject) fieldValue1, (JSONObject) fieldValue2);
				} else if (fieldValue1 instanceof JSONArray && fieldValue2 instanceof JSONArray) {
					individualScoreSum += computeLossForArrays(fieldSignature1, fieldSignature2,
							(JSONArray) fieldValue1, (JSONArray) fieldValue2);
				} else {
					individualScoreSum += computeLossForOtherTypes(fieldSignature1, fieldSignature2, fieldValue1,
							fieldValue2);
				}
			}
		}

		// Loss based on different fields
		int keySetDifferences = countKeySetDifferences(keySet1, keySet2);
		componentCount += keySetDifferences;
		individualScoreSum += keySetDifferences;

		return individualScoreSum / (double) componentCount;
	}

	private double computeLossForArrays(String key1, String key2, JSONArray value1, JSONArray value2) {
		double individualScoreSum = 0;
		int componentCount = 0;

		// Loss based on type
		if (!emptyKey.equals(key1)) {
			componentCount += 1;
			individualScoreSum += computeLossBetweenTypes(key1, key2);
		}

		// Loss based on elements with overlapping indices
		int len1 = value1.length();
		int len2 = value2.length();
		int overlappingLength = Math.min(len1, len2);
		for (int i = 0; i < overlappingLength; i++) {
			componentCount += 1;

			// compute individual score for field
			Object element1 = value1.get(i);
			Object element2 = value2.get(i);
			if (element1 instanceof JSONObject && element2 instanceof JSONObject) {
				individualScoreSum += computeLossForCompositeTypes(emptyKey, emptyKey, (JSONObject) element1,
						(JSONObject) element2);
			} else if (element1 instanceof JSONArray && element2 instanceof JSONArray) {
				individualScoreSum += computeLossForArrays(emptyKey, emptyKey, (JSONArray) element1,
						(JSONArray) element2);
			} else {
				individualScoreSum += computeLossForOtherTypes(emptyKey, emptyKey, element1, element2);
			}
		}

		// Loss based on extra elements
		int absLengthDifference = Math.abs(len1 - len2);
		componentCount += absLengthDifference;
		individualScoreSum += absLengthDifference;

		return individualScoreSum / (double) componentCount;
	}

	private double computeLossForOtherTypes(String key1, String key2, Object value1, Object value2) {
		double individualScoreSum = 0;
		int componentCount = 0;

		// Loss based on type
		if (!emptyKey.equals(key1)) {
			componentCount += 1;
			individualScoreSum += computeLossBetweenTypes(key1, key2);
		}

		// Loss based on value
		componentCount += 1;
		individualScoreSum += computeLossBetweenValues(value1.toString(), value2.toString());

		return individualScoreSum / (double) componentCount;
	}

	private String getComparableKey(String key, Set<String> keySet) {
		for (String potentialMatch : keySet) {
			if (isValidKey(potentialMatch) && areComparableKeys(key, potentialMatch)) {
				return potentialMatch;
			}
		}
		return null;
	}

	private boolean isValidKey(String key) {
		String[] items = key.split(":");
		return items.length == 2;
	}

	/**
	 * Assume both keys are valid. Keys are comparable if they have the same field
	 * name (not necessarily sharing the same field type).
	 */
	private boolean areComparableKeys(String key1, String key2) {
		String field1 = key1.split(":")[0].trim();
		String field2 = key2.split(":")[0].trim();
		return field1.equals(field2);
	}

	/**
	 * Assume both keys are valid and comparable. Loss between two keys == 0 if they
	 * have the same type, Loss == 1 otherwise.
	 */
	private int computeLossBetweenTypes(String key1, String key2) {
		String type1 = key1.split(":")[1].trim();
		String type2 = key2.split(":")[1].trim();
		return identityFunction(type1, type2);
	}

	private int computeLossBetweenValues(String value1, String value2) {
		return identityFunction(value1, value2);
	}

	/**
	 * I{x!=y}
	 */
	private int identityFunction(String x, String y) {
		if (x.equals(y)) {
			return 0;
		} else {
			return 1;
		}
	}

	private int countKeySetDifferences(Set<String> set1, Set<String> set2) {
		int differences = 0;
		for (String key1 : set1) {
			String key2 = getComparableKey(key1, set2);
			if (key2 == null) {
				differences++;
			}
		}
		for (String key2 : set2) {
			String key1 = getComparableKey(key2, set1);
			if (key1 == null) {
				differences++;
			}
		}
		return differences;
	}
}
