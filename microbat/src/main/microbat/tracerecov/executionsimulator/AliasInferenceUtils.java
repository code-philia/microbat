package microbat.tracerecov.executionsimulator;

import java.util.List;

import org.json.JSONObject;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;
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

		/* read variables */
		List<VarValue> readVariables = step.getReadVariables();

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

		for (VarValue v : readVariables) {
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
		for (VarValue v : readVariables) {
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
				"Please infer the Java heap address of the given object by filling in &?& with the provided memory addresses only. If you are not sure, please keep &?&\n"
				+ "\n"
				+ "The provided JSON key has format: “var_name:var_type &heap_address&”.\n"
				+ "Your response should be in JSON format, where key is the variable name with format: “var_name1.var_name2.var_name3” and value is the changed heap address with format: \"&...&\".\n"
				+ "\n"
				+ "To get the key in your response, extract the variable names in the provided JSON. Do not include type or address in your keys.\n"
				+ "Do not include explanation in your response.");

		return question.toString();
	}
}
