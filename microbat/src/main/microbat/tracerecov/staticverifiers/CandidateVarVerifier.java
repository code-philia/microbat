package microbat.tracerecov.staticverifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.INVOKESTATIC;
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

	public WriteStatus getVarWriteStatus(String varName, String methodSignature) {
		// extract variable names
		ArrayList<String> varNames = new ArrayList<>();
		if (varName.contains(".")) {
			String[] entries = varName.split("\\.");
			for (String entry : entries) {
				varNames.add(entry);
			}
		} else {
			varNames.add(varName);
		}

		return getVarWriteStatus(varNames, methodSignature);
	}

	private WriteStatus getVarWriteStatus(List<String> varNames, String methodSignature) {
		// edge cases
		String lastVarName = varNames.get(varNames.size() - 1);
		if (lastVarName.contains("[") || lastVarName.contains("]")) {
			return WriteStatus.NO_GUARANTEE;
		}

		String firstVarName = varNames.get(0);
		if (methodSignature.contains("<init>")) {
			try {
				String className = methodSignature.split("#")[0];
				JavaClass javaClass = Repository.lookupClass(className);
				Field[] fields = javaClass.getFields();
				for (Field field : fields) {
					if (field.getName().equals(firstVarName)) {
						return WriteStatus.GUARANTEE_WRITE;
					}
				}
				return WriteStatus.GUARANTEE_NO_WRITE;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return WriteStatus.NO_GUARANTEE;
			}
		}

		return getWriteStatusRecur(cfg.getStartNode(), new HashMap<>(), varNames, methodSignature);
	}

	private WriteStatus getWriteStatusRecur(CFGNode node, Map<CFGNode, WriteStatus> visitedNodes, List<String> varNames,
			String methodSignature) {
		visitedNodes.put(node, null);

		WriteStatus writeStatus = getWriteStatusAtNode(node, varNames, methodSignature);
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
				childWriteStatus = getWriteStatusRecur(child, visitedNodes, varNames, methodSignature);
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
	private WriteStatus getWriteStatusAtNode(CFGNode node, List<String> varNames, String methodSignature) {
		Instruction instruction = node.getInstructionHandle().getInstruction();
		String varName = varNames.get(0);

		if (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
			FieldInstruction putInstruction = (FieldInstruction) instruction;
			String fieldName = putInstruction.getFieldName(this.constantPoolGen);
			if (fieldName.equals(varName)) {
				return WriteStatus.GUARANTEE_WRITE;
			} else {
				return WriteStatus.GUARANTEE_NO_WRITE;
			}
		} else if (instruction instanceof InvokeInstruction && !(instruction instanceof INVOKESTATIC)) {
			InvokeInstruction invokeInstruction = (InvokeInstruction) instruction;

			// check whether the method is from (1) this class or super class (2) an
			// internal field
			int numOfParameters = invokeInstruction.getArgumentTypes(constantPoolGen).length;
			CFGNode currentNode = node;
			int index = 0;
			while (index <= numOfParameters) {
				currentNode = currentNode.getParents().get(0); // assume one parent
				Instruction ins = currentNode.getInstructionHandle().getInstruction();
				if (ins instanceof InvokeInstruction) {
					InvokeInstruction invokeIns = (InvokeInstruction) ins;
					numOfParameters += invokeIns.getArgumentTypes(constantPoolGen).length;
				}
				index++;
			}
			Instruction previousInstruction = currentNode.getInstructionHandle().getInstruction();
			if (previousInstruction instanceof ALOAD) {
				ALOAD loadObjectInstruction = (ALOAD) previousInstruction;
				if (loadObjectInstruction.getOpcode() == Const.ALOAD_0) {
					// (1) method is invoked from this class or super class
					return getVarWriteStatusFromInvokeInstruction(invokeInstruction, varNames);
				}
			} else if (previousInstruction instanceof GETFIELD) {
				GETFIELD getFieldInstruction = (GETFIELD) previousInstruction;
				String invokingObjectName = getFieldInstruction.getFieldName(constantPoolGen);
				if (invokingObjectName.equals(varName) && varNames.size() > 1) {
					// (2) method is invoked from an internal field
					varNames.remove(0);
					WriteStatus writeStatus = getVarWriteStatusFromInvokeInstruction(invokeInstruction, varNames);
					varNames.add(0, varName);
					return writeStatus;
				}
			}
			return WriteStatus.GUARANTEE_NO_WRITE;
		} else {
			return WriteStatus.GUARANTEE_NO_WRITE;
		}
	}

	private WriteStatus getVarWriteStatusFromInvokeInstruction(InvokeInstruction invokeInstruction,
			List<String> varNames) {
		String methodName = invokeInstruction.getName(this.constantPoolGen);
		String methodSignature = invokeInstruction.getSignature(this.constantPoolGen);
		String invokingType = invokeInstruction.getClassName(this.constantPoolGen);

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(invokingType, methodName + methodSignature);
			if (cfg == null) {
				return WriteStatus.NO_GUARANTEE;
			}
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			return candidateVarVerifier.getVarWriteStatus(varNames, invokingType + "#" + methodName + methodSignature);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
			return WriteStatus.NO_GUARANTEE;
		}
	}

}
