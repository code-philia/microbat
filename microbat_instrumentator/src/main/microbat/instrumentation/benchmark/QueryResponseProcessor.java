package microbat.instrumentation.benchmark;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import microbat.instrumentation.benchmark.MethodInfo.Action;
import microbat.instrumentation.benchmark.MethodInfo.Index;
import microbat.instrumentation.benchmark.MethodInfo.Type;
import microbat.instrumentation.runtime.ExecutionTracer;
import microbat.model.value.VarValue;

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
	public static Set<String> getWrittenVars(String originalInfo, String queryResponse) {
		if (originalInfo.equals("") || queryResponse.equals("")) {
			return null;
		}
		
		Set<String> originalVars = getVariables(originalInfo);
		Set<String> newVars = getVariables(queryResponse);
		
		Set<String> writtenVariables = new HashSet<>();
		
		// get added variables and common variables
		for (String variable : newVars) {
			if (!originalVars.contains(variable)) {
				String[] pairs = variable.split(",type:");
				if (pairs.length < 2) {
					continue;
				}
				String[] nameInfo = pairs[0].split("name:");
				if (nameInfo.length < 2) {
					continue;
				}
				String name = nameInfo[1];
				if (!name.equals("")) {
					writtenVariables.add(name);
				}
			}
		}
		// get removed variables
		for (String variable : originalVars) {
			if (!newVars.contains(variable)) {
				String[] pairs = variable.split(",type:");
				if (pairs.length < 2) {
					continue;
				}
				String[] nameInfo = pairs[0].trim().split("name:");
				if (nameInfo.length < 2) {
					continue;
				}
				String name = nameInfo[1].trim();
				if (!name.equals("")) {
					writtenVariables.add(name);
				}
			}
		}
		
		return writtenVariables;
	}
	
	public static Set<VarValue> getWrittenVariables(String response, VarValue variable, Object value,
			String residingClass, int line, ExecutionTracer tracer) {
		Set<VarValue> variables = new HashSet<>();
		String[] entries = response.split(";");
		Field[] fields = null;
		if (value != null) {
			fields = value.getClass().getDeclaredFields();
		}
		for (String entry : entries) {
			entry = entry.trim();
			
			char[] characters = entry.toCharArray();
			int startIndex = -1;
			int endIndex = characters.length;
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
			entry = entry.substring(startIndex + 1, endIndex);
			
			String[] names = entry.split("#");
			int index = 0;
			VarValue current = variable;
			boolean isFound = false;
			while (index < names.length) {
				VarValue temp = current.findVarValueByName(names[index]);
				if (temp != null) {
					isFound = true;
					current = temp;
				} else if (current.getVarName().equals(names[index])) {
					isFound = true;
				} else {
					break;
				}
				index++;
			}
			if (isFound) {
				variables.add(current);
			} else {
				index = 0;
				while (index < names.length) {
					String fieldName = names[index];
					for (Field f : fields) {
						if (f != null && f.getName().equals(fieldName)) {
							try {
								String varName = variable.getVarName() + "." + fieldName;
								String varType = f.getType().toString();
								VarValue newVarValue = tracer.createVarValue(varName, varType, residingClass, line, "",
										f.get(value));
								variables.add(newVarValue);
							} catch (IllegalArgumentException | IllegalAccessException e) {
								variables.add(variable);
							}
						}
					}
					index++;
				}
			}
		}
		return variables;
	}
	
	private static Set<String> getVariables(String info) {
		char[] characters = info.toCharArray();
		int startIndex = -1;
		int endIndex = characters.length;
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
		info = info.substring(startIndex + 1, endIndex);
		
		String[] entries = info.split("\\{name:");
		Set<String> variables = new HashSet<>();
		for (String entry : entries) {
			if (entry == null || entry.equals("")) {
				continue;
			}
			
			entry = "name:" + entry;
			
			char[] entryChars = entry.toCharArray();
			int index = entryChars.length;
			for (int i = entryChars.length - 1; i >= 0; i--) {
				if (entryChars[i] == '}' || entryChars[i] == ';' || entryChars[i] == '>') {
					index = i;
				} else {
					break;
				}
			}
			variables.add(entry.substring(0, index));
		}
		
		return variables;
	}
	
}
