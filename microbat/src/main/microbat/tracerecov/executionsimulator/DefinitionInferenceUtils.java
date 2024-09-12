package microbat.tracerecov.executionsimulator;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.DefinitionInferenceExampleSearcher;
import microbat.tracerecov.autoprompt.ExampleSearcher;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;
import microbat.tracerecov.coderetriever.SourceCodeRetriever;

public class DefinitionInferenceUtils {

	/* Request content */

	private static final String DEFINITION_INFERENCE_BACKGROUND = "<Background>\n"
			+ "You are a Java expert, you need to analyze whether a variable is written.";

	/* Methods */

	public static String getBackgroundContent() {
		return DEFINITION_INFERENCE_BACKGROUND;
	}

	public static String getBackgroundContent(VarValue rootVar, VarValue targetVar, List<VarValue> criticalVariables) {
		return DEFINITION_INFERENCE_BACKGROUND + getExample(rootVar, targetVar, criticalVariables);
	}

	private static HashMap<String, String> getDatapointFromStep(VarValue rootVar, VarValue targetVar, List<VarValue> criticalVariables) {

		// TARGET_FIELD
		String targetVarName = targetVar.getVarName();
		
		String cascadeFieldName = "";
		int stopIndex = criticalVariables.size() - 1;
		for (int i = 0; i < stopIndex; i++) {
			VarValue criticalVar = criticalVariables.get(i);
			cascadeFieldName += criticalVar.getVarName() + ".";
		}
		cascadeFieldName += targetVarName;
		
		// TARGET_VAR
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		HashMap<String, String> datapoint = new HashMap<>();
		datapoint.put(DatasetReader.TARGET_FIELD, cascadeFieldName);
		datapoint.put(DatasetReader.VAR_NAME, "");
		datapoint.put(DatasetReader.TARGET_VAR, jsonString);
		datapoint.put(DatasetReader.SOURCE_CODE, "");
		datapoint.put(DatasetReader.INVOKED_METHODS, "");
		datapoint.put(DatasetReader.VARS_IN_STEP, "");
		datapoint.put(DatasetReader.GROUND_TRUTH, ""); // not available yet

		return datapoint;
	}

	private static String getExample(VarValue rootVar, VarValue targetVar, List<VarValue> criticalVariables) {
		HashMap<String, String> datapoint = getDatapointFromStep(rootVar, targetVar, criticalVariables);

		ExampleSearcher exampleSearcher = new DefinitionInferenceExampleSearcher(true);
		String closestExample = exampleSearcher.searchForExample(datapoint);

		// TODO: add default example
		if (closestExample == null || closestExample.equals("")) {
			return "";
		}
		return closestExample;
	}

	public static String getQuestionContent(TraceNode step, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables) {
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils
				.processInputStringForLLM(TraceRecovUtils.getSourceCodeOfALine(location, lineNo).trim());

		/* variable properties */
		String rootVarName = rootVar.getVarName();
		String targetVarName = targetVar.getVarName();

		/* type structure */
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		/* all variables */
		Set<VarValue> variablesInStep = step.getAllVariables();

		/* invoked methods to be checked */
		Set<String> invokedMethods = TraceRecovUtils.getInvokedMethodsToBeChecked(step.getInvokingMethod());

		StringBuilder question = new StringBuilder("<Question>\n" + "Given the code as:\n```");
		question.append(sourceCode);
		question.append("```");

		// invoked methods
		SourceCodeRetriever sourceCodeRetriever = new SourceCodeRetriever();
		if (!invokedMethods.isEmpty()) {
			question.append("\n\nGiven the source code of function calls in the code:\n");
			for (String methodSig : invokedMethods) {
				String methodSourceCode = methodSig;
				if (SourceCodeDatabase.sourceCodeMap.containsKey(methodSig)) {
					methodSourceCode = SourceCodeDatabase.sourceCodeMap.get(methodSig);
				} else {
					methodSourceCode = sourceCodeRetriever.getMethodCode(methodSig,
							step.getTrace().getAppJavaClassPath());
					SourceCodeDatabase.sourceCodeMap.put(methodSig, methodSourceCode);
				}
				question.append(methodSourceCode);
				question.append("\n");
			}
		}

		// variables information (name, type, value)
		question.append("\nVariables involved:");
		for (VarValue var : step.getReadVariables()) {
			question.append("`");
			question.append(var.getVarName());
			question.append("` is of type `");
			question.append(var.getType());
			question.append("`, of runtime value \"");
			question.append(var.getStringValue());
			question.append("\",");
		}

		question.append("\n\nwe know that `" + rootVarName + "` has the following structure and value:\n");
		question.append(jsonString);

		boolean isFirstVar = true;
		for (VarValue var : variablesInStep) {
			VarValue criticalVariable = null;
			if (var.getAliasVarID() != null) {
				criticalVariable = criticalVariables.stream().filter(v -> var.getAliasVarID().equals(v.getAliasVarID()))
						.findFirst().orElse(null);
			}
			if (criticalVariable == null) {
				continue;
			}

			String cascadeFieldName = "";
			int splitIndex = criticalVariable.getVarID().indexOf(".");
			if (splitIndex >= 0) {
				cascadeFieldName = rootVar.getVarName() + criticalVariable.getVarID().substring(splitIndex);
			} else {
				cascadeFieldName = rootVar.getVarName();
			}

			if (!cascadeFieldName.equals(var.getVarName())) {
				question.append(isFirstVar ? "where\n`" : "`");
				question.append(var.getVarName());
				question.append("` has the same memory address as `");
				question.append(cascadeFieldName);
				question.append("`,\n");
				isFirstVar = false;
			}
		}

		question.append("`" + rootVarName + "` has a field called `");

		String cascadeFieldName = "";
		int stopIndex = criticalVariables.size() - 1;
		for (int i = 0; i < stopIndex; i++) {
			VarValue criticalVar = criticalVariables.get(i);
			cascadeFieldName += criticalVar.getVarName() + ".";
		}
		cascadeFieldName += targetVarName;

		question.append(cascadeFieldName);
		question.append("`, does the code directly or indirectly write this field?"
				+ "\nIn your response, return T for true and F for false. Do not include explanation.");

		return question.toString();
	}

	public static boolean isModified(String response) {
		response = response.trim();
		return response.equals("T") ? true : false;
	}

}
