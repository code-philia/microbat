package microbat.tracerecov.autoprompt;

import java.util.HashMap;

public class PromptTemplateFiller {

	private static String variableExpansionPromptBackground = 
			"<Background>\n"
			+ "When executing a Java third-party library, some of its internal variables are critical for debugging. Please identify the most critical internal variables of a Java data structure for debugging. \n"
			+ "\n";

	private static String variableExpansionPromptExample = 
			"<Example>\n"
			+ "Class Name: java.util.HashMap<String, Integer>\n"
			+ "Structure: {\n"
			+ " java.util.HashMap$Node[] table;\n"
			+ " java.util.Set entrySet;\n"
			+ " int size;\n"
			+ "}\n"
			+ "\n"
			+ "Given \"map.toString()\" has output value:\n"
			+ "{\"k1\"=1, \"k2\"=2}\n"
			+ "\n"
			+ "We can summarize the structure as:\n"
			+ "{\n"
			+ "  \"map: java.util.HashMap\": {\n"
			+ "    \"table: java.util.HashMap$Node[]\": [\"k1\"=1, \"k2\"=2],\n"
			+ "    \"entrySet: java.util.Set\": {\n"
			+ "      this$0:java.util.HashMap:{\n"
			+ "        table:java.util.HashMap$Node[]:[\"k1\"=1, \"k2\"=2]\n"
			+ "      }\n"
			+ "    },\n"
			+ "    \"size: int\": 2\n"
			+ "  }\n"
			+ "}\n"
			+ "</Example>";

	private static String variableExpansionAdjustmentPromptPrefix = 
			"You are given a prompt template with examples which might be inaccurate.\n"
			+ "\n"
			+ "Given a variable in an existing example `V_e` and a variable in the additional example `V_a`:\n"
			+ "1. Check `V_e` (e.g., `java.util.HashMap<String, Integer>`), identify all generic types present in `V_e` (e.g., `String`, `Integer`). If there are generic types, take note of the generic types with primitive equivalences, OR String. e.g. Integer has corresponding primitive type int.\n"
			+ "2. Choose one of the identified generic types as `T_g`. Let `T_a` be the data type of `V_a` (e.g., `java.lang.StringBuffer`). Replace `T_g` with `T_a`. Let `T_e` be the updated data type of `V_e` (e.g., `java.util.HashMap<String, java.lang.StringBuffer>`).\n"
			+ "3. Create an instance of `T_e`. Create some instances of `T_a`. Fully expand the structure of `V_a` according to its class definition, including all fields and their respective data types. \n"
			+ "4. Populate the `T_e` instance using fully expanded `T_a` instances from step 3.\n"
			+ "5. Replace `V_e` with the updated example. Do not include any explanation.\n";

	public PromptTemplateFiller() {
	}

	public String getDefaultVariableExpansionPromptExample() {
		return variableExpansionPromptExample;
	}

	public String getVariableExpansionPromptQuestion(HashMap<String, String> datapoint) {
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

	public String getVariableExpansionPrompt(HashMap<String, String> datapoint, String example) {
		return variableExpansionPromptBackground + example + getVariableExpansionPromptQuestion(datapoint);
	}

	/**
	 * datapoint features:
	 * 
	 * var_name, var_type, var_value, class_structure, source_code, ground_truth
	 */
	public String getVariableExpansionAdjustmentPrompt(HashMap<String, String> datapoint, String example) {
		/* datapoint features */
		String varType = datapoint.get("var_type");
		String varValue = datapoint.get("var_value");
		String classStructure = datapoint.get("class_structure");
		String groundTruth = datapoint.get("ground_truth");

		StringBuilder stringBuilder = new StringBuilder(variableExpansionAdjustmentPromptPrefix);

		// basic information
		stringBuilder.append("\nAdditional Example:");
		stringBuilder.append("\nClass Name: " + varType);
		stringBuilder.append("\nStructure: " + classStructure);
		stringBuilder.append("\nVariable Value: " + varValue);

		// ground truth
		stringBuilder.append("\nWe can summarize the structure as:\n```json\n" + groundTruth + "\n```");

		// instruction
		stringBuilder.append("\nUpdate the Prompt template: \"\"\"\n");
		stringBuilder.append(example);
		stringBuilder.append("\n\"\"\"");

		return stringBuilder.toString();
	}

	public String getDefaultVariableExpansionAdjustmentPrompt(HashMap<String, String> datapoint) {
		return getVariableExpansionAdjustmentPrompt(datapoint, variableExpansionPromptExample);
	}
}
