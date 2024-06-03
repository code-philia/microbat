package microbat.tracerecov.executionsimulator;

import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import microbat.model.trace.TraceNode;
import microbat.model.value.ArrayValue;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.StringValue;
import microbat.model.value.VarValue;
import microbat.model.variable.FieldVar;
import microbat.model.variable.Variable;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.varexpansion.VariableSkeleton;

public class VariableExpansionUtils {
	/* Request content */

	private static final String VAR_EXPAND_BACKGROUND = "<Background>\r\n"
			+ "When executing a Java third-party library, some of its internal variables are critical for debugging. Please identify the most critical internal variables of a Java data structure for debugging. \r\n"
			+ "\r\n"
			+ "<Example>\r\n"
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
			+ "  \"map: java.util.HashMap<HashMap, ArrayList>\": {\r\n"
			+ "    \"key[1]: HashMap\": {\r\n"
			+ "      {\r\n"
			+ "        \"key[1]: java.lang.String\": \"k1\",\r\n"
			+ "        \"key[2]: java.lang.String\": \"k2\",\r\n"
			+ "        \"value[1]: java.lang.Integer\": 1,\r\n"
			+ "        \"value[2]: java.lang.Integer\": 2,\r\n"
			+ "        \"size: int\": 2	\r\n"
			+ "      }\r\n"
			+ "    },\r\n"
			+ "    \"key[2]: java.util.HashMap\": {\r\n"
			+ "      \"map\": {\r\n"
			+ "        \"key[1]: java.lang.String\": \"kA\",\r\n"
			+ "        \"key[2]: java.lang.String\": \"kB\",\r\n"
			+ "        \"value[1]: java.lang.Integer\": 100,\r\n"
			+ "        \"value[2]: java.lang.Integer\": 200,\r\n"
			+ "         \"size: int\": 2\r\n"
			+ "       }\r\n"
			+ "     },\r\n"
			+ "     \"value[1]: java.util.ArrayList<String>\": [\"v1\", \"v2\"],\r\n"
			+ "     \"value[2]: java.util.ArrayList<String>\": [\"vA\", \"vB\"],\r\n"
			+ "     \"size\": 2\r\n"
			+ "  },\r\n"
			+ " }\r\n"
			+ "\r\n";

	/* Methods */

	public static String getBackgroundContent() {
		return VAR_EXPAND_BACKGROUND;
	}

	public static String getQuestionContent(VarValue selectedVariable, List<VariableSkeleton> variableSkeletons,
			TraceNode step) {
		StringBuilder question = new StringBuilder("<Question>\n"
				+ "Given the following data structure:\n");

		for (VariableSkeleton v : variableSkeletons) {
			question.append(v.toString() + " ");
		}
		
		//TODO to remove it if we have runtime variable type
//		if(selectedVariable.getType().equals("java.util.List")) {
//			question.append("java.util.LinkedList:{"
//					+ "int size;"
//					+ "java.util.LinkedList$Node first;"
//					+ "java.util.LinkedList$Node last;"
//					+ "int modCount;"
//					+ "}");
//		}

		question.append("with the input value of executing \"");
		
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils.getSourceCode(location, lineNo).trim();
		question.append(sourceCode + "\", ");
		question.append("we have the value of *" + selectedVariable.getVarName() + "* of type ");
		
		
		//TODO to remove it if we have runtime variable type
		String type = selectedVariable.getType();
		if(type.equals("java.util.List")) {
			type = "java.util.LinkedList";
		}
		
		question.append(type);
		
		question.append(": \"");
		question.append(selectedVariable.getStringValue());
		question.append("\"");
		
		question.append(", strictly return in JSON format for *" + selectedVariable.getVarName()
				+ "* as the above example, each key must has a value. The JSON object must start with variable *" + selectedVariable.getVarName() + "* as the root. Do not include explanation in your response.\n");

		return question.toString();
	}

	/**
	 * Recursively parse JSON into the input VarValue.
	 */
	public static void processResponse(VarValue selectedVariable, String response) {
		int begin = response.indexOf("{");
		int end = response.lastIndexOf("}");
		response = response.substring(begin, end + 1);
		
		JSONObject variable = new JSONObject(response);
		processResponseRecur(true, variable, selectedVariable);
		
	}
	
	private static void processResponseRecur(boolean isRoot, JSONObject jsonObject, VarValue selectedVariable) {
		Iterator<String> keys = jsonObject.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			
			if(isRoot) {
				Object value = jsonObject.get(key);
				if (value instanceof JSONObject) {
					processResponseRecur(false, (JSONObject) value, selectedVariable);
				} else if (value instanceof JSONArray) {
					processResponseRecur((JSONArray) value, selectedVariable);
				}
				break;
			}
			else {
				String varName = key.split(":")[0].trim();
				String varType = key.split(":")[1].trim();
				
				Variable var = new FieldVar(false, varName, varType, varType);
				var.setVarID(Variable.concanateFieldVarID(selectedVariable.getVarID(), varName));
				
				Object value = jsonObject.get(key);
				VarValue varValue = null;
				
				if (value instanceof JSONArray) {
					varValue = new ArrayValue(false, false, var);
					varValue.setStringValue(String.valueOf(value));
					
					processResponseRecur((JSONArray) value, varValue);
				}
				else if(value instanceof JSONObject) {
					varValue = new ReferenceValue(false, false, var);
					varValue.setStringValue(String.valueOf(value));
					
					processResponseRecur(false, (JSONObject)value, varValue);
				}
				else if(value instanceof String){
					varValue = new StringValue(String.valueOf(value), false, var);
				}
				else if(value instanceof Integer){
					varValue = new PrimitiveValue(String.valueOf(value), false, var);
				}
				else if (value == null) {
					varValue = new ReferenceValue(true, false, var);
				}
				
				if(varValue != null) {
					selectedVariable.addChild(varValue);
					varValue.addParent(selectedVariable);					
				}
			}
		}
	}
	
	private static void processResponseRecur(JSONArray jsonArray, VarValue selectedVariable) {
		int index = 0;
		List<Object> values = jsonArray.toList();
		
		for (Object value : values) {
			String varName = selectedVariable.getVarName().concat("[" + index + "]");
			String varType = "";
			String varID = Variable.concanateFieldVarID(selectedVariable.getVarID(), varName);
			
			Variable var = new FieldVar(false, varName, varType, varType);
			var.setVarID(varID);
			
			VarValue varValue = null;
			
			if (value instanceof JSONArray) {
				varValue = new ArrayValue(false, false, var);
				varValue.setStringValue(String.valueOf(value));
				
				processResponseRecur((JSONArray) value, varValue);
			}
			else if(value instanceof JSONObject) {
				varValue = new ReferenceValue(false, false, var);
				varValue.setStringValue(String.valueOf(value));
				
				processResponseRecur(false, (JSONObject)value, varValue);
			}
			else if(value instanceof String){
				varType = value.getClass().toString();
				var.setType(varType);
				
				varValue = new StringValue(String.valueOf(value), false, var);
			}
			else if(value instanceof Integer){
				varType = value.getClass().toString();
				var.setType(varType);
				
				varValue = new PrimitiveValue(String.valueOf(value), false, var);
			}
			else if (value == null) {
				varValue = new ReferenceValue(true, false, var);
			}
			
			if(varValue != null) {
				selectedVariable.addChild(varValue);
				varValue.addParent(selectedVariable);					
			}
			
			index++;
		};
	}
}
