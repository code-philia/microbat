package microbat.tracerecov.autoprompt;

import java.util.HashMap;

public class PromptTemplateFiller {
	private static String variableExpansionPromptBackground = 
			"<Background>\n"
			+ "When executing a Java third-party library, some of its internal variables are critical for debugging. Please identify the most critical internal variables of a Java data structure for debugging. \n"
			+ "\n";
	
	private static String variableExpansionPromptExample = 
			"<Example>\n"
			+ "Class Name: java.util.HashMap<HashMap, ArrayList>\n"
			+ "Structure: {\n"
			+ "	java.util.HashMap$Node[] table;\n"
			+ "	java.util.Set entrySet;\n"
			+ "	int size;\n"
			+ "	int modCount;\n"
			+ "	int threshold;\n"
			+ "	float loadFactor;\n"
			+ "	java.util.Set keySet;\n"
			+ "	java.util.Collection values;\n"
			+ "}\n"
			+ "\n"
			+ "Given “map.toString()” has output value:\n"
			+ "{{k1=1, k2=2} = [v1, v2], {kA=100, kB=200} = [vA, vB]}\n"
			+ "\n"
			+ "We can summarize the structure as \n"
			+ "Here is the given structure converted to JSON format (every variable shall strictly have a Java type, followed by its value):\n"
			+ "{\n"
			+ "  \"map: java.util.HashMap<HashMap, ArrayList>\": {\n"
			+ "    \"key[0]: HashMap\": {\n"
			+ "      {\n"
			+ "        \"key[0]: java.lang.String\": \"k1\",\n"
			+ "        \"key[1]: java.lang.String\": \"k2\",\n"
			+ "        \"value[0]: java.lang.Integer\": 1,\n"
			+ "        \"value[1]: java.lang.Integer\": 2,\n"
			+ "        \"size: int\": 2	\n"
			+ "      }\n"
			+ "    },\n"
			+ "    \"key[1]: java.util.HashMap\": {\n"
			+ "      \"map\": {\n"
			+ "        \"key[0]: java.lang.String\": \"kA\",\n"
			+ "        \"key[1]: java.lang.String\": \"kB\",\n"
			+ "        \"value[0]: java.lang.Integer\": 100,\n"
			+ "        \"value[1]: java.lang.Integer\": 200,\n"
			+ "         \"size: int\": 2\n"
			+ "       }\n"
			+ "     },\n"
			+ "     \"value[0]: java.util.ArrayList<String>\": { \n"
			+ "\"elementData: java.lang.Object[]\": [ \"v1\", \"v2\"], \n"
			+ "  \"size: int\": 2 \n"
			+ "       }, \n"
			+ "      \"value[1]: java.util.ArrayList<String>\": { \n"
			+ "\"elementData: java.lang.Object[]\": [ \"vA\", \"vB\"], \n"
			+ "  \"size: int\": 2 \n"
			+ "       },\n"
			+ "      \"size\": 2\n"
			+ "  },\n"
			+ " }";

	private static String variableExpansionAdjustmentPromptPrefix = 
			"You are given a prompt template with examples which might be inaccurate.\n"
			+ "\n"
			+ "Given an additional example of the structure of a variable:\n"
			+ "1. Check the existing examples. If an existing example contain the data type in the additional example, modify the existing example such that its structure is more similar as the additional example.\n"
			+ "OR 2. Check the existing examples. If there is a composite type with generic type, nest the additional example into the existing example. e.g. A, If the two data structures are `ArrayList<Integer>` and `PrintWriter`, you should first create an instance of `ArrayList<PrintWriter>`, then populate it with `PrintWriter` instances.\n"
			+ "e.g. B, If the two data structures are `ArrayList<PrintWriter>` and `HashSet<Integer>`, you should first create an instance of `HashSet<ArrayList<PrintWriter>>`, then populate it with `ArrayList` instances.\n"
			+ "OR 3. Add a new example based on the additional example.\n"
			+ "\n"
			+ "- Between tags <Example></Example>, return the updated Example section. \n"
			+ "- You must include all the composite types in the existing examples and the additional example.\n"
			+ "- Keep the example concise, use the minimum possible size for the data structures in the example.\n"
			+ "- Do not include <Background> and <Question>.\n"
			+ "- Do not include any explanation.\n";

	private static String getDefaultVariableExpansionPromptQuestion() {
		HashMap<String, String> placeholders = new HashMap<>();
		placeholders.put("var_name", "___variable name___");
		placeholders.put("var_type", "___variable type___");
		placeholders.put("var_value", "___variable value___");
		placeholders.put("class_structure", "___class structure___");
		placeholders.put("source_code", "___source code___");

		return getVariableExpansionPromptQuestion(placeholders);
	}

	private static String getVariableExpansionPromptQuestion(HashMap<String, String> datapoint) {
		/* datapoint features */
		String varName = datapoint.get("var_name");
		String varType = datapoint.get("var_type");
		String varValue = datapoint.get("var_value");
		String classStructure = datapoint.get("class_structure");
		String sourceCode = datapoint.get("source_code");

		StringBuilder stringBuilder = new StringBuilder("\n\n<Question>\n");
		stringBuilder.append("Given variable *" + varName + "*");
		stringBuilder.append(" of type " + varType + ",");
		stringBuilder.append(" value \"" + varValue + "\",");
		stringBuilder.append(" internal structure " + classStructure + ".\n");
		stringBuilder.append("Execute ```" + sourceCode + "```.\n");
		stringBuilder.append(
				"Return in JSON format for this variable as shown in the above example. You must follow the JSON format as \"var_name:var_type\": var_value. Do not include explanation in your response.");

		return stringBuilder.toString();
	}

	private static String getVariableExpansionPromptTemplate() {
		return variableExpansionPromptBackground + variableExpansionPromptExample
				+ getDefaultVariableExpansionPromptQuestion();
	}

	public static String getVariableExpansionPrompt(HashMap<String, String> datapoint) {
		return variableExpansionPromptBackground + variableExpansionPromptExample
				+ getVariableExpansionPromptQuestion(datapoint);
	}

	/**
	 * datapoint features:
	 * 
	 * var_name, var_type, var_value, class_structure, source_code, ground_truth
	 */
	public static String getVariableExpansionAdjustmentPrompt(HashMap<String, String> datapoint, String textualLoss) {
		/* datapoint features */
		String varType = datapoint.get("var_type");
		String varValue = datapoint.get("var_value");
		String classStructure = datapoint.get("class_structure");
		String groundTruth = datapoint.get("ground_truth");

		StringBuilder stringBuilder = new StringBuilder(variableExpansionAdjustmentPromptPrefix);

		if (textualLoss != null) {
			stringBuilder.append(
					"\nWhen generating the new examples, avoid the following wrong output from being generated again:\n");
			stringBuilder.append(textualLoss);
		}

		// basic information
		stringBuilder.append("\nAdditional Example:");
		stringBuilder.append("\nClass Name: " + varType);
		stringBuilder.append("\nStructure: " + classStructure);
		stringBuilder.append("\nVariable Value: " + varValue);

		// ground truth
		stringBuilder.append("\nWe can summarize the structure as:\n```json\n" + groundTruth + "\n```");

		// instruction
		stringBuilder.append("\nUpdate the Prompt template: \"\"\"\n");
		stringBuilder.append(getVariableExpansionPromptTemplate());
		stringBuilder.append("\n\"\"\"");

		return stringBuilder.toString();
	}

	public static String getVariableExpansionPromptExample() {
		return variableExpansionPromptExample;
	}

	public static void setVariableExpansionPromptExample(String variableExpansionPromptExample) {
		PromptTemplateFiller.variableExpansionPromptExample = variableExpansionPromptExample;
	}
}
