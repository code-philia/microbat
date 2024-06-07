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

/**
 * @author hongshuwang
 */
public class AliasInferenceUtils {

	/* Request content */

	private static final String ALIAS_INFERENCE_BACKGROUND = "<Background>\n"
			+ "You are a Java expert, you need to analyze the runtime execution of a Java program.";

	/* Methods */

	public static String getBackgroundContent() {
		return ALIAS_INFERENCE_BACKGROUND;
	}

	public static String getQuestionContent(TraceNode step, VarValue rootVar, List<VarValue> criticalVariables) {
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils.processInputStringForLLM(TraceRecovUtils.getSourceCode(location, lineNo).trim());

		/* all variables */
		Set<VarValue> variablesInStep = step.getAllVariables();

		/* variable properties */
		String rootVarName = rootVar.getVarName();

		/* type structure */
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		StringBuilder question = new StringBuilder("<Question>\n" + "Given the code as:\n```");
		question.append(sourceCode);
		question.append("```");

		for (VarValue var : variablesInStep) {
			question.append("\n`");
			question.append(var.getVarName());
			question.append("` is of type `");
			question.append(var.getType());
			question.append("`, of runtime value \"");
			question.append(var.getStringValue());
			question.append("\",");
		}

		question.append("\nWe know that `");
		question.append(rootVarName);
		question.append("` has the following structure:\n");
		question.append(jsonString);
		question.append("\n");
		
		boolean isFirstVar = true;
		for (VarValue var : variablesInStep) {
			VarValue criticalVariable = null;
			if (var.getAliasVarID() != null) {
				criticalVariable = criticalVariables.stream().filter(v -> var.getAliasVarID().equals(v.getAliasVarID())).findFirst().orElse(null);
			}
			if (criticalVariable == null) {
				continue;
			}
			int splitIndex = criticalVariable.getVarID().indexOf(".");
			if (splitIndex < 0) {
				continue;
			}
			String cascadeFieldName = rootVar.getVarName() + criticalVariable.getVarID().substring(splitIndex);
			
			question.append(isFirstVar ? "where\n`" : "`");
			question.append(var.getVarName());
			question.append("` refers to `");
			question.append(cascadeFieldName);
			question.append("`,\n");
			isFirstVar = false;
		}
		
		question.append("List all the fields in `" + rootVarName
				+ "` that have the same memory address as other variables at this step. "
				+ "The variable names must be chosen from the above *names*.");

		question.append("\nYour response should be a JSON with this expected format:\r\n" + "{\r\n"
				+ "\"v1.f1\":\"v2\",\r\n" + "\"v1.f1.f2\":\"v3\",\r\n" + "\"v4.f3\":\"v1.f1\"\r\n" + "}\n"
				+ "Both key and value in JSON should be variable or field names."
				+ "If a field is an element in an array, use “array_name[element_index]” as its name."
				+ "\n"
				+ "Field format: \"layer1_var.layer2_var.field\"\n"
				+ "\n"
				+ "In your response, strictly follow this format. Do not include explanation. Do not include duplicate pairs.");

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
			String variableName = (String) inferences.getString(fieldName);

			/* all variables */
			Set<VarValue> variablesInStep = step.getAllVariables();

			/* update memory address in rootVar */
			VarValue variableOnTrace = variablesInStep.stream().filter(v -> v.getVarName().equals(variableName)).findFirst()
					.orElse(null);
			VarValue writtenField = searchForField(fieldName, rootVar);
			if (variableOnTrace == null || writtenField == null) {
				continue;
			}

			fieldsWithAddressRecovered.put(writtenField, variableOnTrace);
		}

		return fieldsWithAddressRecovered;
	}

	private static VarValue searchForField(String fieldName, VarValue rootVar) {
		VarValue field = null;

		String[] fields = fieldName.split("\\.");
		if (fields.length <= 1 && !rootVar.getVarName().equals(fields[0].trim())) {
			// fieldName has <= 1 layers OR rootVar not matched
			return field;
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

//	public static String getQuestionContent(TraceNode step, VarValue rootVar) {
//		/* source code */
//		int lineNo = step.getLineNumber();
//		String location = step.getBreakPoint().getFullJavaFilePath();
//		String sourceCode = TraceRecovUtils.getSourceCode(location, lineNo).trim();
//
//		/* variable of interest properties */
//		String varType = rootVar.getType();
//		String varName = rootVar.getVarName();
//		String aliasID = rootVar.getAliasVarID();
//
//		/* type structure */
//		JSONObject typeStructure = VariableOfInterest.getVariableOfInterestForAliasInferencing(rootVar.getAliasVarID());
//		String jsonString = typeStructure.toString();
//
//		/* variables */
//		List<VarValue> variables = new ArrayList<>();
//		variables.addAll(step.getReadVariables());
//		variables.addAll(step.getWrittenVariables());
//
//		StringBuilder question = new StringBuilder("Given the data structure of ");
//		question.append(varType);
//		question.append(" as the following format where && stands for Java heap address:\n");
//		question.append(jsonString);
//		question.append("\nAssume we have a step executing:`");
//		question.append(sourceCode);
//		question.append("`, where the variable *");
//		question.append(varName);
//		question.append("* share the same memory address of &");
//		question.append(aliasID);
//		question.append("&.\nBefore executing the step `");
//		question.append(sourceCode);
//		question.append("`, ");
//
//		for (VarValue v : variables) {
//			if (v.getVarName() != null && v.getVarName().equals("this")) {
//				continue;
//			}
//			question.append("the value of *");
//			question.append(v.getVarName());
//			question.append("* is \"");
//			question.append(v.getStringValue());
//			question.append("\",");
//		}
//
//		question.append("\nWe know that ");
//		for (VarValue v : variables) {
//			if (v.getVarName() != null && v.getVarName().equals("this")) {
//				continue;
//			}
//			question.append("the address of *");
//			question.append(v.getVarName());
//			question.append("* is &");
//			question.append(v.getAliasVarID());
//			question.append("&,");
//		}
//
//		question.append(
//				"Please infer the Java heap address of the given object by filling in &?& with the *provided memory addresses only*. Do not make up address. If you are not sure, please keep &?&\n"
//						+ "\n" + "The provided JSON key has format: “var_name:var_type &heap_address&”.\n"
//						+ "Your response should be in JSON format, where key is the variable name with format: “var_name1.var_name2.var_name3” and value is the changed heap address with format: \"&...&\".\n"
//						+ "\n"
//						+ "To get the key in your response, extract the variable names in the provided JSON. Do not include type or address in your keys.");
//
//		return question.toString();
//	}
//
//	
//	/**
//	 * alias inference 
//	 * @param response
//	 * @param rootVar
//	 * @param step
//	 */
//	public static void processResponse(String response, VarValue rootVar, TraceNode step) {
//		int begin = response.indexOf("{");
//		int end = response.lastIndexOf("}");
//		response = response.substring(begin, end + 1);
//
//		JSONObject jsonObject = new JSONObject(response);
//
//		Iterator<String> keys = jsonObject.keys();
//		while (keys.hasNext()) {
//			String key = keys.next();
//			String address = jsonObject.get(key).toString();
//			if (address == null || address.contains("?") || !address.contains("&")) {
//				continue;
//			}
//			begin = address.indexOf("&");
//			end = address.lastIndexOf("&");
//			address = address.substring(begin + 1, end);
//			
//			// get valid addresses
//			List<VarValue> variables = new ArrayList<>();
//			variables.addAll(step.getReadVariables());
//			variables.addAll(step.getWrittenVariables());
//			List<String> validAddresses = variables.stream().map(v -> v.getAliasVarID()).toList();
//			
//			//TODO change here
////			if (!VariableGraph.containsVar(address) && !validAddresses.contains(address)) {
////				// invalid address
////				continue;
////			}
//
//			// search for variable
//			String[] fields = key.split("\\.");
//			if (!fields[0].trim().equals(rootVar.getVarName())) {
//				continue;
//			}
//			VarValue innerVar = rootVar;
//			for (int i = 1; i < fields.length; i++) {
//				List<VarValue> children = innerVar.getChildren();
//				boolean childIsFound = false;
//				for (VarValue child : children) {
//					if (child.getVarName().equals(fields[i])) {
//						innerVar = child;
//						childIsFound = true;
//						break;
//					}
//				}
//				if (!childIsFound) {
//					break;
//				}
//			}
//
//			// update address
//			if (innerVar != null) {
//				innerVar.setAliasVarID(address);
//				//TODO change here
////				VariableGraph.addVar(innerVar);
//			}
//		}
//	}
}
