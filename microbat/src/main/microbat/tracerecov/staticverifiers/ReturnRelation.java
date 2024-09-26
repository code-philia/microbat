package microbat.tracerecov.staticverifiers;

public class ReturnRelation {

	private ReturnStatus returnStatus;

	private String fieldName;

	private ReturnRelation(ReturnStatus returnStatus, String fieldName) {
		this.returnStatus = returnStatus;
		this.fieldName = fieldName;
	}

	public static ReturnRelation getNoGuaranteeReturnRelation() {
		return new ReturnRelation(ReturnStatus.NO_GUARANTEE, null);
	}

	public static ReturnRelation getGuaranteeNoReturnRelation() {
		return new ReturnRelation(ReturnStatus.GUARANTEE_NO_RETURN, null);
	}

	public static ReturnRelation getGuaranteeReturnRelation(String fieldName) {
		return new ReturnRelation(ReturnStatus.GUARANTEE_RETURN, fieldName);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ReturnRelation) {
			ReturnRelation otherRelation = (ReturnRelation) other;
			boolean sameReturnStatus = this.returnStatus.equals(otherRelation.returnStatus);
			boolean sameFieldName = this.fieldName == null ? otherRelation.fieldName == null
					: this.fieldName.equals(otherRelation.fieldName);
			return sameReturnStatus && sameFieldName;
		}
		return false;
	}

	public ReturnStatus getReturnStatus() {
		return returnStatus;
	}

	public String getFieldName() {
		return fieldName;
	}

}
