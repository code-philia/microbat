package microbat.tracerecov.staticverifiers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;

import microbat.codeanalysis.bytecode.CFG;
import microbat.codeanalysis.bytecode.CFGNode;
import microbat.tracerecov.CannotBuildCFGException;
import microbat.tracerecov.TraceRecovUtils;

/**
 * This class performs static analysis and verifies whether a candidate variable
 * is guaranteed to be written or not written by a method call.
 * 
 * @author hongshuwang
 */
public class CandidateVarVerifier {

	private CFG cfg;
	private ConstantPoolGen constantPoolGen;

	public CandidateVarVerifier(CFG cfg) {
		this.cfg = cfg;
		this.constantPoolGen = new ConstantPoolGen(cfg.getConstantPool());
	}

	public WriteStatus getVarWriteStatus(String varName) {
		if (varName.contains("[") || varName.contains("]")) {
			return WriteStatus.NO_GUARANTEE;
		}
		return getWriteStatusRecur(cfg.getStartNode(), new HashMap<>(), varName);
	}

	private WriteStatus getWriteStatusRecur(CFGNode node, Map<CFGNode, WriteStatus> visitedNodes, String varName) {
		visitedNodes.put(node, null);

		WriteStatus writeStatus = getWriteStatusAtNode(node, varName);
		// only check the whole graph when status is GUARANTEE_NO_WRITE
		if (writeStatus != WriteStatus.GUARANTEE_NO_WRITE) {
			visitedNodes.put(node, writeStatus);
			return writeStatus;
		}

		WriteStatus writeStatusAmongChildren = null;
		List<CFGNode> children = node.getChildren();
		for (CFGNode child : children) {
			WriteStatus childWriteStatus;
			if (visitedNodes.containsKey(child)) {
				if (visitedNodes.get(child) == null) {
					continue;
				} else {
					childWriteStatus = visitedNodes.get(child);
				}
			} else {
				childWriteStatus = getWriteStatusRecur(child, visitedNodes, varName);
			}

			// if status is NO_GUARANTEE at any point, the overall status is NO_GUARANTEE
			if (childWriteStatus == WriteStatus.NO_GUARANTEE) {
				visitedNodes.put(node, childWriteStatus);
				return childWriteStatus;
			}

			if (writeStatusAmongChildren == null) {
				writeStatusAmongChildren = childWriteStatus;
			} else if (writeStatusAmongChildren != childWriteStatus) {
				// if write status are different among branches, the overall status is
				// NO_GUARANTEE
				visitedNodes.put(node, WriteStatus.NO_GUARANTEE);
				return WriteStatus.NO_GUARANTEE;
			}
		}

		if (writeStatusAmongChildren == null) {
			// if there are no children, return the status of the current node
			visitedNodes.put(node, writeStatus);
			return writeStatus;
		} else if (writeStatusAmongChildren == WriteStatus.GUARANTEE_WRITE) {
			// if the children all have status GUARANTEE_WRITE, then the overall status is
			// GUARANTEE_WRITE
			visitedNodes.put(node, writeStatusAmongChildren);
			return writeStatusAmongChildren;
		}

		visitedNodes.put(node, writeStatus);
		return writeStatus;
	}

	/**
	 * Write Pattern: putfield|putstatic
	 */
	private WriteStatus getWriteStatusAtNode(CFGNode node, String varName) {
		Instruction instruction = node.getInstructionHandle().getInstruction();

		if (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
			FieldInstruction putInstruction = (FieldInstruction) instruction;
			String fieldName = putInstruction.getFieldName(this.constantPoolGen);
			if (fieldName.equals(varName)) {
				return WriteStatus.GUARANTEE_WRITE;
			} else {
				return WriteStatus.GUARANTEE_NO_WRITE;
			}
		} else if (instruction instanceof INVOKEVIRTUAL || instruction instanceof INVOKESPECIAL) {
			InvokeInstruction invokeInstruction = (InvokeInstruction) instruction;
			String methodName = invokeInstruction.getName(this.constantPoolGen);
			String methodSigature = invokeInstruction.getSignature(this.constantPoolGen);
			String invokingType = invokeInstruction.getClassName(this.constantPoolGen);

			try {
				CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(invokingType, methodName + methodSigature);
				if (cfg == null) {
					return WriteStatus.NO_GUARANTEE;
				}
				CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
				return candidateVarVerifier.getVarWriteStatus(varName);
			} catch (CannotBuildCFGException e) {
				e.printStackTrace();
			}
		} else if (instruction instanceof InvokeInstruction) {
			return WriteStatus.NO_GUARANTEE;
		} else {
			return WriteStatus.GUARANTEE_NO_WRITE;
		}

		return WriteStatus.NO_GUARANTEE;
	}

}
