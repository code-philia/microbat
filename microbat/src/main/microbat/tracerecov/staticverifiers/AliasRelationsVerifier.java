package microbat.tracerecov.staticverifiers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;

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
	
	public AliasRelationsVerifier(CFG cfg) {
		this.cfg = cfg;
		this.constantPoolGen = new ConstantPoolGen(cfg.getConstantPool());
	}
	
	public AssignRelation getVarAssignRelation(String varName) {
		if (varName.contains("[") || varName.contains("]")) {
			return AssignRelation.getNoGuaranteeAssignRelation();
		}
		return getAssignRelationRecur(cfg.getStartNode(), new HashMap<>(), varName);
	}
	
	private AssignRelation getAssignRelationRecur(CFGNode node, Map<CFGNode, WriteStatus> visitedNodes, String varName) {
		visitedNodes.put(node, null);
		
//		AssignRelation assignRelation = getAssignRelationAtNode(node, varName);
		return null;
	}

//	/**
//	 * Analyze two nodes (one assign step) at a time.
//	 * Assign Pattern: aload astore|putfield|putstatic
//	 */
//	private AssignRelation getAssignRelationAtNode(CFGNode node, String varName) {
//		Instruction instruction = node.getInstructionHandle().getInstruction();
//		
//		if (instruction instanceof ALOAD) {
//			List<CFGNode> children = node.getChildren();
//			boolean isLastNode = children == null || children.size() == 0;
//			if (isLastNode) {
//				return AssignStatus.GUARANTEE_NO_ASSIGN;
//			} else if (children.size() > 1) {
//				return AssignStatus.NO_GUARANTEE;
//			} else {
//				Instruction nextInstruction = children.get(0).getInstructionHandle().getInstruction();
//				if (nextInstruction instanceof ASTORE || nextInstruction instanceof PUTFIELD || nextInstruction instanceof PUTSTATIC) {}
//			}
//		} else if (instruction instanceof InvokeInstruction) {
//			return AssignStatus.NO_GUARANTEE;
//		} else {
//			return AssignStatus.GUARANTEE_NO_ASSIGN;
//		}
//
//		return AssignStatus.NO_GUARANTEE;
//	}

	public ReturnStatus getVarReturnStatus(String varName) {
		if (varName.contains("[") || varName.contains("]")) {
			return ReturnStatus.NO_GUARANTEE;
		}
		return getReturnStatusRecur(cfg.getStartNode(), new HashMap<>(), varName);
	}

	private ReturnStatus getReturnStatusRecur(CFGNode node, Map<CFGNode, WriteStatus> visitedNodes, String varName) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
