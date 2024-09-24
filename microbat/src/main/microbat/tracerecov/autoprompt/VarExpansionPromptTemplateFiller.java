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
			+ "  \"map| java.util.HashMap<HashMap, ArrayList>\": {\r\n"
			+ "    \"key[0]| java.util.HashMap\": {\r\n"
			+ "      \"map\":{\r\n"
			+ "        \"key[0]| java.lang.String\": \"k1\",\r\n"
			+ "        \"key[1]| java.lang.String\": \"k2\",\r\n"
			+ "        \"value[0]| java.lang.Integer\": 1,\r\n"
			+ "        \"value[1]| java.lang.Integer\": 2,\r\n"
			+ "        \"size| int\": 2	\r\n"
			+ "       }\r\n"
			+ "    },\r\n"
			+ "    \"key[1]| java.util.HashMap\": {\r\n"
			+ "      \"map\": {\r\n"
			+ "        \"key[0]| java.lang.String\": \"kA\",\r\n"
			+ "        \"key[1]| java.lang.String\": \"kB\",\r\n"
			+ "        \"value[0]| java.lang.Integer\": 100,\r\n"
			+ "        \"value[1]| java.lang.Integer\": 200,\r\n"
			+ "         \"size| int\": 2\r\n"
			+ "       }\r\n"
			+ "     },\r\n"
			+ "     \"value[0]| java.util.ArrayList<String>\": { \r\n"
			+ "			\"elementData| java.lang.Object[]\": [ \"v1\", \"v2\"], \r\n"
			+ "  		\"size| int\": 2 \r\n"
			+ "       }, \r\n"
			+ "     \"value[1]| java.util.ArrayList<String>\": { \r\n"
			+ "			\"elementData| java.lang.Object[]\": [ \"vA\", \"vB\"], \r\n"
			+ "  		\"size| int\": 2 \r\n"
			+ "       },\r\n"
			+ "     \"size| int\": 2\r\n"
			+ "  },\r\n"
			+ " }";

	@Override
	public String getDefaultPromptExample() {
		return variableExpansionPromptExample;
	}

	// TODO: update later (not used for now)
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

	// TODO: update later (not used for now)
	@Override
	public String getPrompt(HashMap<String, String> datapoint, String example) {
		return variableExpansionPromptBackground + example + getPromptQuestion(datapoint);
	}

	// TODO: update later (not used for now)
	@Override
	public String getDefaultPrompt(HashMap<String, String> datapoint) {
		return variableExpansionPromptBackground + variableExpansionPromptExample + getPromptQuestion(datapoint);
	}

	@Override
	public String getExample(HashMap<String, String> datapoint, String groundTruth) {
		String varType = datapoint.get(DatasetReader.VAR_TYPE);
		String varValue = TraceRecovUtils.processInputStringForLLM(datapoint.get(DatasetReader.VAR_VALUE));
		String classStructure = datapoint.get(DatasetReader.CLASS_STRUCTURE);

		StringBuilder stringBuilder = new StringBuilder("<Example>\r\n");

		stringBuilder.append("Class Name: " + varType);
		stringBuilder.append("\nStructure: " + classStructure);
		stringBuilder.append("\nVariable Value: " + varValue);

		stringBuilder.append("\nWe can summarize the structure as:\n```json\n" + groundTruth + "\n```");

		return stringBuilder.toString();
	}

}
