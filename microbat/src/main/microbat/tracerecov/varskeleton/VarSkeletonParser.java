package microbat.tracerecov.varskeleton;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class is used to parse a class structure string into Variable Skeleton.
 */
public class VarSkeletonParser {

	public VarSkeletonParser() {
	}

	public VariableSkeleton parseClassStructure(String classStructure) {
		String[] keyValuePair = classStructure.split(":", 2); // assume root layer has one key
		if (keyValuePair.length == 1) {
			String varType = keyValuePair[0];
			VariableSkeleton variableSkeleton = new VariableSkeleton(varType);
			return variableSkeleton;
		}

		String varType = keyValuePair[0];
		String varValue = keyValuePair[1];

		if (varType.contains("|")) {
			varType = varType.split("\\|")[1];
		}

		VariableSkeleton variableSkeleton = new VariableSkeleton(varType);
		parseClassStructureRecur(varValue, variableSkeleton);

		return variableSkeleton;
	}

	private void parseClassStructureRecur(String varValue, VariableSkeleton parentVar) {
		if (varValue.startsWith("{")) {
			varValue = extractVarValueContent(varValue);
		}

		int lastIndex = 0;
		String key = "";
		String value = "";
		int bracketCount = 0;
		char lastCharacter = ' ';
		for (int i = 0; i < varValue.length(); i++) {
			char character = varValue.charAt(i);
			if (character == ':' && bracketCount == 0) {
				key = varValue.substring(lastIndex, i);
				lastIndex = i + 1;
			} else if (character == '{') {
				bracketCount++;
			} else if (character == '}') {
				bracketCount--;
			} else if (character == ';' && bracketCount == 0) {
				if (lastCharacter == '}') {
					value = varValue.substring(lastIndex, i);

					String[] typeNamePair = key.split(" ");
					VariableSkeleton childVar = new VariableSkeleton(typeNamePair[0], typeNamePair[1]);
					parentVar.addChild(childVar);

					parseClassStructureRecur(value, childVar);
				} else {
					key = varValue.substring(lastIndex, i);

					String[] typeNamePair = key.split(" ");
					VariableSkeleton childVar = new VariableSkeleton(typeNamePair[0], typeNamePair[1]);
					parentVar.addChild(childVar);
				}
				lastIndex = i + 1;
			}
			lastCharacter = character;
		}
	}

	private String extractVarValueContent(String varValue) {
		int startIndex = varValue.indexOf("{");
		int endIndex = varValue.lastIndexOf("}");
		return varValue.substring(startIndex + 1, endIndex);
	}

	public VariableSkeleton parseVariableValueJSONObject(JSONObject variable) {
		String key = variable.keys().next(); // assume root layer has one key
		String[] nameAndType = key.split("\\|");
		Object value = variable.get(key);

		VariableSkeleton varSkeleton = new VariableSkeleton(nameAndType[1], nameAndType[0]);
		if (value instanceof JSONObject) {
			parseVariableValueJSONObjectRecur((JSONObject) value, varSkeleton);
		} else if (value instanceof JSONArray) {
			parseVariableValueJSONArrayRecur((JSONArray) value, varSkeleton);
		} else {
			parseVariableValueOthersRecur(value, varSkeleton);
		}

		return varSkeleton;
	}

	private void parseVariableValueJSONObjectRecur(JSONObject variable, VariableSkeleton parentVar) {
		for (String key : variable.keySet()) {
			String[] nameAndType = key.split("\\|");
			Object value = variable.get(key);

			VariableSkeleton childVar = new VariableSkeleton(nameAndType[1], nameAndType[0]);
			parentVar.addChild(childVar);

			if (value instanceof JSONObject) {
				parseVariableValueJSONObjectRecur((JSONObject) value, childVar);
			} else if (value instanceof JSONArray) {
				parseVariableValueJSONArrayRecur((JSONArray) value, childVar);
			} else {
				parseVariableValueOthersRecur(value, childVar);
			}
		}
	}

	private void parseVariableValueJSONArrayRecur(JSONArray variable, VariableSkeleton parentVar) {
		String arrayElementType = "Object";
		if (parentVar.getType().contains("[")) {
			parentVar.getType().substring(0, parentVar.getType().lastIndexOf("["));
		}
		String arrayName = parentVar.getName();
		int length = variable.length();
		for (int i = 0; i < length; i++) {
			String arrayElementName = arrayName + "[" + i + "]";
			Object value = variable.get(i);
			
			VariableSkeleton childVar = new VariableSkeleton(arrayElementType, arrayElementName);
			parentVar.addChild(childVar);
			
			if (value instanceof JSONObject) {
				parseVariableValueJSONObjectRecur((JSONObject) value, childVar);
			} else if (value instanceof JSONArray) {
				parseVariableValueJSONArrayRecur((JSONArray) value, childVar);
			} else {
				parseVariableValueOthersRecur(value, childVar);
			}
		}
	}

	private void parseVariableValueOthersRecur(Object value, VariableSkeleton parentVar) {
		String valueString = value.toString();
		parentVar.setValue(valueString);
	}
}
