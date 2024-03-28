package microbat.instrumentation.benchmark;

import java.util.List;

public class ClassInfo {
	
	private String className;
	private List<MethodInfo> writters;
	private List<String> criticalVariables;

	public ClassInfo(String className, List<MethodInfo> writters, List<String> criticalVariables) {
		this.className = className;
		this.writters = writters;
		this.criticalVariables = criticalVariables;
	}
	
	public MethodInfo getMethodInfo(String methodSignature) {
		for (MethodInfo method : writters) {
			if (method.getMethodSig().equals(methodSignature)) {
				return method;
			}
		}
		return null;
	}
	
	public String getCriticalDataStructure() {
		return this.criticalVariables.get(0);
	}
	
	public String getLengthVariable() {
		return this.criticalVariables.get(1);
	}
}
