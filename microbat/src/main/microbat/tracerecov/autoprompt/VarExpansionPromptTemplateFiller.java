package microbat.tracerecov.autoprompt;

import java.util.HashMap;

import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;

public class VarExpansionPromptTemplateFiller extends PromptTemplateFiller {

	private static String variableExpansionPromptBackground = "<Background>\r\n"
			+ "When executing a Java third-party library, some of its internal variables are critical for debugging. Please identify the most critical internal variables of a Java data structure for debugging. \r\n"
			+ "\r\n";

	private static String variableExpansionPromptExample = 
			"<Example>\r\n"
			+ "Class Name: java.util.HashMap<HashMap, ArrayList>\r\n"
			+ "Structure: {\r\n"
			+ "	java.util.HashMap$Node[] table;\r\n"
			+ "	java.util.Set entrySet;\r\n"
			+ "	int size;\r\n"
			+ "	int modCount;\r\n"
			+ "	int threshold;\r\n"
			+ "	float loadFactor;\r\n"
			+ "	java.util.Set keySet;\r\n"
			+ "	java.util.Collection values;\r\n"
			+ "}\r\n"
			+ "\r\n"
			+ "Given “map.toString()” has output value:\r\n"
			+ "{{k1=1, k2=2} = [v1, v2], {kA=100, kB=200} = [vA, vB]}\r\n"
			+ "\r\n"
			+ "We can summarize the structure as \r\n"
			+ "Here is the given structure converted to JSON format (every variable shall strictly have a Java type, followed by its value):\r\n"
			+ "{\r\n"
			+ "  \"map: java.util.HashMap<HashMap, ArrayList>\": {\r\n"
			+ "    \"key[0]: HashMap\": {\r\n"
			+ "      {\r\n"
			+ "        \"key[0]: java.lang.String\": \"k1\",\r\n"
			+ "        \"key[1]: java.lang.String\": \"k2\",\r\n"
			+ "        \"value[0]: java.lang.Integer\": 1,\r\n"
			+ "        \"value[1]: java.lang.Integer\": 2,\r\n"
			+ "        \"size: int\": 2	\r\n"
			+ "      }\r\n"
			+ "    },\r\n"
			+ "    \"key[1]: java.util.HashMap\": {\r\n"
			+ "      \"map\": {\r\n"
			+ "        \"key[0]: java.lang.String\": \"kA\",\r\n"
			+ "        \"key[1]: java.lang.String\": \"kB\",\r\n"
			+ "        \"value[0]: java.lang.Integer\": 100,\r\n"
			+ "        \"value[1]: java.lang.Integer\": 200,\r\n"
			+ "         \"size: int\": 2\r\n"
			+ "       }\r\n"
			+ "     },\r\n"
			+ "     \"value[0]: java.util.ArrayList<String>\": { \r\n"
			+ "\"elementData: java.lang.Object[]\": [ \"v1\", \"v2\"], \r\n"
			+ "  \"size: int\": 2 \r\n"
			+ "       }, \r\n"
			+ "      \"value[1]: java.util.ArrayList<String>\": { \r\n"
			+ "\"elementData: java.lang.Object[]\": [ \"vA\", \"vB\"], \r\n"
			+ "  \"size: int\": 2 \r\n"
			+ "       },\r\n"
			+ "      \"size\": 2\r\n"
			+ "  },\r\n"
			+ " }\r\n";

	// TODO: update later (not used for now)
	private static String variableExpansionAdjustmentPromptPrefix = 
			"You are given a prompt template with examples which might be inaccurate.\n"
			+ "\n"
			+ "Given a variable in an existing example `V_e` and a variable in the additional example `V_a`:\n"
			+ "1. Check `V_e` (e.g., `java.util.HashMap<String, Integer>`), identify all generic types present in `V_e` (e.g., `String`, `Integer`). If there are generic types, take note of the generic types with primitive equivalences, OR String. e.g. Integer has corresponding primitive type int.\n"
			+ "2. Choose one of the identified generic types as `T_g`. Let `T_a` be the data type of `V_a` (e.g., `java.lang.StringBuffer`). Replace `T_g` with `T_a`. Let `T_e` be the updated data type of `V_e` (e.g., `java.util.HashMap<String, java.lang.StringBuffer>`).\n"
			+ "3. Create an instance of `T_e`. Create some instances of `T_a`. Fully expand the structure of `V_a` according to its class definition, including all fields and their respective data types. \n"
			+ "4. Populate the `T_e` instance using fully expanded `T_a` instances from step 3.\n"
			+ "5. Replace `V_e` with the updated example. Do not include any explanation.\n";

	/* Prompt to be adjusted */

	@Override
	public String getDefaultPromptExample() {
		return variableExpansionPromptExample;
	}

	@Override
	public String getPromptQuestion(HashMap<String, String> datapoint) {
		/* datapoint features */
		String varName = datapoint.get(DatasetReader.VAR_NAME);
		String varType = datapoint.get(DatasetReader.VAR_TYPE);
		String varValue = datapoint.get(DatasetReader.VAR_VALUE);
		String classStructure = datapoint.get(DatasetReader.CLASS_STRUCTURE);
		String sourceCode = datapoint.get(DatasetReader.SOURCE_CODE);

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

	@Override
	public String getPrompt(HashMap<String, String> datapoint, String example) {
		return variableExpansionPromptBackground + example + getPromptQuestion(datapoint);
	}

	@Override
	public String getDefaultPrompt(HashMap<String, String> datapoint) {
		return variableExpansionPromptBackground + variableExpansionPromptExample + getPromptQuestion(datapoint);
	}

	@Override
	public String getExample(HashMap<String, String> datapoint, String groundTruthExample) {
		String varType = datapoint.get(DatasetReader.VAR_TYPE);
		String varValue = TraceRecovUtils.processInputStringForLLM(datapoint.get(DatasetReader.VAR_VALUE));
		String classStructure = datapoint.get(DatasetReader.CLASS_STRUCTURE);

		StringBuilder stringBuilder = new StringBuilder("<Example>\r\n");

		stringBuilder.append("Class Name: " + varType);
		stringBuilder.append("\nStructure: " + classStructure);
		stringBuilder.append("\nVariable Value: " + varValue);

		stringBuilder.append("\nWe can summarize the structure as:\n```json\n" + groundTruthExample + "\n```");

		return stringBuilder.toString();
	}

	/* Adjustment Prompt */

	/**
	 * datapoint features:
	 * 
	 * var_name, var_type, var_value, class_structure, source_code, ground_truth
	 */
	@Override
	public String getAdjustmentPrompt(HashMap<String, String> datapoint, String example) {
		StringBuilder stringBuilder = new StringBuilder(variableExpansionAdjustmentPromptPrefix);
		String groundTruth = datapoint.get(DatasetReader.GROUND_TRUTH);

		// basic information
		stringBuilder.append("\nAdditional Example:");
		stringBuilder.append(getExample(datapoint, groundTruth));

		// instruction
		stringBuilder.append("\nUpdate the Prompt template: \"\"\"\n");
		stringBuilder.append(example);
		stringBuilder.append("\n\"\"\"");

		return stringBuilder.toString();
	}

	@Override
	public String getDefaultAdjustmentPrompt(HashMap<String, String> datapoint) {
		return getAdjustmentPrompt(datapoint, variableExpansionPromptExample);
	}

	/* Adjustment Prompt Incorporating Textual Loss */

	@Override
	public String getAdjustmentPromptWithLoss(String example, HashMap<String, String> datapoint, String output,
			String textualLoss) {
		// existing example
		StringBuilder stringBuilder = new StringBuilder("Given example:\n");
		stringBuilder.append(example);
		stringBuilder.append("\n");

		// output
		stringBuilder.append("Based on the example, an output was generated:\n\"\"\"");
		stringBuilder.append(getExample(datapoint, output));
		stringBuilder.append("\"\"\"");

		// errors (textual loss) in output
		stringBuilder.append("It has the following errors:\n\"\"\"");
		stringBuilder.append(textualLoss);
		stringBuilder.append("\"\"\"");

		stringBuilder.append("Summarize the errors first, update the given example to avoid these errors. "
				+ "In your answer, include the updated example only. Do not include explanations.");
		return stringBuilder.toString();
	}
}
