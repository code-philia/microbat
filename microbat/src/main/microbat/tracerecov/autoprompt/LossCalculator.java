package microbat.tracerecov.autoprompt;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class is used to compute the loss function for automatic prompt
 * engineering.
 */
public class LossCalculator {

	private String emptyKey = ":";

	public LossCalculator() {
	}

	/**
	 * Compute loss from root of the variable.
	 */
	public double computeLoss(JSONObject actualJSON, JSONObject expectedJSON) {
		// Assume there is only one key in the root.
		String actualKey = null;
		for (String entry : actualJSON.keySet()) {
			actualKey = entry;
			break;
		}
		String expectedKey = null;
		for (String entry : expectedJSON.keySet()) {
			expectedKey = entry;
			break;
		}

		if (actualKey == null || expectedKey == null) {
			return 1;
		}

		Object actualValue = actualJSON.get(actualKey);
		Object expectedValue = expectedJSON.get(expectedKey);
		if (actualValue instanceof JSONObject && expectedValue instanceof JSONObject) {
			return computeLossForCompositeTypes(actualKey, expectedKey, (JSONObject) actualValue,
					(JSONObject) expectedValue);
		} else if (actualValue instanceof JSONArray && expectedValue instanceof JSONArray) {
			return computeLossForArrays(actualKey, expectedKey, (JSONArray) actualValue, (JSONArray) expectedValue);
		} else {
			return computeLossForOtherTypes(actualKey, expectedKey, actualValue, expectedValue);
		}
	}

	private double computeLossForCompositeTypes(String actualJSONKey, String expectedJSONKey,
			JSONObject actualJSONValue, JSONObject expectedJSONValue) {
		double individualScoreSum = 0;
		int componentCount = 0;

		// Loss based on type
		if (!emptyKey.equals(actualJSONKey)) {
			componentCount += 1;
			individualScoreSum += computeLossBetweenTypes(actualJSONKey, expectedJSONKey);
		}

		// Loss based on common fields
		Set<String> actualKeySet = actualJSONValue.keySet();
		Set<String> expectedKeySet = expectedJSONValue.keySet();
		for (String actualFieldSignature : actualKeySet) {
			String expectedFieldSignature = getComparableKey(actualFieldSignature, expectedKeySet);
			if (expectedFieldSignature != null) {
				// found common field
				componentCount += 1;

				// compute individual score for field
				Object actualFieldValue = actualJSONValue.get(actualFieldSignature);
				Object expectedFieldValue = expectedJSONValue.get(expectedFieldSignature);
				if (actualFieldValue instanceof JSONObject && expectedFieldValue instanceof JSONObject) {
					individualScoreSum += computeLossForCompositeTypes(actualFieldSignature, expectedFieldSignature,
							(JSONObject) actualFieldValue, (JSONObject) expectedFieldValue);
				} else if (actualFieldValue instanceof JSONArray && expectedFieldValue instanceof JSONArray) {
					individualScoreSum += computeLossForArrays(actualFieldSignature, expectedFieldSignature,
							(JSONArray) actualFieldValue, (JSONArray) expectedFieldValue);
				} else {
					individualScoreSum += computeLossForOtherTypes(actualFieldSignature, expectedFieldSignature,
							actualFieldValue, expectedFieldValue);
				}
			}
		}

		// Loss based on different fields
		int keySetDifferences = countKeySetDifferences(actualKeySet, expectedKeySet);
		componentCount += keySetDifferences;
		individualScoreSum += keySetDifferences;

		return individualScoreSum / (double) componentCount;
	}

	private double computeLossForArrays(String actualJSONKey, String expectedJSONKey, JSONArray actualJSONArray,
			JSONArray expectedJSONArray) {
		double individualScoreSum = 0;
		int componentCount = 0;

		// Loss based on type
		if (!emptyKey.equals(actualJSONKey)) {
			componentCount += 1;
			individualScoreSum += computeLossBetweenTypes(actualJSONKey, expectedJSONKey);
		}

		// Loss based on elements with overlapping indices
		int actualLen = actualJSONArray.length();
		int expectedLen = expectedJSONArray.length();
		int overlappingLength = Math.min(actualLen, expectedLen);
		for (int i = 0; i < overlappingLength; i++) {
			componentCount += 1;

			// compute individual score for field
			Object actualElement = actualJSONArray.get(i);
			Object expectedElement = expectedJSONArray.get(i);
			if (actualElement instanceof JSONObject && expectedElement instanceof JSONObject) {
				individualScoreSum += computeLossForCompositeTypes(emptyKey, emptyKey, (JSONObject) actualElement,
						(JSONObject) expectedElement);
			} else if (actualElement instanceof JSONArray && expectedElement instanceof JSONArray) {
				individualScoreSum += computeLossForArrays(emptyKey, emptyKey, (JSONArray) actualElement,
						(JSONArray) expectedElement);
			} else {
				individualScoreSum += computeLossForOtherTypes(emptyKey, emptyKey, actualElement, expectedElement);
			}
		}

		// Loss based on extra elements
		int absLengthDifference = Math.abs(actualLen - expectedLen);
		componentCount += absLengthDifference;
		individualScoreSum += absLengthDifference;

		return individualScoreSum / (double) componentCount;
	}

	private double computeLossForOtherTypes(String actualJSONKey, String expectedJSONKey, Object actualValue,
			Object expectedValue) {
		double individualScoreSum = 0;
		int componentCount = 0;

		// Loss based on type
		if (!emptyKey.equals(actualJSONKey)) {
			componentCount += 1;
			individualScoreSum += computeLossBetweenTypes(actualJSONKey, expectedJSONKey);
		}

		// Loss based on value
		componentCount += 1;
		individualScoreSum += computeLossBetweenValues(actualValue.toString(), expectedValue.toString());

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
		try {
			String field1 = key1.split(":")[0].trim();
			String field2 = key2.split(":")[0].trim();
			return field1.equals(field2);
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}

	/**
	 * Assume both keys are valid and comparable. Loss between two keys == 0 if they
	 * have the same type, Loss == 1 otherwise.
	 */
	private int computeLossBetweenTypes(String key1, String key2) {
		Set<String> equivalentTypes = new HashSet<>();
		equivalentTypes.add("java.lang.String");
		equivalentTypes.add("char[]");
		equivalentTypes.add("byte[]");

		try {
			String type1 = key1.split(":")[1].trim();
			String type2 = key2.split(":")[1].trim();

			if (equivalentTypes.contains(type1) && equivalentTypes.contains(type2)) {
				return 0;
			}

			return identityFunction(type1, type2);
		} catch (ArrayIndexOutOfBoundsException e) {
			return 1;
		}
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
