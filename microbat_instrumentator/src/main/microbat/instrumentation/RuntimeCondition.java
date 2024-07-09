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

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableType() {
		return variableType;
	}

	public void setVariableType(String variableType) {
		this.variableType = variableType;
	}

	public String getVariableValue() {
		return variableValue;
	}

	public void setVariableValue(String variableValue) {
		this.variableValue = variableValue;
	}

	public String getClassStructure() {
		return classStructure;
	}

	public void setClassStructure(String classStructure) {
		this.classStructure = classStructure;
	}

}
