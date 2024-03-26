package microbat.instrumentation.benchmark;

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
}
