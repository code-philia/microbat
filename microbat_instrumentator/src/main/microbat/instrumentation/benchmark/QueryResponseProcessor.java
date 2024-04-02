package microbat.instrumentation.benchmark;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import microbat.instrumentation.benchmark.MethodInfo.Action;
import microbat.instrumentation.benchmark.MethodInfo.Index;
import microbat.instrumentation.benchmark.MethodInfo.Type;

public class QueryResponseProcessor {

	public static MethodInfo getMethodInfo(String methodSig, String queryResponse) {
		String[] tags = queryResponse.split(">");
		for (int i = 0; i < tags.length; i++) {
			// first character: "<"
			tags[i] = tags[i].substring(1);
		}
		
		if (tags.length == 0) {
			// TODO: throw exception and query again
		}
		Type type = Type.valueOf(tags[0].toUpperCase());
		Action action = Action.NA;
		String criticalDataStructure = "";
		Index index = Index.NA;
		if (type.equals(Type.SET)) {
			action = Action.valueOf(tags[1].toUpperCase());
			if (!action.equals(Action.REMOVE)) {
				criticalDataStructure = tags[2];
				index = Index.valueOf(tags[3].toUpperCase());
			}
		}
		
		MethodInfo methodInfo = new MethodInfo(methodSig, type, action, criticalDataStructure, index);
		
		return methodInfo;
	}
	
	/**
	 * Returns a list of names of written variables
	 * 
	 * @param response
	 * @return
	 */
	public static Map<String, String> getWrittenVars(String originalInfo, String queryResponse) {
		if (originalInfo.equals("") || queryResponse.equals("")) {
			return null;
		}
		
		Set<String> originalVars = getVariables(originalInfo);
		Set<String> newVars = getVariables(queryResponse);
		
		Map<String, String> writtenVariables = new HashMap<>();
		for (String variable : newVars) {
			if (!originalVars.contains(variable)) {
				String[] pairs = variable.split(",");
				String name = "";
				String type = "";
				for (String pair : pairs) {
					String key = pair.split(":")[0];
					if (key.equals("name")) {
						name = pair.split(":")[1];
					} else if (key.equals("type")) {
						type = pair.split(":")[1];
					}
				}
				if (!name.equals("") && !type.equals("")) {
					writtenVariables.put(name, type);
				}
			}
		}
		
		return writtenVariables;
	}
	
	private static Set<String> getVariables(String info) {
		char[] characters = info.toCharArray();
		int startIndex = -1;
		int endIndex = characters.length + 1;
		for (int i = 0; i < characters.length; i++) {
			if (characters[i] == '<') {
				startIndex = i;
				break;
			}
		}
		for (int i = characters.length - 1; i >= 0; i--) {
			if (characters[i] == '>') {
				endIndex = i;
				break;
			}
		}
		info = info.substring(startIndex + 1, endIndex - 1);
		
		String[] entries = info.split("\\{");
		Set<String> variables = new HashSet<>();
		for (String entry : entries) {
			if (entry == null || entry.equals("")) {
				continue;
			}
			if (entry.contains("}")) {
				int index = entry.indexOf("}");
				variables.add(entry.substring(0, index));
			} else {
				variables.add(entry);
			}
		}
		
		return variables;
	}
	
}
