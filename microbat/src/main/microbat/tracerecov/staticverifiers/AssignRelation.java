package microbat.tracerecov.staticverifiers;

import microbat.model.value.VarValue;

public class AssignRelation {
	
	private AssignStatus assignStatus;
	private VarValue variableOfInterest;
	private VarValue field;
	
	private AssignRelation(AssignStatus assignStatus, VarValue variableOfInterest, VarValue field) {
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
	
	public static AssignRelation getGuaranteeAssignRelation(VarValue variableOfInterest, VarValue field) {
		return new AssignRelation(AssignStatus.GUARANTEE_ASSIGN, variableOfInterest, field);
	}
	
	public AssignStatus getAssignStatus() {
		return assignStatus;
	}

	public VarValue getVariableOfInterest() {
		return variableOfInterest;
	}

	public VarValue getField() {
		return field;
	}
	
}
