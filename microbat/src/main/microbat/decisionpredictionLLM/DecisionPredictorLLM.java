package microbat.decisionpredictionLLM;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.jobs.Job;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import microbat.debugpilot.userfeedback.DPUserFeedback;
import microbat.debugpilot.userfeedback.DPUserFeedbackType;
import microbat.handler.DebugPilotHandler;
import microbat.handler.callbacks.HandlerCallbackManager;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;
import microbat.model.variable.VirtualVar;
import microbat.util.Settings;
import microbat.views.DialogUtil;

public class DecisionPredictorLLM {
	private int condition_result_num = 0;
	
	public boolean predictDecision(DPUserFeedback curStep) {
		Agent agent = new Agent();
//		agent.setLogFile(Settings.projectName);
		
		// infer specification
		String specification = agent.infer_specification(curStep);
		if("".equals(specification)) {
			DialogUtil.popErrorDialog("An error occured when getting reponse from LLM, please confirm again.", "Response Error");
			return false;
		}
		
		// make decision
		int retry_parse_failed = 3;
		while(retry_parse_failed > 0) {
			try { // in case parse error caused by incorrect format
				String decision = agent.make_decision(specification,curStep);
				if("".equals(specification)) {
					DialogUtil.popErrorDialog("An error occured when getting reponse from LLM, please confirm again.", "Response Error");
					return false;
				}
				
				int start = decision.indexOf("{");
				int end = decision.lastIndexOf("}");
				decision = decision.substring(start,end+1);
				
				JSONObject responseJson = JSONUtil.parseObj(decision);
				
				String typeStr = responseJson.getStr("decision");
				curStep.setTypeByStrNew(typeStr);
				if(curStep.getType() == DPUserFeedbackType.UNCLEAR) {
					retry_parse_failed -= 1;
					continue;
				}
				
				if(responseJson.containsKey("reason")) {
					String reasonStr = responseJson.getStr("reason");
					curStep.setReason(reasonStr);
				}
				else {
					curStep.setReason("No reason.");
				}
				
				if(responseJson.containsKey("wrong_variables")) {
					condition_result_num = 0;
					List<VarValue> readVariables = curStep.getNode().getReadVariables();
					
					JSONArray wrong_vars = responseJson.getJSONArray("wrong_variables");
					List<String> wrong_var_list = JSONUtil.toList(wrong_vars, String.class);
					
					for(VarValue varValue : readVariables) {
						String varName = parseVarName(varValue.getVariable(),readVariables);
						if(wrong_var_list.contains(varName)) {
							curStep.addWrongVar(varValue);
						}
					}
				}
				return true;
				
			} catch(Exception e) {
				retry_parse_failed -= 1;
				continue;
			}
		}
		
		//shut down debugpilot
		DialogUtil.popErrorDialog("An error occured when parsing reponse, please confirm again.", "Response Error");
		return false;
		
		
//		condition_result_num = 0;
//		if(responseJson.containsKey("variable")) {
//			JSONObject var_label = responseJson.getJSONObject("variable");
//			List<VarValue> readVariables = curStep.getNode().getReadVariables();
//			for(VarValue varValue : readVariables) {
//				String varName = parseVarName(varValue.getVariable(),readVariables);
//				if(var_label.keySet().contains(varName)) {
////					if(var_label.getStr(varName).equals("correct")) {
////						curStep.addCorrectVar(varValue);
////					}
//					if(var_label.getStr(varName).equals("wrong")){
//						curStep.addWrongVar(varValue);
//					}
//				}
//			}
//		}
	}
	
	
    public String getClassName(String classFullName) {
    	if(!classFullName.contains(".")) {
    		return classFullName;
    	}
    	String[] parts = classFullName.split("\\.");
    	return parts[parts.length-1];
    }
    
    
    public String parseVarName(Variable variable, List<VarValue> readVarValues) {
    	String varName = variable.getName();
    	if(variable instanceof VirtualVar) {
    		String result = "return_from_";
    		String[] parts = varName.split("#");

    		String className = parts[0];
    		result+=(getClassName(className)+".");

    		String methodName = parts[1].split( "\\(" )[0];
    		result+=methodName;
    		return result;
    	}
    	//ConditionResult_1
    	else if(varName.contains("ConditionResult_")) {
    		String result =  "condition_result_"+String.valueOf(condition_result_num);
    		condition_result_num+=1;
    		return result;
    	}
    	else if(varName.contains("[")) {
    		int index = varName.indexOf("[");
    		String address = varName.substring(0,index);
    		varName = varName.substring(index);
    		
    		for(VarValue v : readVarValues) {
    			String oriValue = v.getStringValue();
    			if(oriValue.contains("@")) {
    				String afterAt = oriValue.substring(oriValue.indexOf("@")+1);
    				if(Long.parseLong(afterAt,16) == Long.parseLong(address,10)) {
    					varName = (v.getVarName()+varName);
    					break;
    				}
    			}
    		}
    		
    	}
    	return varName;
    }
}
