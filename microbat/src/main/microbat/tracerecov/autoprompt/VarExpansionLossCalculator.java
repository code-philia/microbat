package microbat.tracerecov.autoprompt;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VarExpansionLossCalculator extends LossCalculator {

	/**
	 * Compute loss from root of the variable.
	 */
	@Override
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
		} else if (actualValue instanceof JSONArray && expectedValue instanceof String) {
			// expectedValue cannot be parsed into JSONArray
			try {
				JSONArray expectedJSONArray = trimArrayString((String) expectedValue);
				return computeLossForArrays(actualKey, expectedKey, (JSONArray) actualValue, expectedJSONArray);
			} catch (JSONException e) {
				return computeLossForOtherTypes(actualKey, expectedKey, actualValue, expectedValue);
			}
		} else if (expectedValue instanceof JSONArray && actualValue instanceof String) {
			// actualValue cannot be parsed into JSONArray
			try {
				JSONArray actualJSONArray = trimArrayString((String) actualValue);
				return computeLossForArrays(actualKey, expectedKey, actualJSONArray, (JSONArray) expectedValue);
			} catch (JSONException e) {
				return computeLossForOtherTypes(actualKey, expectedKey, actualValue, expectedValue);
			}
		} else if (actualValue instanceof String && expectedValue instanceof String) {
			// both are string, attempt to parse them into arrays
			try {
				JSONArray actualJSONArray = trimArrayString((String) actualValue);
				JSONArray expectedJSONArray = trimArrayString((String) expectedValue);
				return computeLossForArrays(actualKey, expectedKey, actualJSONArray, expectedJSONArray);
			} catch (JSONException e) {
				return computeLossForOtherTypes(actualKey, expectedKey, actualValue, expectedValue);
			}
		} else {
			return computeLossForOtherTypes(actualKey, expectedKey, actualValue, expectedValue);
		}
	}

	private double computeLossForCompositeTypes(String actualJSONKey, String expectedJSONKey,
			JSONObject actualJSONValue, JSONObject expectedJSONValue) {
		double individualScoreSum = 0;
		int componentCount = 0;

		// Loss based on type
		componentCount += 1;
		individualScoreSum += computeLossBetweenTypes(actualJSONKey, expectedJSONKey);

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
				} else if (actualFieldValue instanceof JSONArray && expectedFieldValue instanceof String) {
					// expectedFieldValue cannot be parsed into JSONArray
					try {
						JSONArray expectedJSONArray = trimArrayString((String) expectedFieldValue);
						individualScoreSum += computeLossForArrays(actualFieldSignature, expectedFieldSignature,
								(JSONArray) actualFieldValue, expectedJSONArray);
					} catch (JSONException e) {
						individualScoreSum += computeLossForOtherTypes(actualFieldSignature, expectedFieldSignature,
								actualFieldValue, expectedFieldValue);
					}
				} else if (expectedFieldValue instanceof JSONArray && actualFieldValue instanceof String) {
					// actualFieldValue cannot be parsed into JSONArray
					try {
						JSONArray actualJSONArray = trimArrayString((String) actualFieldValue);
						individualScoreSum += computeLossForArrays(actualFieldSignature, expectedFieldSignature,
								actualJSONArray, (JSONArray) expectedFieldValue);
					} catch (JSONException e) {
						individualScoreSum += computeLossForOtherTypes(actualFieldSignature, expectedFieldSignature,
								actualFieldValue, expectedFieldValue);
					}
				} else if (actualFieldValue instanceof String && expectedFieldValue instanceof String) {
					// both are string, attempt to parse them into arrays
					try {
						JSONArray actualJSONArray = trimArrayString((String) actualFieldValue);
						JSONArray expectedJSONArray = trimArrayString((String) expectedFieldValue);
						individualScoreSum += computeLossForArrays(actualFieldSignature, expectedFieldSignature,
								actualJSONArray, expectedJSONArray);
					} catch (JSONException e) {
						individualScoreSum += computeLossForOtherTypes(actualFieldSignature, expectedFieldSignature,
								actualFieldValue, expectedFieldValue);
					}
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
		componentCount += 1;
		individualScoreSum += computeLossBetweenTypes(actualJSONKey, expectedJSONKey);

		// Loss based on elements with overlapping indices
		int actualLen = actualJSONArray.length();
		int expectedLen = expectedJSONArray.length();
		int overlappingLength = Math.min(actualLen, expectedLen);
		for (int i = 0; i < overlappingLength; i++) {
			componentCount += 1;

			// compute individual score for field
			Object actualElement = actualJSONArray.get(i);
			Object expectedElement = expectedJSONArray.get(i);

			String arrayName = getFieldName(actualJSONKey);
			String arrayElementType = getArrayElementType(actualJSONKey);
			String key = arrayName + "[" + i + "]:" + arrayElementType;

			if (actualElement instanceof JSONObject && expectedElement instanceof JSONObject) {
				individualScoreSum += computeLossForCompositeTypes(key, key, (JSONObject) actualElement,
						(JSONObject) expectedElement);
			} else if (actualElement instanceof JSONArray && expectedElement instanceof JSONArray) {
				individualScoreSum += computeLossForArrays(key, key, (JSONArray) actualElement,
						(JSONArray) expectedElement);
			} else if (actualElement instanceof JSONArray && expectedElement instanceof String) {
				// expectedElement cannot be parsed into JSONArray
				try {
					JSONArray expectedChildJSONArray = trimArrayString((String) expectedElement);
					individualScoreSum += computeLossForArrays(key, key, (JSONArray) actualElement,
							expectedChildJSONArray);
				} catch (JSONException e) {
					individualScoreSum += computeLossForOtherTypes(key, key, actualElement, expectedElement);
				}
			} else if (expectedElement instanceof JSONArray && actualElement instanceof String) {
				// actualElement cannot be parsed into JSONArray
				try {
					JSONArray actualChildJSONArray = trimArrayString((String) actualElement);
					individualScoreSum += computeLossForArrays(key, key, actualChildJSONArray,
							(JSONArray) expectedElement);
				} catch (JSONException e) {
					individualScoreSum += computeLossForOtherTypes(key, key, actualElement, expectedElement);
				}
			} else if (actualElement instanceof String && expectedElement instanceof String) {
				// both are string, attempt to parse them into arrays
				try {
					JSONArray actualChildJSONArray = trimArrayString((String) actualElement);
					JSONArray expectedChildJSONArray = trimArrayString((String) expectedElement);
					individualScoreSum += computeLossForArrays(key, key, actualChildJSONArray, expectedChildJSONArray);
				} catch (JSONException e) {
					individualScoreSum += computeLossForOtherTypes(key, key, actualElement, expectedElement);
				}
			} else {
				individualScoreSum += computeLossForOtherTypes(key, key, actualElement, expectedElement);
			}
		}

		// Loss based on extra elements
		for (int i = actualLen - 1; i >= overlappingLength; i--) {
			Object actualElement = actualJSONArray.get(i);
			if (actualElement != null && !actualElement.equals(JSONObject.NULL)) {
				int extraElementCount = i - (overlappingLength - 1);
				componentCount += extraElementCount;
				individualScoreSum += extraElementCount;
				break;
			}
		}
		for (int i = expectedLen - 1; i >= overlappingLength; i--) {
			Object expectedElement = expectedJSONArray.get(i);
			if (expectedElement != null && !expectedElement.equals(JSONObject.NULL)) {
				int extraElementCount = i - (overlappingLength - 1);
				componentCount += extraElementCount;
				individualScoreSum += extraElementCount;
				break;
			}
		}

		return individualScoreSum / (double) componentCount;
	}

	private double computeLossForOtherTypes(String actualJSONKey, String expectedJSONKey, Object actualValue,
			Object expectedValue) {
		double individualScoreSum = 0;
		int componentCount = 0;

		// Loss based on type
		componentCount += 1;
		individualScoreSum += computeLossBetweenTypes(actualJSONKey, expectedJSONKey);

		// Loss based on value
		componentCount += 1;
		individualScoreSum += computeLossBetweenValues(actualJSONKey, expectedJSONKey, actualValue.toString(),
				expectedValue.toString());

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

	private String getFieldName(String key) {
		return key.split(":")[0].trim();
	}

	private String getFieldType(String key) {
		return key.split(":")[1].trim();
	}

	private String getArrayElementType(String key) {
		String arrayType = getFieldType(key);
		if (arrayType.contains("[]")) {
			return arrayType.substring(0, arrayType.lastIndexOf("[]"));
		} else {
			return arrayType;
		}
	}

	/**
	 * Assume both keys are valid. Keys are comparable if they have the same field
	 * name (not necessarily sharing the same field type).
	 */
	private boolean areComparableKeys(String key1, String key2) {
		try {
			String field1 = getFieldName(key1);
			String field2 = getFieldName(key2);
			return field1.equals(field2);
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}

	private boolean areStringEquivalences(String type1, String type2) {
		Set<String> equivalentTypes = new HashSet<>();
		equivalentTypes.add("java.lang.String");
		equivalentTypes.add("char[]");
		equivalentTypes.add("byte[]");

		return equivalentTypes.contains(type1) && equivalentTypes.contains(type2);
	}

	/**
	 * Assume both keys are valid and comparable. Loss between two keys == 0 if they
	 * have the same type, Loss == 1 otherwise.
	 */
	private int computeLossBetweenTypes(String key1, String key2) {
		try {
			String type1 = getFieldType(key1);
			String type2 = getFieldType(key2);
			if (areStringEquivalences(type1, type2)) {
				return 0;
			}

			return identityFunction(type1, type2);
		} catch (ArrayIndexOutOfBoundsException e) {
			return 1;
		}
	}

	private String convertArrayToString(String plainArrayString) {
		if (plainArrayString.startsWith("[") && plainArrayString.endsWith("]")) {
			plainArrayString = plainArrayString.substring(1, plainArrayString.length() - 1);
			String[] entries = plainArrayString.split(",");
			StringBuilder stringBuilder = new StringBuilder();
			for (String entry : entries) {
				entry = entry.trim();
				if (entry == "") {
					entry = " ";
				}
				stringBuilder.append(entry);
			}
			return stringBuilder.toString();
		} else {
			return plainArrayString;
		}
	}

	private int computeLossBetweenValues(String actualKey, String expectedKey, String actualValue,
			String expectedValue) {
		String actualType = getFieldType(actualKey);
		String expectedType = getFieldType(expectedKey);
		if (areStringEquivalences(actualType, expectedType)) {
			actualValue = convertArrayToString(actualValue);
			expectedValue = convertArrayToString(expectedValue);
		} else if (actualType.equals("byte")) {
			try {
				int actualByte = Integer.valueOf(actualValue);
				char character = (char) actualByte;
				actualValue = String.valueOf(character);
			} catch (java.lang.NumberFormatException e) {
				// do nothing
			}
		}

		return identityFunction(actualValue, expectedValue);
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

	private JSONArray trimArrayString(String value) {
		StringBuilder trimmedStringBuilder = new StringBuilder();
		String[] entries = value.split(",");
		for (int i = 0; i < entries.length; i++) {
			String entry = entries[i];
			trimmedStringBuilder.append(entry.trim());
			if (i != entries.length - 1) {
				trimmedStringBuilder.append(",");
			}
		}
		return new JSONArray(trimmedStringBuilder.toString());
	}
}
