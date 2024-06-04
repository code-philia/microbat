package microbat.tracerecov.executionsimulator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONObject;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.VariableOfInterest;

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

	public static String getQuestionContent(TraceNode step, VarValue rootVar) {
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils.getSourceCode(location, lineNo).trim();

		/* all variables */
		Set<VarValue> variables = new HashSet<>();
		variables.addAll(step.getReadVariables());
		variables.addAll(step.getWrittenVariables());

		/* variable properties */
		String rootVarName = rootVar.getVarName();

		/* type structure */
		JSONObject typeStructure = VariableOfInterest.getVariableOfInterestForDefinitionInferencing();
		String jsonString = typeStructure.toString();

		StringBuilder question = new StringBuilder("<Question>\n" + "Given the code as:\n```");
		question.append(sourceCode);
		question.append("```\n");

		for (VarValue var : variables) {
			question.append("`");
			question.append(var.getVarName());
			question.append("` is of type `");
			question.append(var.getType());
			question.append("`, of runtime value \"");
			question.append(var.getStringValue());
			question.append("\",");
		}

		question.append("We know that `");
		question.append(rootVarName);
		question.append("` has the following structure:\n");
		question.append(jsonString);
		question.append("\n");
		question.append("List all the fields that have the same memory address as variables other than`");
		question.append(rootVarName);
		question.append("`.\nIf a field is an element in an array, use “array_name[element_index]” as its name.\n"
				+ "Your response should be a JSON with field_name as keys and variable_name as values, where "
				+ "field_name has format: v1.v2.v3, and field_name must include all the parent fields.\nDo not include explanation.");

		return question.toString();
	}

	public static Set<VarValue> processResponse(String response, VarValue rootVar, TraceNode step) {
		Set<VarValue> fieldsWithAddressRecovered = new HashSet<>();

		int begin = response.indexOf("{");
		int end = response.lastIndexOf("}");
		response = response.substring(begin, end + 1);

		JSONObject inferences = new JSONObject(response);

		Iterator<String> keys = inferences.keys();
		while (keys.hasNext()) {
			String fieldName = keys.next();
			String variableName = (String) inferences.getString(fieldName);

			/* all variables */
			Set<VarValue> variables = new HashSet<>();
			variables.addAll(step.getReadVariables());
			variables.addAll(step.getWrittenVariables());

			/* update memory address in rootVar */
			VarValue variableOnTrace = variables.stream().filter(v -> v.getVarName().equals(variableName)).findFirst()
					.orElse(null);
			VarValue candidateVariable = searchForField(fieldName, rootVar);
			if (variableOnTrace == null || candidateVariable == null) {
				continue;
			}
			// TODO: if candidate variable exists, remove it from rootVar
			VarValue child = variableOnTrace.clone();
			child.setParents(new ArrayList<>());
			child.clearValue();
			rootVar.linkAchild(child);

			fieldsWithAddressRecovered.add(child);
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

		String fName = fieldName.substring(fieldName.indexOf("."));
		field = rootVar.getAllDescedentChildren().stream().filter(child -> {
			String childID = child.getVarID();
			String cascadeFieldName = childID.substring(childID.indexOf("."));
			return cascadeFieldName.contains(fName);
		}).findFirst().orElse(null);

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
