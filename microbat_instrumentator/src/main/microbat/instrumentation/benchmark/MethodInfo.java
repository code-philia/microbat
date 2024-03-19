package microbat.instrumentation.benchmark;

public class MethodInfo {
	
	static public enum Type {
		NONE,
		IS_SETTER,
		IS_GETTER
	}
	
	static public enum Action {
		ADD,
		REMOVE,
		REPLACE
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
	private Index index;

	public MethodInfo(String methodSig, Type type, Action action, Index index) {
		this.methodSig = methodSig;
		this.type = type;
		this.action = action;
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

	public Index getIndex() {
		return index;
	}

}
