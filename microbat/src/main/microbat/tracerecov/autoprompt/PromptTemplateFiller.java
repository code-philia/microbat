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

	private static String variableExpansionPromptQuestion = 
			"\n\n<Question>\n"
			+ "Given variable *___variable name___* of type ___variable type___, value \"___variable value___\", internal structure ___class structure___.\n"
			+ "Execute ```___source code___```.\n"
			+ "Return in JSON format for this variable as shown in the above example. You must follow the JSON format as \"var_name:var_type\": var_value. Do not include explanation in your response.";
	
	private static String variableExpansionAdjustmentPromptPrefix = 
			"You are given a prompt template with an example which might be inaccurate.\n"
			+ "\n"
			+ "Given another example of the structure of a variable: \n"
			+ "If the example variables have different data structures, create an example of a nested data structure from the two given data structures. For example, if the two data structures are `ArrayList` and `HashSet`, you should first create an instance of `ArrayList`, then populate it with `HashSet` instances.\n"
			+ "Otherwise, modify the existing example such that its structure is similar to the additional example, keep one version of the example in the prompt.\n"
			+ "\n"
			+ "Replace the old example in the prompt by the updated example. Make sure you nest the structure in Additional Example with the structure in the <Example>.\n"
			+ "\n"
			+ "Additional Example:";
	
	private static String getVariableExpansionPromptTemplate(String example) {
		return variableExpansionPromptBackground + example + variableExpansionPromptQuestion;
	}
	
	/**
	 * datapoint features:
	 * 
	 * var_name, var_type, var_value, class_structure, source_code, ground_truth
	 */
	public static String getVariableExpansionAdjustmentPrompt(HashMap<String, String> datapoint) {
		/* datapoint features */
		String varName = datapoint.get("var_name");
		String varType = datapoint.get("var_type");
		String varValue = datapoint.get("var_value");
		String classStructure = datapoint.get("class_structure");
		String sourceCode = datapoint.get("source_code");
		String groundTruth = datapoint.get("ground_truth");

		StringBuilder stringBuilder = new StringBuilder(variableExpansionAdjustmentPromptPrefix);

		// basic information
		stringBuilder.append("\nClass Name: " + varType);
		stringBuilder.append("\nStructure: " + classStructure);
		stringBuilder.append("\nVariable Value: " + varValue);

		// ground truth
		stringBuilder.append("\nWe can summarize the structure as:\n```json\n" + groundTruth + "\n```");

		// instruction
		stringBuilder.append("\nUpdate the Prompt template: \"\"\"\n");
		stringBuilder.append(getVariableExpansionPromptTemplate(variableExpansionPromptExample));
		stringBuilder.append("\n\"\"\"");
		stringBuilder.append("\nReturn the updated <Example> section directly and do not include any explanation.");

		return stringBuilder.toString();
	}
	
	public static String getVariableExpansionPromptExample() {
		return variableExpansionPromptExample;
	}

	public static void setVariableExpansionPromptExample(String variableExpansionPromptExample) {
		PromptTemplateFiller.variableExpansionPromptExample = variableExpansionPromptExample;
	}
}
