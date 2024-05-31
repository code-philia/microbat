package microbat.tracerecov.executionsimulator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.VariableGraph;
import microbat.tracerecov.varexpansion.VarSkeletonBuilder;
import microbat.tracerecov.varexpansion.VariableSkeleton;

/**
 * @author hongshuwang
 */
public class LinkageEstimationUtils {

	/* Request content */

	private static final String LINKAGE_REQUEST_BACKGROUND = "In the Java steps provided, the "
			+ "variable values before the execution are given. The structure of the data structures "
			+ "is also given. You need to identify when two variables can be linked at a step. "
			+ "Constants are values that are not assigned to variables. ***Do not include linkage "
			+ "between variable and constant. Do not include linkage across steps.***\n"
			+ "Your response should be in this format:\n" + "step_No#var_x_name==var_y_name\n"
			+ "For example, consider:\n" + "data structures:\n"
			+ "java.util.ArrayList: {Object[] elementData; int size;}\n" + "steps:\n"
			+ "1: “list.add(e);”, variables: {java.util.ArrayList; list: []}, {String; e: “a”}\n"
			+ "Your response for this example should be:\n" + "1#list.elementData[0]==e\n";

	/* Methods */

	public static String getBackgroundContent() {
		return LINKAGE_REQUEST_BACKGROUND;
	}

	public static String getQuestionContent(List<TraceNode> potentialLinkageSteps) {
		StringBuilder question = new StringBuilder("Give your response without explanation:\n");
		StringBuilder dataStructures = new StringBuilder("data structures:\n");
		StringBuilder steps = new StringBuilder("steps:\n");

		Set<String> types = new HashSet<>();

		for (TraceNode step : potentialLinkageSteps) {
			// collect all the data structures
			for (VarValue readVar : step.getReadVariables()) {
				types.add(readVar.getType());
			}
			for (VarValue writtenVar : step.getWrittenVariables()) {
				types.add(writtenVar.getType());
			}

			// convert step to string
			steps.append(getContentForRelevantStep(step));
		}

		for (String type : types) {
			VariableSkeleton var = VarSkeletonBuilder.getVariableStructure(type);
			if (var != null) {
				dataStructures.append(var.toString());
				dataStructures.append("\n");
			}
		}

		question.append(dataStructures);
		question.append(steps);

		return question.toString();
	}
	
	/**
	 * STEP_NO:“CODE”,variables:{VAR1},{VAR2},...
	 */
	private static StringBuilder getContentForRelevantStep(TraceNode node) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(node.getOrder());

		stringBuilder.append(":\"");

		int lineNo = node.getLineNumber();
		String location = node.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils.getSourceCode(location, lineNo).trim();
		stringBuilder.append(sourceCode);

		stringBuilder.append("\",variables:");

		List<VarValue> readVars = node.getReadVariables();
		for (VarValue v : readVars) {
			stringBuilder.append(getContentForVar(v));
		}

		stringBuilder.append("\n");
		return stringBuilder;
	}

	/**
	 * {TYPE; NAME:"VALUE"}
	 */
	private static StringBuilder getContentForVar(VarValue varValue) {
		StringBuilder stringBuilder = new StringBuilder("{");
		stringBuilder.append(varValue.getType());
		stringBuilder.append("; ");
		stringBuilder.append(varValue.getVarName());
		stringBuilder.append(":\"");
		stringBuilder.append(varValue.getStringValue());
		stringBuilder.append("\"}");
		return stringBuilder;
	}

	public static void processResponse(String response, List<TraceNode> potentialLinkageSteps) {
		String[] lines = response.split("\n");
		for (String line : lines) {
			String[] entries = line.split("#");
			if (entries.length != 2) {
				continue;
			}
			int stepNo = Integer.valueOf(entries[0].trim());
			String linkage = entries[1].trim();

			// get variables
			String[] variables = linkage.split("==");
			if (variables.length != 2) {
				continue;
			}
			String var1 = variables[0];
			String var2 = variables[1];

			// find step
			TraceNode step = potentialLinkageSteps.stream().filter(s -> {
				return s.getOrder() == stepNo;
			}).findFirst().orElse(null);
			if (step == null) {
				continue;
			}

			String[] fields1 = var1.split("\\.");
			String[] fields2 = var2.split("\\.");
			String varName1 = fields1[0];
			String varName2 = fields2[0];

			ArrayList<VarValue> stepVars = new ArrayList<>();
			stepVars.addAll(step.getReadVariables());
			stepVars.addAll(step.getWrittenVariables());
			VarValue varValue1 = stepVars.stream().filter(v -> v.getVarName().equals(varName1)).findAny().orElse(null);
			VarValue varValue2 = stepVars.stream().filter(v -> v.getVarName().equals(varName2)).findAny().orElse(null);
			if (varValue1 == null || varValue2 == null) {
				continue;
			}
			VariableGraph.linkVariables(varValue1, varValue2, var1, var2);
		}
	}

}
