package microbat.instrumentation.trace.data;

import java.util.Stack;

import microbat.model.trace.TraceNode;

public class MethodCallStack extends Stack<TraceNode> {
	private static final long serialVersionUID = 1L;

	public TraceNode safePop() {
		if (size() != 0) {
			return pop();
		}
		return null;
	}
	
}