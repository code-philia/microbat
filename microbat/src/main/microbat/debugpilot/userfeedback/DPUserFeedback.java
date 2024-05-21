package microbat.debugpilot.userfeedback;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import microbat.log.Log;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

/**
 * User feedback for DebugPilot (DP) <br/>
 * 
 * It support multiple variable selection
 */
public class DPUserFeedback {
	
	protected DPUserFeedbackType type;
	protected final TraceNode node;
	protected final Set<VarValue> correctVars;
	protected final Set<VarValue> wrongVars;
	protected final Set<VarValue> unclearVars;
	protected String reason;
	protected boolean reasonable;

	private boolean confirmed;
	private DPUserFeedback parent;
	private DPUserFeedbackType parent_type;
	private float confidence;
	private double reach_possibility;
	private double[] step_prediction;
	private double[][] var_prediction;
	
	public DPUserFeedback(final DPUserFeedbackType type, final TraceNode node) {
		this(type, node, null, null);
	}
	
	public DPUserFeedback(final DPUserFeedbackType type, final TraceNode node, final Collection<VarValue> wrongVars, Collection<VarValue> correctVars) {
		Objects.requireNonNull(type, Log.genMsg(getClass(), "Given user feedback type cannot be null"));
		Objects.requireNonNull(node, Log.genMsg(getClass(), "Given node cannot be null"));
		this.type = type;
		this.node = node;
		this.wrongVars = new HashSet<>();
		this.correctVars = new HashSet<>();
		this.unclearVars = new HashSet<>();
		this.reason = "";
		this.reasonable = true;
		if (wrongVars != null) {
			this.addWrongVars(wrongVars);			
		}
		if (correctVars != null) {			
			this.addCorrectVars(correctVars);
		}
		this.parent = null;
	}
 
	/**
	 * Add wrong variables in set. <br/>
	 * If the given variable is already in correct set, it will be removed from correct set.
	 * @param wrongVars Wrong variables to add
	 */
	public void addWrongVars(final Collection<VarValue> wrongVars) {
		this.wrongVars.addAll(wrongVars);
		if (!this.correctVars.isEmpty()) {
			this.correctVars.removeAll(wrongVars);			
		}
	}
	
	/**
	 * Add wrong variables in set. <br/>
	 * If the given variable is already in correct set, it will be removed from correct set.
	 * @param wrongVars Wrong variables to add
	 */
	public void addWrongVar(final VarValue... wrongVars) {
		this.addWrongVars(Arrays.asList(wrongVars));
	}
	
	/**
	 * Add correct variable in set. <br/>
	 * If the give variable is already in wrong set, it will be removed from wrong set.
	 * @param correctVars Correct variables to add
	 */
	public void addCorrectVars(final Collection<VarValue> correctVars) {
		this.correctVars.addAll(correctVars);
		if (!this.wrongVars.isEmpty())
			this.wrongVars.removeAll(correctVars);
	}
	
	/**
	 * Add correct variable in set. <br/>
	 * If the give variable is already in wrong set, it will be removed from wrong set.
	 * @param correctVars Correct variables to add
	 */
	public void addCorrectVar(final VarValue... correctVars) {
		this.addCorrectVars(Arrays.asList(correctVars));
	}
	
	public void addUnclearVars(final Collection<VarValue> unclearVars) {
		this.unclearVars.addAll(unclearVars);
	}
	
	public void addUnclearVar(final VarValue... unclearVars) {
		this.addUnclearVars(Arrays.asList(unclearVars));
	}
	
	
	public DPUserFeedbackType getType() {
		return type;
	}
	
	public String getTypeStr() {
		if(type == DPUserFeedbackType.CORRECT) {
			return "<CORRECT>";
		}
		else if(type == DPUserFeedbackType.ROOT_CAUSE) {
			return "<ROOT>";
		}
		else if(type == DPUserFeedbackType.WRONG_PATH) {
			return "<CONTROL>";
		}
		else if(type == DPUserFeedbackType.WRONG_VARIABLE) {
			return "<DATA>";
		}
		return "<UNCLEAR>";
	}
	
	public String getTypeStr4LLM() {
		if(type == DPUserFeedbackType.CORRECT) {
			return "correct";
		}
		else if(type == DPUserFeedbackType.ROOT_CAUSE) {
			return "root cause";
		}
		else if(type == DPUserFeedbackType.WRONG_PATH) {
			return "should not be excuted";
//			return "wrong path";
		}
		else if(type == DPUserFeedbackType.WRONG_VARIABLE) {
			return "contains wrong variable";
//			return "wrong variable";
		}
		return "unclear";
	}
	
	public String getTypeDesc() {
		if(type == DPUserFeedbackType.CORRECT) {
			return "the step is correct";
		}
		else if(type == DPUserFeedbackType.ROOT_CAUSE) {
			return "the step is root cause of bug";
		}
		else if(type == DPUserFeedbackType.WRONG_PATH) {
			return "the step should not be excuted";
		}
		else if(type == DPUserFeedbackType.WRONG_VARIABLE) {
			return "some variables are wrong";
		}
		return "unclear";
	}

	public TraceNode getNode() {
		return node;
	}

	public Set<VarValue> getCorrectVars() {
		return correctVars;
	}

	public Set<VarValue> getWrongVars() {
		return wrongVars;
	}
	
	public DPUserFeedback getParent() {
		return parent;
	}
	
	public String getReason() {
		return reason;
	}
	
	public float getConfidence() {
		return confidence;
	}
	
	public double getReachPossibility() {
		return reach_possibility;
	}
	
	public DPUserFeedbackType getParentType() {
		return parent_type;
	}
	
	public double[] getPrediction() {
		return step_prediction;
	}
	
	public void setPrediction(double[] p) {
		step_prediction = p;
	}
	
	public double[][] getVarPrediction(){
		return var_prediction;
	}
	
	public void setVarPrediction(double[][] p) {
		var_prediction = p;
	}
	
	public void setParentType(DPUserFeedbackType type) {
		parent_type = type;
	}
	
	public boolean isConfirmed() {
		return confirmed;
	}
	
	public void setParent(DPUserFeedback p) {
		parent = p;
	}
	
	public void setType(DPUserFeedbackType t) {
		type = t;
	}
	
	public void setTypeByPred(int i) {
		switch(i) {
		case 0:
			type = DPUserFeedbackType.ROOT_CAUSE;
			break;
		case 1:
			type = DPUserFeedbackType.CORRECT;
			break;
		case 2:
			type = DPUserFeedbackType.WRONG_PATH;
			break;
		case 3:
			type = DPUserFeedbackType.WRONG_VARIABLE;
			break;
		default:
			type = DPUserFeedbackType.UNCLEAR;
		}
	}
	
	public void setTypeByStr(String typeStr) {
		if("root cause".equals(typeStr)) {
			type = DPUserFeedbackType.ROOT_CAUSE; 
		}
		else if("correct".equals(typeStr)) {
			type = DPUserFeedbackType.CORRECT;
		}
		else if("wrong path".equals(typeStr)) {
			type = DPUserFeedbackType.WRONG_PATH;
		}
		else if("wrong variable".equals(typeStr)) {
			type = DPUserFeedbackType.WRONG_VARIABLE;
		}
		else {
			type = DPUserFeedbackType.UNCLEAR;
		}
	}
	
	public void setTypeByStrNew(String typeStr) {
		if("root_cause".equals(typeStr)) {
			type = DPUserFeedbackType.ROOT_CAUSE; 
		}
		else if("stop".equals(typeStr)) {
			type = DPUserFeedbackType.CORRECT;
		}
		else if("check_control_dependency".equals(typeStr)) {
			type = DPUserFeedbackType.WRONG_PATH;
		}
		else if("check_data_dependency".equals(typeStr)) {
			type = DPUserFeedbackType.WRONG_VARIABLE;
		}
		else {
			type = DPUserFeedbackType.UNCLEAR;
		}
	}
	
	public void setReason(String r) {
		reason = r;
	}
	
	public void setConfidence(float c) {
		confidence = c;
	}
	
	public void setConfidence() {
	    if(type==DPUserFeedbackType.ROOT_CAUSE) {
			confidence = (float)step_prediction[0];
		}
		else if(type==DPUserFeedbackType.CORRECT) {
			confidence = (float)step_prediction[1];
		}
		else if(type==DPUserFeedbackType.WRONG_PATH) {
			confidence = (float)step_prediction[2];
		}
		else if(type == DPUserFeedbackType.WRONG_VARIABLE){
			confidence = (float)step_prediction[3];
		}
	}
	
	public void setReachPossibility(double p) {
		reach_possibility = p;
	}
	
	public void setConfirmed(boolean c) {
		confirmed = c;
	}
	
	public boolean isReasonable() {
		return reasonable;
	}
	
	public void setReasonable(boolean r) {
		reasonable = r;
	}
	
	/**
	 * Two feedback are similar if: <br/>
	 * 1. They have the same type <br/>
	 * 2. If it is wrong variable, wrong variables set of otherFeedback should be the sub-set of this wrong variable set
	 * @param otherFeedback Feedback to compare
	 * @return True if similar
	 */
	public boolean isSimilar(final DPUserFeedback otherFeedback) {
		if (!this.node.equals(otherFeedback.node)) {
			return false;
		}
		
		if (this.type != otherFeedback.type) {
			return false;
		}
		
		if (this.type == DPUserFeedbackType.WRONG_VARIABLE && !this.wrongVars.containsAll(otherFeedback.wrongVars)) {
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public int hashCode() {
		int hashCode = 7;
		hashCode = 17 * hashCode + this.type.hashCode();
		hashCode = 17 * hashCode + this.node.hashCode();
		hashCode = 17 * hashCode + this.wrongVars.hashCode();
		hashCode = 17 * hashCode + this.correctVars.hashCode();
		return hashCode;
	}
	
	@Override
	public boolean equals(Object otherObj) {
		if (this == otherObj) return true;
		if (otherObj == null || this.getClass() != otherObj.getClass()) return false;
		
		final DPUserFeedback otherFeedback = (DPUserFeedback) otherObj;
		
		if (this.type != otherFeedback.type) {
			return false;
		}
		
		if (!this.node.equals(otherFeedback.node)) {
			return false;
		}
		
		if (!this.wrongVars.equals(otherFeedback.wrongVars)) {
			return false;
		}
		
		if (!this.correctVars.equals(otherFeedback.correctVars)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("[");
		stringBuilder.append(this.type.name() + ",");
		stringBuilder.append("Node: " + this.node.getOrder() + ",");
		stringBuilder.append("Wrong Variable: ");
		for (VarValue wrongVar : this.wrongVars) {
			stringBuilder.append(wrongVar.getVarName() + ",");
		}
		stringBuilder.append("Correct Variable: ");
		for (VarValue correctVar : this.correctVars) {
			stringBuilder.append(correctVar.getVarName() + ",");
		}
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	
//	public static DPUserFeedback parseFeedbacks(final TraceNode node, final UserFeedback... feedbacks) {
//		return DPUserFeedback.parseFeedbacks(node, Arrays.asList(feedbacks));
//	}
//	
//	public static DPUserFeedback parseFeedbacks(final TraceNode node, final Collection<UserFeedback> userFeedbacks) {
//		Objects.requireNonNull(userFeedbacks, Log.genMsg("DPUserFeedback", "Given userFeedbacks cannot be null"));
//		
//		if (userFeedbacks.isEmpty()) {
//			throw new IllegalArgumentException(Log.genMsg("DPUserFeedback", "Cannot create DPUserFeedback from no feedbacks"));
//		}
//		
//		// Check that there are no conflicting feedbacks
//		if (userFeedbacks.stream().map(feedback -> feedback.getFeedbackType()).distinct().limit(2).count() == 1) {
//			throw new IllegalArgumentException(Log.genMsg("DPUserFeedback", "Cannot create DPUserFeedback from conflicting feedbacks"));
//		}
//		
//		final String feedbackType = userFeedbacks.stream().map(feedback -> feedback.getFeedbackType()).findFirst().orElse(null);
//		if (feedbackType == null) {
//			throw new RuntimeException(Log.genMsg("DPUserFeedbacks", "feedbackType is null"));
//		}
//		
//		if (feedbackType.equals(UserFeedback.ROOTCAUSE)) {
//			return new DPUserFeedback(DPUserFeedbackType.ROOT_CAUSE, node);
//		} else if (feedbackType.equals(UserFeedback.WRONG_PATH)) {
//			return new DPUserFeedback(Log.genm)
//		}
//		
//		
//		return null;
//	}
}
