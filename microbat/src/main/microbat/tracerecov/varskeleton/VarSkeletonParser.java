package microbat.tracerecov.varskeleton;

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

		VariableSkeleton variableSkeleton = new VariableSkeleton(varType);
		parseClassStructureRecur(varValue, variableSkeleton);

		return variableSkeleton;
	}

	public void parseClassStructureRecur(String varValue, VariableSkeleton parentVar) {
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
}
