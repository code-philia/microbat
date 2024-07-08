package microbat.instrumentation;

public class RuntimeCondition {
	private String variableName;
	private String variableType;
	private String variableValue;
	private String classStructure;
	
	public RuntimeCondition(String variableName, String variableType, String variableValue, String classStructure) {
		super();
		this.variableName = variableName;
		this.variableType = variableType;
		this.variableValue = variableValue;
		this.classStructure = classStructure;
	}
	
	
}
