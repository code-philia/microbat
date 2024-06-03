package microbat.tracerecov.executionsimulator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.VariableGraph;
import microbat.tracerecov.VariableOfInterest;

/**
 * @author hongshuwang
 */
public class AliasInferenceUtils {

	public static String getQuestionContent(TraceNode step, VarValue rootVar) {
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils.getSourceCode(location, lineNo).trim();

		/* variable of interest properties */
		String varType = rootVar.getType();
		String varName = rootVar.getVarName();
		String aliasID = rootVar.getAliasVarID();

		/* type structure */
		JSONObject typeStructure = VariableOfInterest.getVariableOfInterestForAliasInferencing(rootVar.getAliasVarID());
		String jsonString = typeStructure.toString();

		/* variables */
		List<VarValue> variables = new ArrayList<>();
		variables.addAll(step.getReadVariables());
		variables.addAll(step.getWrittenVariables());

		StringBuilder question = new StringBuilder("Given the data structure of ");
		question.append(varType);
		question.append(" as the following format where && stands for Java heap address:\n");
		question.append(jsonString);
		question.append("\nAssume we have a step executing:`");
		question.append(sourceCode);
		question.append("`, where the variable *");
		question.append(varName);
		question.append("* share the same memory address of &");
		question.append(aliasID);
		question.append("&.\nBefore executing the step `");
		question.append(sourceCode);
		question.append("`, ");

		for (VarValue v : variables) {
			if (v.getVarName() != null && v.getVarName().equals("this")) {
				continue;
			}
			question.append("the value of *");
			question.append(v.getVarName());
			question.append("* is \"");
			question.append(v.getStringValue());
			question.append("\",");
		}

		question.append("\nWe know that ");
		for (VarValue v : variables) {
			if (v.getVarName() != null && v.getVarName().equals("this")) {
				continue;
			}
			question.append("the address of *");
			question.append(v.getVarName());
			question.append("* is &");
			question.append(v.getAliasVarID());
			question.append("&,");
		}

		question.append(
				"Please infer the Java heap address of the given object by filling in &?& with the *provided memory addresses only*. Do not make up address. If you are not sure, please keep &?&\n"
						+ "\n" + "The provided JSON key has format: “var_name:var_type &heap_address&”.\n"
						+ "Your response should be in JSON format, where key is the variable name with format: “var_name1.var_name2.var_name3” and value is the changed heap address with format: \"&...&\".\n"
						+ "\n"
						+ "To get the key in your response, extract the variable names in the provided JSON. Do not include type or address in your keys.");

		return question.toString();
	}

	public static void processResponse(String response, VarValue rootVar, TraceNode step) {
		int begin = response.indexOf("{");
		int end = response.lastIndexOf("}");
		response = response.substring(begin, end + 1);

		JSONObject jsonObject = new JSONObject(response);

		Iterator<String> keys = jsonObject.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			String address = jsonObject.get(key).toString();
			if (address == null || address.contains("?") || !address.contains("&")) {
				continue;
			}
			begin = address.indexOf("&");
			end = address.lastIndexOf("&");
			address = address.substring(begin + 1, end);
			
			// get valid addresses
			List<VarValue> variables = new ArrayList<>();
			variables.addAll(step.getReadVariables());
			variables.addAll(step.getWrittenVariables());
			List<String> validAddresses = variables.stream().map(v -> v.getAliasVarID()).toList();
			
			if (!VariableGraph.containsVar(address) && !validAddresses.contains(address)) {
				// invalid address
				continue;
			}

			// search for variable
			String[] fields = key.split("\\.");
			if (!fields[0].trim().equals(rootVar.getVarName())) {
				continue;
			}
			VarValue innerVar = rootVar;
			for (int i = 1; i < fields.length; i++) {
				List<VarValue> children = innerVar.getChildren();
				boolean childIsFound = false;
				for (VarValue child : children) {
					if (child.getVarName().equals(fields[i])) {
						innerVar = child;
						childIsFound = true;
						break;
					}
				}
				if (!childIsFound) {
					break;
				}
			}

			// update address
			if (innerVar != null) {
				innerVar.setAliasVarID(address);
				VariableGraph.addVar(innerVar);
			}
		}
	}
}
