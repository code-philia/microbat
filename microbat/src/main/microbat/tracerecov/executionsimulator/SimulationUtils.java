package microbat.tracerecov.executionsimulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;

/**
 * This class contains constants and methods used in execution simulation.
 * 
 * @author hongshuwang
 */
public class SimulationUtils {

	/* ChatGPT API */
	public static final String API_URL = "https://api.openai.com/v1/chat/completions";
	public static final String GPT3 = "gpt-3.5-turbo";
	public static final String GPT4 = "gpt-4-turbo";
	public static final String GPT4O = "gpt-4o";

	/* Model constants */
	public static final double TEMPERATURE = 0.7;

	/* Request content */
	private static final String REQUEST_BACKGROUND = "In each step, the variable of interest (VOI) is "
			+ "identified. The variable values ***before*** execution are given, you need to provide the value "
			+ "of the VOI ***after*** the execution of each step. Don't return the value of variables other "
			+ "than the VOI.\n"
			+ "If the VOI is not modified ***before and after*** the execution, you shouldn't include this "
			+ "step in your response.";

	private static final String QUESTION_SUFFIX = "Return the value of the VOI ***after*** the execution. Your "
			+ "response should be in this format without explanation: <step_No>#<VOI_value>";

	/* Methods */

	public static String getBackgroundContent() {
		return REQUEST_BACKGROUND;
	}

	public static String getQuestionContent(String variableID, List<TraceNode> relevantSteps) {
		StringBuilder stringBuilder = new StringBuilder();

		VarValue criticalVar = relevantSteps.get(0).getReadVariables().stream()
				.filter(v -> Variable.truncateSimpleID(v.getAliasVarID()).equals(variableID)).findFirst().orElse(null);
		String type = criticalVar.getType();
		stringBuilder.append(getContentForVariableOfInterest(type));

		stringBuilder.append("In the following steps: \n");
		for (TraceNode step : relevantSteps) {
			criticalVar = step.getReadVariables().stream()
					.filter(v -> Variable.truncateSimpleID(v.getAliasVarID()).equals(variableID)).findFirst()
					.orElse(null);
			stringBuilder.append(getContentForRelevantStep(step, criticalVar));
		}

		stringBuilder.append(QUESTION_SUFFIX);

		return stringBuilder.toString();
	}

	/**
	 * The VOI in each of the following steps has type "TYPE".
	 */
	private static StringBuilder getContentForVariableOfInterest(String type) {
		StringBuilder stringBuilder = new StringBuilder("The VOI in each of the following steps has type \"");
		stringBuilder.append(type);
		stringBuilder.append("\".\n");
		return stringBuilder;
	}

	/**
	 * step_No:STEP_NO, source_code:"CODE", VOI:{name:"NAME", type:"TYPE",
	 * value:"VAL"}, other_variables:{name:"NAME", type:"TYPE", value:"VAL"}
	 * {name:"NAME", type:"TYPE", value:"VAL"}...
	 */
	public static StringBuilder getContentForRelevantStep(TraceNode node, VarValue varValue) {
		StringBuilder stringBuilder = new StringBuilder("step_No:");
		stringBuilder.append(node.getOrder());

		stringBuilder.append(",source_code:\"");
		int lineNo = node.getLineNumber();
		String location = node.getBreakPoint().getFullJavaFilePath();
		String sourceCode = getSourceCode(location, lineNo).trim();
		stringBuilder.append(sourceCode);

		stringBuilder.append("\",VOI:");
		stringBuilder.append(getContentForVar(varValue));

		List<VarValue> readVars = node.getReadVariables();
		if (readVars.size() <= 1) {
			stringBuilder.append("\n");
			return stringBuilder;
		}

		stringBuilder.append(",other_variables:");
		for (VarValue v : readVars) {
			if (v.getVarName().equals(varValue.getVarName())) {
				continue;
			}
			stringBuilder.append(getContentForVar(v));
		}

		stringBuilder.append("\n");
		return stringBuilder;
	}

	private static StringBuilder getContentForVar(VarValue varValue) {
		StringBuilder stringBuilder = new StringBuilder("{name:\"");
		stringBuilder.append(varValue.getVarName());
		stringBuilder.append("\",type:\"");
		stringBuilder.append(varValue.getType());
		stringBuilder.append("\",value:\"");
		stringBuilder.append(varValue.getStringValue());
		stringBuilder.append("\"}");
		return stringBuilder;
	}

	private static String getSourceCode(String filePath, int lineNumber) {
		String line = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			int currentLine = 0;
			while ((line = reader.readLine()) != null) {
				currentLine++;
				if (currentLine == lineNumber) {
					return line;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void processResponse(String response, String variableID, List<TraceNode> relevantSteps) {
		String[] lines = response.split("\n");
		for (String line : lines) {
			String[] entries = line.split("#");
			if (entries.length != 2) {
				continue;
			}
			int stepNo = Integer.valueOf(entries[0].trim());
			String value = entries[1].trim();

			TraceNode step = relevantSteps.stream().filter(s -> {
				return s.getOrder() == stepNo;
			}).findFirst().orElse(null);
			if (step == null) {
				continue;
			}

			VarValue writtenVar = step.getWrittenVariables().stream().filter(v -> v.getAliasVarID().equals(variableID))
					.findFirst().orElse(null);
			if (writtenVar != null) {
				continue;
			}

			VarValue readVar = step.getReadVariables().stream().filter(v -> v.getAliasVarID().equals(variableID))
					.findFirst().orElse(null);
			if (readVar == null) {
				continue;
			}

			writtenVar = readVar.clone();
			writtenVar.setStringValue(value);
			step.addWrittenVariable(writtenVar);
		}
	}
}
