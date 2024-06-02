package microbat.tracerecov.executionsimulator;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.VariableGraph;
import microbat.tracerecov.varexpansion.VariableSkeleton;

public class VariableExpansionUtils {
	/* Request content */

	private static final String VAR_EXPAND_BACKGROUND = "<Background>\n"
			+ "When executing a Java third-party library, some of its internal variables are critical for debugging. Please identify the most critical internal variables of a Java data structure for debugging. \n"
			+ "\n" + "<Example>\n" + "Class Name: java.util.HashMap<HashMap, ArrayList>\n" + "Structure: {\n"
			+ "	java.util.HashMap$Node[] table;\n" + "	java.util.Set entrySet;\n" + "	int size;\n"
			+ "	int modCount;\n" + "	int threshold;\n" + "	float loadFactor;\n" + "	java.util.Set keySet;\n"
			+ "	java.util.Collection values;\n" + "}\n" + "\n" + "Given “map.toString()” has output value:\n"
			+ "{{k1=1, k2=2} = [v1, v2], {kA=100, kB=200} = [vA, vB]}\n" + "\n" + "We can summarize the structure as \n"
			+ "Here is the given structure converted to JSON format (every variable shall strictly have a Java type, followed by its value):\n"
			+ "{\n" + "  \"map: java.util.HashMap<HashMap, ArrayList>\": {\n" + "    \"key[1]: HashMap\": {\n"
			+ "      {\n" + "        \"key[1]: java.lang.String\": \"k1\",\n"
			+ "        \"key[2]: java.lang.String\": \"k2\",\n" + "        \"value[1]: java.lang.Integer\": 1,\n"
			+ "        \"value[2]: java.lang.Integer\": 2,\n" + "        \"size: int\": 2	\n" + "      }\n"
			+ "    },\n" + "    \"key[2]: java.util.HashMap\": {\n" + "      \"map\": {\n"
			+ "        \"key[1]: java.lang.String\": \"kA\",\n" + "        \"key[2]: java.lang.String\": \"kB\",\n"
			+ "        \"value[1]: java.lang.Integer\": 100,\n" + "        \"value[2]: java.lang.Integer\": 200,\n"
			+ "         \"size: int\": 2\n" + "      }\n" + "    }\n" + "  },\n"
			+ "  \"value[1]: java.util.ArrayList<String>\": [\"v1\", \"v2\"],\n"
			+ "  \"value[2]: java.util.ArrayList<String>\": [\"vA\", \"vB\"],\n" + "  \"size\": 2\n" + "}\n"
			+ "<Question>";

	/* Methods */

	public static String getBackgroundContent() {
		return VAR_EXPAND_BACKGROUND;
	}

	public static String getQuestionContent(VarValue selectedVariable, List<VariableSkeleton> variableSkeletons,
			TraceNode step) {
		StringBuilder question = new StringBuilder("Given the following data structure:\n");

		for (VariableSkeleton v : variableSkeletons) {
			question.append(v.toString());
		}

		question.append("with the input value of executing \"");
		
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils.getSourceCode(location, lineNo).trim();
		question.append(sourceCode);
		question.append("we have the value of *" + selectedVariable.getVarName() + "* of type ");
		question.append(selectedVariable.getType());
		
		question.append(": \"");
		question.append(selectedVariable.getStringValue());
		question.append("\"");
		
		question.append(", strictly show me the JSON format of *" + selectedVariable.getVarName()
				+ "* as the above example.\n" + "No explanation is needed, just return the result.\n");

		return question.toString();
	}

	public static void processResponse(String response) {
		new JSONObject(response);
	}
}
