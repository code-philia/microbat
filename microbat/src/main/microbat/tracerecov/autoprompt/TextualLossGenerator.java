package microbat.tracerecov.autoprompt;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class is used to compute the loss function in textual form for automatic
 * prompt engineering.
 */
public class TextualLossGenerator {

	public TextualLossGenerator() {
	}

	public String getLossFromException(String output, Exception exception) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("When parsing the previously generated output: ```\n" + output + "\n```,");
		stringBuilder.append("The following exception was thrown: " + exception.getMessage());

		return stringBuilder.toString();
	}

	/**
	 * Compute loss from root of the variable.
	 */
	public String getLoss(JSONObject actualJSON, JSONObject expectedJSON) {
		// Assume there is only one key in the root.
		String actualJSONKey = null;
		for (String entry : actualJSON.keySet()) {
			actualJSONKey = entry;
			break;
		}
		String expectedJSONKey = null;
		for (String entry : expectedJSON.keySet()) {
			expectedJSONKey = entry;
			break;
		}

		if (actualJSONKey == null || expectedJSONKey == null) {
			return "Variable is empty.";
		}

		Object actualJSONValue = actualJSON.get(actualJSONKey);
		Object expectedJSONValue = expectedJSON.get(expectedJSONKey);
		if (actualJSONValue instanceof JSONObject && expectedJSONValue instanceof JSONObject) {
			return getLossForCompositeTypes(actualJSONKey, expectedJSONKey, (JSONObject) actualJSONValue,
					(JSONObject) expectedJSONValue);
		} else if (actualJSONValue instanceof JSONArray && expectedJSONValue instanceof JSONArray) {
			return getLossForArrays(actualJSONKey, expectedJSONKey, (JSONArray) actualJSONValue,
					(JSONArray) expectedJSONValue);
		} else if (actualJSONValue instanceof JSONArray && expectedJSONValue instanceof String) {
			// expectedJSONValue cannot be parsed into JSONArray
			JSONArray expectedJSONArray = trimArrayString((String) expectedJSONValue);
			return getLossForArrays(actualJSONKey, expectedJSONKey, (JSONArray) actualJSONValue, expectedJSONArray);
		} else if (expectedJSONValue instanceof JSONArray && actualJSONValue instanceof String) {
			// actualJSONValue cannot be parsed into JSONArray
			JSONArray actualJSONArray = trimArrayString((String) actualJSONValue);
			return getLossForArrays(actualJSONKey, expectedJSONKey, actualJSONArray, (JSONArray) expectedJSONValue);
		} else if (actualJSONValue instanceof String && expectedJSONValue instanceof String) {
			// both are string, attempt to parse them into arrays
			try {
				JSONArray actualJSONArray = trimArrayString((String) actualJSONValue);
				JSONArray expectedJSONArray = trimArrayString((String) expectedJSONValue);
				return getLossForArrays(actualJSONKey, expectedJSONKey, actualJSONArray, expectedJSONArray);
			} catch (JSONException e) {
				return getLossForOtherTypes(actualJSONKey, expectedJSONKey, actualJSONValue, expectedJSONValue);
			}
		} else {
			return getLossForOtherTypes(actualJSONKey, expectedJSONKey, actualJSONValue, expectedJSONValue);
		}
	}

	private String getLossForCompositeTypes(String actualJSONKey, String expectedJSONKey, JSONObject actualJSONValue,
			JSONObject expectedJSONValue) {
		StringBuilder loss = new StringBuilder();

		// Loss based on type
		loss.append(getLossBetweenTypes(actualJSONKey, expectedJSONKey));

		// Loss based on common fields
		Set<String> actualJSONKeyset = actualJSONValue.keySet();
		Set<String> expectedJSONKeyset = expectedJSONValue.keySet();
		for (String actualFieldSignature : actualJSONKeyset) {
			String expectedFieldSignature = getComparableKey(actualFieldSignature, expectedJSONKeyset);
			if (expectedFieldSignature != null) {
				// compute individual score for field
				Object actualFieldValue = actualJSONValue.get(actualFieldSignature);
				Object expectedFieldValue = expectedJSONValue.get(expectedFieldSignature);
				String fieldLoss;
				if (actualFieldValue instanceof JSONObject && expectedFieldValue instanceof JSONObject) {
					fieldLoss = getLossForCompositeTypes(actualFieldSignature, expectedFieldSignature,
							(JSONObject) actualFieldValue, (JSONObject) expectedFieldValue);
				} else if (actualFieldValue instanceof JSONArray && expectedFieldValue instanceof JSONArray) {
					fieldLoss = getLossForArrays(actualFieldSignature, expectedFieldSignature,
							(JSONArray) actualFieldValue, (JSONArray) expectedFieldValue);
				} else if (actualFieldValue instanceof JSONArray && expectedFieldValue instanceof String) {
					// expectedFieldValue cannot be parsed into JSONArray
					JSONArray expectedJSONArray = trimArrayString((String) expectedFieldValue);
					fieldLoss = getLossForArrays(actualFieldSignature, expectedFieldSignature,
							(JSONArray) actualFieldValue, expectedJSONArray);
				} else if (expectedFieldValue instanceof JSONArray && actualFieldValue instanceof String) {
					// actualFieldValue cannot be parsed into JSONArray
					JSONArray actualJSONArray = trimArrayString((String) actualFieldValue);
					fieldLoss = getLossForArrays(actualFieldSignature, expectedFieldSignature, actualJSONArray,
							(JSONArray) expectedFieldValue);
				} else if (actualFieldValue instanceof String && expectedFieldValue instanceof String) {
					// both are string, attempt to parse them into arrays
					try {
						JSONArray actualJSONArray = trimArrayString((String) actualFieldValue);
						JSONArray expectedJSONArray = trimArrayString((String) expectedFieldValue);
						fieldLoss = getLossForArrays(actualFieldSignature, expectedFieldSignature, actualJSONArray,
								expectedJSONArray);
					} catch (JSONException e) {
						fieldLoss = getLossForOtherTypes(actualFieldSignature, expectedFieldSignature, actualFieldValue,
								expectedFieldValue);
					}
				} else {
					fieldLoss = getLossForOtherTypes(actualFieldSignature, expectedFieldSignature, actualFieldValue,
							expectedFieldValue);
				}
				loss.append(fieldLoss);
			}
		}

		// Loss based on different fields
		String keySetDifferences = getKeySetDifferences(actualJSONKeyset, expectedJSONKeyset);
		loss.append(keySetDifferences);

		return loss.toString();
	}

	private String getLossForArrays(String actualJSONKey, String expectedJSONKey, JSONArray actualJSONArray,
			JSONArray expectedJSONArray) {
		StringBuilder loss = new StringBuilder();

		// Loss based on type
		loss.append(getLossBetweenTypes(actualJSONKey, expectedJSONKey));

		// Loss based on elements with overlapping indices
		int actualLen = actualJSONArray.length();
		int expectedLen = expectedJSONArray.length();
		int overlappingLength = Math.min(actualLen, expectedLen);
		for (int i = 0; i < overlappingLength; i++) {
			// compute individual score for field
			Object actualElement = actualJSONArray.get(i);
			Object expectedElement = expectedJSONArray.get(i);
			String elementLoss;

			String arrayName = getFieldName(actualJSONKey);
			String arrayElementType = getArrayElementType(actualJSONKey);
			String key = arrayName + "[" + i + "]:" + arrayElementType;

			if (actualElement instanceof JSONObject && expectedElement instanceof JSONObject) {
				elementLoss = getLossForCompositeTypes(key, key, (JSONObject) actualElement,
						(JSONObject) expectedElement);
			} else if (actualElement instanceof JSONArray && expectedElement instanceof JSONArray) {
				elementLoss = getLossForArrays(key, key, (JSONArray) actualElement, (JSONArray) expectedElement);
			} else if (actualElement instanceof JSONArray && expectedElement instanceof String) {
				// expectedElement cannot be parsed into JSONArray
				JSONArray expectedChildJSONArray = trimArrayString((String) expectedElement);
				elementLoss = getLossForArrays(key, key, (JSONArray) actualElement, expectedChildJSONArray);
			} else if (expectedElement instanceof JSONArray && actualElement instanceof String) {
				// actualElement cannot be parsed into JSONArray
				JSONArray actualChildJSONArray = trimArrayString((String) actualElement);
				elementLoss = getLossForArrays(key, key, actualChildJSONArray, (JSONArray) expectedElement);
			} else if (actualElement instanceof String && expectedElement instanceof String) {
				// both are string, attempt to parse them into arrays
				try {
					JSONArray actualChildJSONArray = trimArrayString((String) actualElement);
					JSONArray expectedChildJSONArray = trimArrayString((String) expectedElement);
					elementLoss = getLossForArrays(key, key, actualChildJSONArray, expectedChildJSONArray);
				} catch (JSONException e) {
					elementLoss = getLossForOtherTypes(key, key, actualElement, expectedElement);
				}
			} else {
				elementLoss = getLossForOtherTypes(key, key, actualElement, expectedElement);
			}
			loss.append(elementLoss);
		}

		// Loss based on extra elements
		for (int i = actualLen - 1; i >= overlappingLength; i--) {
			Object actualElement = actualJSONArray.get(i);
			if (actualElement != null && !actualElement.equals(JSONObject.NULL)) {
				int extraElementCount = i - (overlappingLength - 1);
				loss.append("the output array mistakenly includes " + extraElementCount + " more elements\n");
				break;
			}
		}
		for (int i = expectedLen - 1; i >= overlappingLength; i--) {
			Object expectedElement = expectedJSONArray.get(i);
			if (expectedElement != null && !expectedElement.equals(JSONObject.NULL)) {
				int extraElementCount = i - (overlappingLength - 1);
				loss.append("the output array mistakenly excludes " + extraElementCount + " less elements\n");
				break;
			}
		}

		return loss.toString();
	}

	private String getLossForOtherTypes(String actualJSONKey, String expectedJSONKey, Object actualValue,
			Object expectedValue) {
		StringBuilder loss = new StringBuilder();

		// Loss based on type
		loss.append(getLossBetweenTypes(actualJSONKey, expectedJSONKey));

		// Loss based on value
		loss.append(getLossBetweenValues(actualJSONKey, expectedJSONKey, actualValue.toString(), expectedValue.toString()));

		return loss.toString();
	}

	/**
	 * Copied from {@code LossCalculator} class.
	 */
	private String getComparableKey(String key, Set<String> keySet) {
		for (String potentialMatch : keySet) {
			if (isValidKey(potentialMatch) && areComparableKeys(key, potentialMatch)) {
				return potentialMatch;
			}
		}
		return null;
	}

	/**
	 * Copied from {@code LossCalculator} class.
	 */
	private boolean isValidKey(String key) {
		String[] items = key.split(":");
		return items.length == 2;
	}

	/**
	 * Copied from {@code LossCalculator} class.
	 */
	private String getFieldName(String key) {
		return key.split(":")[0].trim();
	}

	/**
	 * Copied from {@code LossCalculator} class.
	 */
	private String getFieldType(String key) {
		return key.split(":")[1].trim();
	}

	/**
	 * Copied from {@code LossCalculator} class.
	 */
	private String getArrayElementType(String key) {
		String arrayType = getFieldType(key);
		if (arrayType.contains("[]")) {
			return arrayType.substring(0, arrayType.lastIndexOf("[]"));
		} else {
			return arrayType;
		}
	}

	/**
	 * Copied from {@code LossCalculator} class.
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

	/**
	 * Copied from {@code LossCalculator} class.
	 */
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
	private String getLossBetweenTypes(String actualKey, String expectedKey) {
		try {
			String actualType = getFieldType(actualKey);
			String expectedType = getFieldType(expectedKey);
			if (areStringEquivalences(actualType, expectedType)) {
				return "";
			}

			if (actualType.equals(expectedType)) {
				return "";
			} else {
				StringBuilder loss = new StringBuilder();
				loss.append("`" + actualKey + "`: ");
				loss.append("actual type is " + actualType);
				loss.append("; expected type is " + expectedType);
				loss.append("\n");
				return loss.toString();
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return "JSON key `" + actualKey + "` has wrong format, expected format: `" + expectedKey + "`\n";
		}
	}

	/**
	 * Copied from {@code LossCalculator} class.
	 */
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
	
	private String getLossBetweenValues(String actualKey, String expectedKey, String actualValue, String expectedValue) {
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

		if (actualValue.equals(expectedValue)) {
			return "";
		} else {
			StringBuilder loss = new StringBuilder();
			loss.append("`" + actualKey + "`: ");
			loss.append("actual value is " + actualValue);
			loss.append("; expected value is " + expectedValue);
			loss.append("\n");
			return loss.toString();
		}
	}

	private String getKeySetDifferences(Set<String> actualKeyset, Set<String> expectedKeyset) {
		StringBuilder loss = new StringBuilder();
		Set<String> extraKeys = new HashSet<>();
		Set<String> missingKeys = new HashSet<>();

		for (String actualKey : actualKeyset) {
			String expectedKey = getComparableKey(actualKey, expectedKeyset);
			if (expectedKey == null) {
				extraKeys.add(actualKey);
			}
		}
		for (String expectedKey : expectedKeyset) {
			String actualKey = getComparableKey(expectedKey, actualKeyset);
			if (actualKey == null) {
				missingKeys.add(expectedKey);
			}
		}

		if (!extraKeys.isEmpty()) {
			loss.append("the output mistakenly includes keys: {");
			for (String key : extraKeys) {
				loss.append("`");
				loss.append(key);
				loss.append("`;");
			}
			loss.append("}\n");
		}
		if (!missingKeys.isEmpty()) {
			loss.append("the output mistakenly excludes keys: {");
			for (String key : missingKeys) {
				loss.append("`");
				loss.append(key);
				loss.append("`;");
			}
			loss.append("}\n");
		}

		return loss.toString();
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
