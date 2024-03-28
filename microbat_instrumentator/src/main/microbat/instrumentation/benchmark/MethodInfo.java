package microbat.instrumentation.benchmark;

public class MethodInfo {
	
	static public enum Type {
		NONE,
		SET,
		GET
	}
	
	static public enum Action {
		ADD,
		REMOVE,
		REPLACE,
		NA
	}
	
	static public enum Index {
		START,
		END,
		INDEX,
		KEY,
		ALL,
		NA
	}
	
	private String methodSig;
	private Type type;
	private Action action;
	private String criticalDataStructure;
	private Index index;

	/**
	 * Used in hardcode version
	 */
	public MethodInfo(String methodSig, Type type, Action action, Index index) {
		this.methodSig = methodSig;
		this.type = type;
		this.action = action;
		this.index = index;
	}
	
	/**
	 * Used in ChatGPT version
	 */
	public MethodInfo(String methodSig, Type type, Action action, String criticalDataStructure, Index index) {
		this.methodSig = methodSig;
		this.type = type;
		this.action = action;
		this.criticalDataStructure = criticalDataStructure;
		this.index = index;
	}

	public String getMethodSig() {
		return methodSig;
	}

	public Type getType() {
		return type;
	}

	public Action getAction() {
		return action;
	}
	
	public String getCriticalDataStructure() {
		return criticalDataStructure;
	}

	public Index getIndex() {
		return index;
	}

}
