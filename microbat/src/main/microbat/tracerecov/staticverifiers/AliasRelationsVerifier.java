package microbat.tracerecov.staticverifiers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
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
	
	private AssignRelation getAssignRelationRecur(CFGNode node, Map<CFGNode, AssignRelation> visitedNodes, String varName) {
		visitedNodes.put(node, null);
		
		AssignRelation assignRelation = getAssignRelationAtNode(node, varName);
		if (assignRelation.getAssignStatus().equals(AssignStatus.NO_GUARANTEE)) {
			visitedNodes.put(node, assignRelation);
			return assignRelation;
		}
		
		AssignRelation assignRelationAmongChildren = null;
		List<CFGNode> children = node.getChildren();
		for (CFGNode child : children) {
			AssignRelation childAssignRelation;
			if (visitedNodes.containsKey(child)) {
				if (visitedNodes.get(child) == null) {
					continue;
				} else {
					childAssignRelation = visitedNodes.get(child);
				}
			} else {
				childAssignRelation = getAssignRelationRecur(child, visitedNodes, varName);
			}

			// if status is NO_GUARANTEE at any point, the overall status is NO_GUARANTEE
			if (childAssignRelation.getAssignStatus().equals(AssignStatus.NO_GUARANTEE)) {
				visitedNodes.put(node, childAssignRelation);
				return childAssignRelation;
			}

			if (assignRelationAmongChildren == null) {
				assignRelationAmongChildren = childAssignRelation;
			} else if (!assignRelationAmongChildren.equals(childAssignRelation)) {
				// if assign relations are different among branches, the overall status is
				// NO_GUARANTEE
				AssignRelation overallAssignRelation = AssignRelation.getNoGuaranteeAssignRelation();
				visitedNodes.put(node, overallAssignRelation);
				return overallAssignRelation;
			}
		}

		if (assignRelationAmongChildren == null) {
			// if there are no children, return the assign relation of the current node
			visitedNodes.put(node, assignRelation);
			return assignRelation;
		} else if (assignRelationAmongChildren.getAssignStatus().equals(AssignStatus.GUARANTEE_ASSIGN)) {
			// if the children all have status GUARANTEE_ASSIGN, and are assigning to the same field, 
			// then the overall status is GUARANTEE_ASSIGN
			visitedNodes.put(node, assignRelationAmongChildren);
			return assignRelationAmongChildren;
		}

		visitedNodes.put(node, assignRelation);
		return assignRelation;
	}

	/**
	 * Analyze two nodes (one assign step) at a time.
	 * Assign Pattern: aload putfield|putstatic
	 */
	private AssignRelation getAssignRelationAtNode(CFGNode node, String varName) {
		Instruction instruction = node.getInstructionHandle().getInstruction();
		
		if (instruction instanceof ALOAD) {
			List<CFGNode> children = node.getChildren();
			boolean isLastNode = children == null || children.size() == 0;
			if (isLastNode) {
				return AssignRelation.getGuaranteeNoAssignRelation();
			} else if (children.size() > 1) {
				return AssignRelation.getNoGuaranteeAssignRelation();
			} else {
				Instruction nextInstruction = children.get(0).getInstructionHandle().getInstruction();
				if (nextInstruction instanceof PUTFIELD || nextInstruction instanceof PUTSTATIC) {
					FieldInstruction fieldInstruction = (FieldInstruction) nextInstruction;
					String fieldName = fieldInstruction.getFieldName(constantPoolGen);
					return AssignRelation.getGuaranteeAssignRelation(varName, fieldName);
				}
			}
		} else if (instruction instanceof InvokeInstruction) {
			return AssignRelation.getNoGuaranteeAssignRelation();
		} else {
			return AssignRelation.getGuaranteeNoAssignRelation();
		}

		return AssignRelation.getNoGuaranteeAssignRelation();
	}

	public ReturnStatus getVarReturnStatus(String varName) {
		if (varName.contains("[") || varName.contains("]")) {
			return ReturnStatus.NO_GUARANTEE;
		}
		return getReturnStatusRecur(cfg.getStartNode(), new HashMap<>(), varName);
	}

	private ReturnStatus getReturnStatusRecur(CFGNode node, Map<CFGNode, ReturnStatus> visitedNodes, String varName) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
