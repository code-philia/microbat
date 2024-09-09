package microbat.tracerecov.executionsimulator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.coderetriever.SourceCodeRetriever;
import microbat.tracerecov.varskeleton.VarSkeletonBuilder;
import microbat.tracerecov.varskeleton.VariableSkeleton;
import sav.strategies.dto.AppJavaClassPath;

/**
 * @author hongshuwang
 */
public class AliasInferenceUtils {

	/* Request content */

	private static final String ALIAS_INFERENCE_BACKGROUND = "<Background>\n"
			+ "You are a Java expert, you need to analyze the alias relationships through static analysis.\n"
			+ "\n"
			+ "<Example>\n"
			+ "Given code:\n"
			+ "```list.add(element);```\n"
			+ "\n"
			+ "Given the source code of function calls in the code:\n"
			+ "public boolean add(E e) {\n"
			+ "        modCount++;\n"
			+ "        add(e, elementData, size);\n"
			+ "        return true;\n"
			+ "    }\n"
			+ "\n"
			+ "Variables involved:\n"
			+ "`list` is of type `java.util.ArrayList`,\n"
			+ "`element` is of type `Integer`,\n"
			+ "\n"
			+ "We know that another variable not in the code, `list`, with the following structure:\n"
			+ "{\"list:java.util.ArrayList\":{\"elementData:java.lang.Object[]\":\"[]\",\"size:int\":\"0\"}}\n"
			+ "\n"
			+ "Your response should be:\n"
			+ "{\n"
			+ "\"list.elementData[0]\":\"element\"\n"
			+ "}\n\n";

	/* Methods */

	public static String getBackgroundContent() {
		return ALIAS_INFERENCE_BACKGROUND;
	}

	public static String getQuestionContent(TraceNode step, VarValue rootVar, List<VarValue> criticalVariables) {
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils
				.processInputStringForLLM(TraceRecovUtils.getSourceCodeOfALine(location, lineNo).trim());

		/* all variables */
		Set<VarValue> variablesInStep = step.getAllVariables();

		/* variable properties */
		String rootVarName = rootVar.getVarName();

		/* type structure */
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		/* invoked methods to be checked */
		Set<String> invokedMethods = TraceRecovUtils.getInvokedMethodsToBeChecked(step.getInvokingMethod());

		// source code
		StringBuilder question = new StringBuilder("<Question>\n" + "Given code:\n```");
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
		for (VarValue var : variablesInStep) {
			question.append("\n`");
			question.append(var.getVarName());
			question.append("` is of type `");
			question.append(var.getType());
			question.append("`,");
		}

		// fields in variables
		for (VarValue var : variablesInStep) {
			if (var.equals(rootVar)) {
				continue;
			}
			VariableSkeleton varSkeleton = VarSkeletonBuilder.getVariableStructure(var.getType(),
					step.getTrace().getAppJavaClassPath());
			if (varSkeleton == null) {
				continue;
			}
			question.append("\n\n`");
			question.append(var.getVarName());
			question.append("` has the following fields: ");
			question.append(varSkeleton.fieldsToString());
			question.append(",");
		}
		question.append(
				"\n\nIf a variable has name of format `<TYPE>_instance`, it refers to the instance created by calling the constructor of `<TYPE>`.");

		// target variable structure
		question.append("\n\nWe know that another variable not in the code, `");
		question.append(rootVarName);
		question.append("`, with the following structure:\n");
		question.append(jsonString);
		question.append("\n");

		// existing alias relations
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

			question.append(isFirstVar ? "where\n`" : "`");
			question.append(cascadeFieldName);
			question.append("` has the same memory address as `");
			question.append(var.getVarName());
			question.append("`,\n");
			isFirstVar = false;
		}

		// keys (critical variables)
		question.append("\nWe are interested in the fields ");
		String cascadeName = "";
		for (VarValue criticalVar : criticalVariables) {
			question.append("`" + cascadeName + criticalVar.getVarName() + "`,");
			cascadeName += criticalVar.getVarName() + ".";
		}

		question.append("\n\nPerform static analysis. From the given code, identify all the aliases of `" + rootVarName
				+ "` and the fields in `" + rootVarName + "`.");

		question.append("\n\nIn your response, strictly follow the JSON format. Do not include explanation.");

		return question.toString();
	}

	/**
	 * Return a map with key: written_field, value: variable_on_trace
	 */
	public static Map<VarValue, VarValue> processResponse(String response, VarValue rootVar, TraceNode step) {
		Map<VarValue, VarValue> fieldsWithAddressRecovered = new HashMap<>();

		int begin = response.indexOf("{");
		int end = response.lastIndexOf("}");
		response = response.substring(begin, end + 1);

		JSONObject inferences = new JSONObject(response);

		Iterator<String> keys = inferences.keys();
		while (keys.hasNext()) {
			String fieldName = keys.next();
			if (!(inferences.get(fieldName) instanceof String)) {
				continue;
			}

			String variableName = (String) inferences.getString(fieldName);

			/* all variables */
			Set<VarValue> variablesInStep = step.getAllVariables();

			/* update memory address in rootVar */
			VarValue writtenField = searchForField(fieldName, rootVar);

			VarValue variableOnTrace = variablesInStep.stream().filter(v -> v.getVarName().equals(variableName))
					.findFirst().orElse(null);
			if (variableOnTrace == null) {
				String rootVariableOnTrace = "";
				if (variableName.contains(".")) {
					String[] fieldNames = variableName.split("\\.");
					if (fieldNames.length > 1) {
						rootVariableOnTrace = fieldNames[0];
					} else {
						rootVariableOnTrace = variableName;
					}
				} else {
					rootVariableOnTrace = variableName;
				}
				variableOnTrace = searchForVariableOnTrace(variablesInStep, rootVariableOnTrace);
			}

			if (variableOnTrace != null) {
				VarValue fieldInVarOnTrace = searchForField(variableName, variableOnTrace);
				if (fieldInVarOnTrace == null) {
					// field is not recorded
					if (!variableOnTrace.getVarName().equals("this")) {
						fieldsWithAddressRecovered.put(variableOnTrace, variableOnTrace);
					}
					continue;
				} else {
					variableOnTrace = fieldInVarOnTrace;
				}
			}

			if (variableOnTrace == null || writtenField == null) {
				continue;
			}

			AppJavaClassPath appJavaClassPath = step.getTrace().getAppJavaClassPath();
			if (!variableOnTrace.getVarName().equals("this")
					&& TraceRecovUtils.isAssignable(writtenField, variableOnTrace, appJavaClassPath)) {
				fieldsWithAddressRecovered.put(writtenField, variableOnTrace);
			}
		}

		return fieldsWithAddressRecovered;
	}

	private static VarValue searchForVariableOnTrace(Set<VarValue> variablesInStep, String rootVariableName) {
		return variablesInStep.stream().filter(v -> v.getVarName().equals(rootVariableName)).findFirst().orElse(null);
	}

	private static VarValue searchForField(String fieldName, VarValue rootVar) {
		VarValue field = rootVar;

		String[] fields = fieldName.split("\\.");
		if (fields.length <= 1 || !rootVar.getVarName().equals(fields[0].trim())) {
			// fieldName has <= 1 layers OR rootVar not matched
			return field;
		}
		
		int lastIndex = fields.length - 1;
		String lastField = fields[lastIndex];
		if (lastField.contains("[") && lastField.contains("]")) {
			// is array element
			String arrayName = lastField.substring(0, lastField.indexOf("["));
			if (lastIndex != 0 && !fields[lastIndex - 1].equals(arrayName)) {
				// add array name
				StringBuilder nameBuilder = new StringBuilder();
				for (int i = 0; i < lastIndex; i++) {
					nameBuilder.append(fields[i]);
					nameBuilder.append(".");
				}
				nameBuilder.append(arrayName);
				nameBuilder.append(".");
				nameBuilder.append(lastField);
				return searchForField(nameBuilder.toString(), rootVar);
			}
		}
		
		int splitIndex = fieldName.indexOf(".");
		if (splitIndex >= 0) {
			String fName = fieldName.substring(fieldName.indexOf("."));
			field = rootVar.getAllDescedentChildren().stream().filter(child -> {
				String childID = child.getVarID();
				String cascadeFieldName = childID.substring(childID.indexOf("."));
				return cascadeFieldName.endsWith(fName);
			}).findFirst().orElse(null);
		}

		return field;
	}
}
