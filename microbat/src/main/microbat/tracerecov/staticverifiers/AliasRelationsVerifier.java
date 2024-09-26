package microbat.tracerecov.staticverifiers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.ReturnInstruction;

import microbat.codeanalysis.bytecode.CFG;
import microbat.codeanalysis.bytecode.CFGNode;
import microbat.tracerecov.CannotBuildCFGException;
import microbat.tracerecov.TraceRecovUtils;

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
	private String methodSignature;

	public AliasRelationsVerifier(CFG cfg, String methodSignature) {
		this.cfg = cfg;
		this.constantPoolGen = new ConstantPoolGen(cfg.getConstantPool());
		this.methodSignature = methodSignature;
	}

	public AssignRelation getVarAssignRelation(String varName, int varIndex, String className) {
		if (varName == null || varIndex == -1) {
			return AssignRelation.getGuaranteeNoAssignRelation();
		}

		if (varName.contains("[") || varName.contains("]")) {
			return AssignRelation.getNoGuaranteeAssignRelation();
		}

		return getAssignRelationRecur(cfg.getStartNode(), new HashMap<>(), varName, varIndex, className);
	}

	private AssignRelation getAssignRelationRecur(CFGNode node, Map<CFGNode, AssignRelation> visitedNodes,
			String varName, int varIndex, String className) {
		visitedNodes.put(node, null);

		AssignRelation assignRelation = getAssignRelationAtNode(node, varName, varIndex, className);
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
				childAssignRelation = getAssignRelationRecur(child, visitedNodes, varName, varIndex, className);
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
			// if the children all have status GUARANTEE_ASSIGN, and are assigning to the
			// same field, then the overall status is GUARANTEE_ASSIGN
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
	private AssignRelation getAssignRelationAtNode(CFGNode node, String varName, int varIndex, String className) {
		Instruction instruction = node.getInstructionHandle().getInstruction();

		if (instruction instanceof LoadInstruction) {
			LoadInstruction loadInstruction = (LoadInstruction) instruction;
			int fieldIndex = loadInstruction.getIndex();
			if (fieldIndex != varIndex) {
				return AssignRelation.getGuaranteeNoAssignRelation();
			}
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
				} else {
					return AssignRelation.getGuaranteeNoAssignRelation();
				}
			}
		} else if (instruction instanceof InvokeInstruction) {
			InvokeInstruction invokeInstruction = (InvokeInstruction) instruction;
			String methodName = invokeInstruction.getName(this.constantPoolGen);
			String methodSigature = invokeInstruction.getSignature(this.constantPoolGen);
			String invokingType = invokeInstruction.getClassName(this.constantPoolGen);

			if (!className.equals(invokingType)) {
				return AssignRelation.getGuaranteeNoAssignRelation();
			}

			// track the index of the parameter in method calls
			int numOfParameters = invokeInstruction.getArgumentTypes(constantPoolGen).length;
			int newIndex = numOfParameters + 1;
			CFGNode currentNode = node;
			for (int i = 0; i < numOfParameters; i++) {
				newIndex--;
				CFGNode parent = currentNode.getParents().get(0); // assume one parent
				Instruction previousInstruction = parent.getInstructionHandle().getInstruction();
				if (previousInstruction instanceof LoadInstruction) {
					LoadInstruction loadInstruction = (LoadInstruction) previousInstruction;
					int index = loadInstruction.getIndex();
					if (index == varIndex) {
						break;
					}
				}
				currentNode = parent;
			}
			if (newIndex == 0) {
				return AssignRelation.getGuaranteeNoAssignRelation();
			}

			try {
				CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(invokingType, methodName + methodSigature);
				if (cfg == null) {
					return AssignRelation.getNoGuaranteeAssignRelation();
				}
				AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg,
						invokingType + "#" + methodName + methodSigature);
				return aliasRelationsVerifier.getVarAssignRelation(varName, newIndex, className);
			} catch (CannotBuildCFGException e) {
				e.printStackTrace();
			}

			return AssignRelation.getNoGuaranteeAssignRelation();
		} else {
			return AssignRelation.getGuaranteeNoAssignRelation();
		}
	}

	public ReturnRelation getVarReturnRelation() {
		if (this.methodSignature.contains("<init>")) {
			String className = this.methodSignature.split("#")[0];
			String[] entries = className.split("\\.");
			String simpleClassName = entries[entries.length - 1];
			String returnedVariableName = simpleClassName + "_instance";
			return ReturnRelation.getGuaranteeReturnRelation(returnedVariableName);
		}
		return getReturnRelationRecur(cfg.getStartNode(), new HashMap<>());
	}

	private ReturnRelation getReturnRelationRecur(CFGNode node, Map<CFGNode, ReturnRelation> visitedNodes) {
		visitedNodes.put(node, null);

		ReturnRelation returnRelation = getReturnRelationAtNode(node);
		// only check the whole graph when status is GUARANTEE_NO_RETURN
		if (!returnRelation.getReturnStatus().equals(ReturnStatus.GUARANTEE_NO_RETURN)) {
			visitedNodes.put(node, returnRelation);
			return returnRelation;
		}

		ReturnRelation returnRelationAmongChildren = null;
		List<CFGNode> children = node.getChildren();
		for (CFGNode child : children) {
			ReturnRelation childReturnRelation;
			if (visitedNodes.containsKey(child)) {
				if (visitedNodes.get(child) == null) {
					continue;
				} else {
					childReturnRelation = visitedNodes.get(child);
				}
			} else {
				childReturnRelation = getReturnRelationRecur(child, visitedNodes);
			}

			// if status is NO_GUARANTEE at any point, the overall status is NO_GUARANTEE
			if (childReturnRelation.getReturnStatus().equals(ReturnStatus.NO_GUARANTEE)) {
				visitedNodes.put(node, childReturnRelation);
				return childReturnRelation;
			}

			if (returnRelationAmongChildren == null) {
				returnRelationAmongChildren = childReturnRelation;
			} else if (!returnRelationAmongChildren.equals(childReturnRelation)) {
				// if return status are different among branches, the overall status is
				// NO_GUARANTEE
				ReturnRelation noGuaranteeRelation = ReturnRelation.getNoGuaranteeReturnRelation();
				visitedNodes.put(node, noGuaranteeRelation);
				return noGuaranteeRelation;
			}
		}

		if (returnRelationAmongChildren == null) {
			// if there are no children, return the status of the current node
			visitedNodes.put(node, returnRelation);
			return returnRelation;
		} else if (returnRelationAmongChildren.getReturnStatus().equals(ReturnStatus.GUARANTEE_RETURN)) {
			// if the children all have status GUARANTEE_RETURN, then the overall status is
			// GUARANTEE_RETURN
			visitedNodes.put(node, returnRelationAmongChildren);
			return returnRelationAmongChildren;
		}

		visitedNodes.put(node, returnRelation);
		return returnRelation;
	}

	/**
	 * Analyze two nodes (one return step) at a time.
	 * Return Pattern: getfield return
	 */
	private ReturnRelation getReturnRelationAtNode(CFGNode node) {
		Instruction instruction = node.getInstructionHandle().getInstruction();

		if (instruction instanceof GETFIELD) {
			GETFIELD getFieldInstruction = (GETFIELD) instruction;
			String fieldName = getFieldInstruction.getFieldName(constantPoolGen);
			if (fieldName == null) {
				return ReturnRelation.getGuaranteeNoReturnRelation();
			}

			List<CFGNode> children = node.getChildren();
			boolean isLastNode = children == null || children.size() == 0;
			if (isLastNode) {
				return ReturnRelation.getGuaranteeNoReturnRelation();
			} else if (children.size() > 1) {
				return ReturnRelation.getNoGuaranteeReturnRelation();
			} else {
				Instruction nextInstruction = children.get(0).getInstructionHandle().getInstruction();
				if (nextInstruction instanceof ReturnInstruction) {
					return ReturnRelation.getGuaranteeReturnRelation(fieldName);
				}
			}
		} else if (instruction instanceof InvokeInstruction) {
			return ReturnRelation.getNoGuaranteeReturnRelation();
		} else {
			return ReturnRelation.getGuaranteeNoReturnRelation();
		}

		return ReturnRelation.getNoGuaranteeReturnRelation();
	}

}
