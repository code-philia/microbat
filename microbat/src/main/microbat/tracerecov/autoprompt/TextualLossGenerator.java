package microbat.tracerecov.autoprompt;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class is used to compute the loss function in textual form for automatic
 * prompt engineering.
 */
public class TextualLossGenerator {

	private String emptyKey = ":";

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
		} else {
			return getLossForOtherTypes(actualJSONKey, expectedJSONKey, actualJSONValue, expectedJSONValue);
		}
	}

	private String getLossForCompositeTypes(String actualJSONKey, String expectedJSONKey, JSONObject actualJSONValue,
			JSONObject expectedJSONValue) {
		StringBuilder loss = new StringBuilder();

		// Loss based on type
		if (!emptyKey.equals(actualJSONKey)) {
			loss.append(getLossBetweenTypes(actualJSONKey, expectedJSONKey));
		}

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
		if (!emptyKey.equals(actualJSONKey)) {
			loss.append(getLossBetweenTypes(actualJSONKey, expectedJSONKey));
		}

		// Loss based on elements with overlapping indices
		int actualLen = actualJSONArray.length();
		int expectedLen = expectedJSONArray.length();
		int overlappingLength = Math.min(actualLen, expectedLen);
		for (int i = 0; i < overlappingLength; i++) {
			// compute individual score for field
			Object actualElement = actualJSONArray.get(i);
			Object expectedElement = expectedJSONArray.get(i);
			String elementLoss;
			if (actualElement instanceof JSONObject && expectedElement instanceof JSONObject) {
				elementLoss = getLossForCompositeTypes(emptyKey, emptyKey, (JSONObject) actualElement,
						(JSONObject) expectedElement);
			} else if (actualElement instanceof JSONArray && expectedElement instanceof JSONArray) {
				elementLoss = getLossForArrays(emptyKey, emptyKey, (JSONArray) actualElement,
						(JSONArray) expectedElement);
			} else {
				elementLoss = getLossForOtherTypes(emptyKey, emptyKey, actualElement, expectedElement);
			}
			loss.append(elementLoss);
		}

		// Loss based on extra elements
		int lengthDifference = actualLen - expectedLen;
		if (lengthDifference > 0) {
			loss.append("the output array mistakenly includes " + lengthDifference + " more elements\n");
		} else if (lengthDifference < 0) {
			loss.append("the output array mistakenly excludes " + lengthDifference + " less elements\n");
		}

		return loss.toString();
	}

	private String getLossForOtherTypes(String actualJSONKey, String expectedJSONKey, Object actualValue,
			Object expectedValue) {
		StringBuilder loss = new StringBuilder();

		// Loss based on type
		if (!emptyKey.equals(actualJSONKey)) {
			loss.append(getLossBetweenTypes(actualJSONKey, expectedJSONKey));
		}

		// Loss based on value
		loss.append(getLossBetweenValues(actualJSONKey, actualValue.toString(), expectedValue.toString()));

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
	private String getLossBetweenTypes(String actualKey, String expectedKey) {
		try {
			String actualType = actualKey.split(":")[1].trim();
			String expectedType = expectedKey.split(":")[1].trim();

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

	private String getLossBetweenValues(String actualKey, String actualValue, String expectedValue) {
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
}
