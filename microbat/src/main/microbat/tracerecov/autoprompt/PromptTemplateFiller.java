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
			+ "	java.util.HashMap$Node<String, Integer>[] table;\n"
			+ "	java.util.Set<Map.Entry<String, Integer>> entrySet;\n"
			+ "	int size;\n"
			+ "}\n"
			+ "\n"
			+ "Given \"map.toString()\" has output value:\n"
			+ "{\"k1\"=1, \"k2\"=2}\n"
			+ "\n"
			+ "We can summarize the structure as:\n"
			+ "{\n"
			+ "  \"map: java.util.HashMap<String, Integer>\": {\n"
			+ "    \"table: java.util.HashMap$Node<String, Integer>[]\": [\"k1\"=1, \"k2\"=2],\n"
			+ "    \"entrySet: java.util.Set<Map.Entry<String, Integer>>\": {\n"
			+ "      this$0:java.util.HashMap<String, Integer>:{\n"
			+ "        table:java.util.HashMap$Node<String, Integer>[]:[\"k1\"=1, \"k2\"=2]\n"
			+ "      }\n"
			+ "    },\n"
			+ "    \"size: int\": 2\n"
			+ "  }\n"
			+ "}\n"
			+ "";

	private static String variableExpansionAdjustmentPromptPrefix = 
			"You are given a prompt template with examples which might be inaccurate.\n"
			+ "\n"
			+ "Given an additional example of the structure of a variable:\n"
			+ "1. Check the existing examples and extract all the composite types.\n"
			+ "2. Take note of the data types with generic type parameters. If there are generic types, take note of the generic types with primitive equivalences. e.g. Integer has corresponding primitive type int.\n"
			+ "3. Replace one of the generic types in step 2 with the data type in the additional example.\n"
			+ "4. Replace the existing example with the updated example.\n"
			+ "Demonstration: If the existing example is `ArrayList<Integer>` and the additional example is `PrintWriter`. The generic type `Integer` has primitive equivalence `int`. Replace `Integer` with `PrintWriter`, the updated example should be `ArrayList<PrintWriter>`. You should first create an instance of `ArrayList<PrintWriter>`, then populate it with `PrintWriter` instances.\n"
			+ "\n"
			+ "Notes:\n"
			+ "- Between tags <Example></Example>, return the updated Example section.\n"
			+ "- Do not include <Background> and <Question>.\n"
			+ "- You should use the specific data type in the additional example below.\n"
			+ "- Do not include any explanation.\n";

	public PromptTemplateFiller() {
	}
	
	public String getDefaultVariableExpansionPromptExample() {
		return variableExpansionPromptExample;
	}

	public String getDefaultVariableExpansionPromptQuestion() {
		HashMap<String, String> placeholders = new HashMap<>();
		placeholders.put("var_name", "___variable name___");
		placeholders.put("var_type", "___variable type___");
		placeholders.put("var_value", "___variable value___");
		placeholders.put("class_structure", "___class structure___");
		placeholders.put("source_code", "___source code___");

		return getVariableExpansionPromptQuestion(placeholders);
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

	public String getVariableExpansionPromptTemplate(String example) {
		return variableExpansionPromptBackground + example + getDefaultVariableExpansionPromptQuestion();
	}

	public String getVariableExpansionPrompt(HashMap<String, String> datapoint, String example) {
		return variableExpansionPromptBackground + example + getVariableExpansionPromptQuestion(datapoint);
	}

	/**
	 * datapoint features:
	 * 
	 * var_name, var_type, var_value, class_structure, source_code, ground_truth
	 */
	public String getVariableExpansionAdjustmentPrompt(HashMap<String, String> datapoint, String textualLoss,
			String example) {
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
		stringBuilder.append(variableExpansionPromptBackground + example + getDefaultVariableExpansionPromptQuestion());
		stringBuilder.append("\n\"\"\"");

		return stringBuilder.toString();
	}

	public String getDefaultVariableExpansionAdjustmentPrompt(HashMap<String, String> datapoint, String textualLoss) {
		return getVariableExpansionAdjustmentPrompt(datapoint, textualLoss, variableExpansionPromptExample);
	}
}
