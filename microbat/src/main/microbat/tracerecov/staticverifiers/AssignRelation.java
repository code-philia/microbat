package microbat.tracerecov.staticverifiers;

public class AssignRelation {
	
	private AssignStatus assignStatus;
	private String variableOfInterest;
	private String field;
	
	private AssignRelation(AssignStatus assignStatus, String variableOfInterest, String field) {
		this.assignStatus = assignStatus;
		this.variableOfInterest = variableOfInterest;
		this.field = field;
	}
	
	public static AssignRelation getNoGuaranteeAssignRelation() {
		return new AssignRelation(AssignStatus.NO_GUARANTEE, null, null);
	}
	
	public static AssignRelation getGuaranteeNoAssignRelation() {
		return new AssignRelation(AssignStatus.GUARANTEE_NO_ASSIGN, null, null);
	}
	
	public static AssignRelation getGuaranteeAssignRelation(String variableOfInterest, String field) {
		return new AssignRelation(AssignStatus.GUARANTEE_ASSIGN, variableOfInterest, field);
	}
	
	public AssignStatus getAssignStatus() {
		return assignStatus;
	}

	public String getVariableOfInterest() {
		return variableOfInterest;
	}

	public String getField() {
		return field;
	}
	
}
