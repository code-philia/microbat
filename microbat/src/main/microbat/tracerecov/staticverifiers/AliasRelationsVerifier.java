package microbat.tracerecov.staticverifiers;

import java.util.HashMap;

import org.apache.bcel.generic.ConstantPoolGen;

import microbat.codeanalysis.bytecode.CFG;
import microbat.codeanalysis.bytecode.CFGNode;

/**
 * This class performs static analysis and verifies whether a candidate variable
 * is guaranteed to be assigned / not assigned / returned / not returned by a 
 * method call.
 * 
 * @author hongshuwang
 */
public class AliasRelationsVerifier {
	
	private CFG cfg;
	private ConstantPoolGen constantPoolGen;
	
	public enum AssignStatus {
		GUARANTEE_ASSIGN, GUARANTEE_NO_ASSIGN, NO_GUARANTEE
	}
	
	public enum ReturnStatus {
		GUARANTEE_RETURN, GUARANTEE_NO_RETURN, NO_GUARANTEE
	}
	
	public AliasRelationsVerifier(CFG cfg) {
		this.cfg = cfg;
		this.constantPoolGen = new ConstantPoolGen(cfg.getConstantPool());
	}
	
	public AssignStatus getVarAssignStatus(String varName) {
		if (varName.contains("[") || varName.contains("]")) {
			return AssignStatus.NO_GUARANTEE;
		}
		return getAssignStatusRecur(cfg.getStartNode(), new HashMap<>(), varName);
	}
	
	private AssignStatus getAssignStatusRecur(CFGNode startNode, HashMap hashMap, String varName) {
		// TODO Auto-generated method stub
		return null;
	}

	public ReturnStatus getVarReturnStatus(String varName) {
		if (varName.contains("[") || varName.contains("]")) {
			return ReturnStatus.NO_GUARANTEE;
		}
		return getReturnStatusRecur(cfg.getStartNode(), new HashMap<>(), varName);
	}

	private ReturnStatus getReturnStatusRecur(CFGNode startNode, HashMap hashMap, String varName) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
