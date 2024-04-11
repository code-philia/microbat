package microbat;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

public class QueryRequestGenerator {

	public QueryRequestGenerator() {}
	
	public static String getQueryRequest(List<TraceNode> relatedNodes, VarValue wrongValue){
		
		StringBuilder stringBuilder = new StringBuilder("");
		for (TraceNode node : relatedNodes) {
			stringBuilder.append(QueryRequestGenerator.getOneRequest(node, wrongValue));
		}
		
		return stringBuilder.toString();
	}
	
	public static String getOneRequest(TraceNode node, VarValue wrongValue){
		String statement = node.getCodeStatement().trim();
		String value = wrongValue.getVarName();
		return ("Will the variable `" + value + "` be changed after the statement `" + statement + "` is executed?\n");
	}
	
	public static boolean[] getResult(int resultCnt, String respones){
		boolean[] result = new boolean[resultCnt];
		respones = respones.replaceAll("\\\\n|\\n", " ");
        String[] words = respones.split(" ");
        
		for (int i = words.length - 1; i >= 0; i--) {
            if(resultCnt <= 0) break;
            words[i] = words[i].trim().toLowerCase();
            if(words[i].equals("no")) {
            	result[resultCnt-1] = false;
            	resultCnt --;
            }
            else if(words[i].equals("yes")) {
            	result[resultCnt-1] = true;
            	resultCnt --;
            }
        }
		return result;
	}
	
};