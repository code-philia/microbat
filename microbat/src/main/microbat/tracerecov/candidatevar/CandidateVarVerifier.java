package microbat.tracerecov.candidatevar;

import java.util.List;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;

import microbat.codeanalysis.bytecode.CFG;
import microbat.codeanalysis.bytecode.CFGNode;

/**
 * This class performs static analysis and verifies whether a candidate variable
 * is guaranteed to be written or not written by a method call.
 * 
 * @author hongshuwang
 */
public class CandidateVarVerifier {

	private CFG cfg;
	private ConstantPool constantPool;

	public enum WriteStatus {
		GUARANTEE_WRITE, GUARANTEE_NO_WRITE, NO_GUARANTEE
	}

	public CandidateVarVerifier(CFG cfg) {
		this.cfg = cfg;
		this.constantPool = cfg.getConstantPool();
	}

	public WriteStatus getVarWriteStatus(String varName) {
		// TODO: record visited nodes
		return getWriteStatusRecur(cfg.getStartNode(), varName);
	}

	private WriteStatus getWriteStatusRecur(CFGNode node, String varName) {
		WriteStatus writeStatus = getWriteStatusAtNode(node, varName);
		// stop early when status is GUARANTEE_WRITE
		if (writeStatus != WriteStatus.GUARANTEE_NO_WRITE) {
			return writeStatus;
		}

		WriteStatus writeStatusAmongChildren = null;
		List<CFGNode> children = node.getChildren();
		for (CFGNode child : children) {
			WriteStatus childWriteStatus = getWriteStatusRecur(child, varName);
			// if status is NO_GUARANTEE at any point, the overall status is NO_GUARANTEE
			if (childWriteStatus == WriteStatus.NO_GUARANTEE) {
				return childWriteStatus;
			}

			if (writeStatusAmongChildren == null) {
				writeStatusAmongChildren = childWriteStatus;
			} else if (writeStatusAmongChildren != childWriteStatus) {
				// if write status are different among branches, the overall status is
				// NO_GUARANTEE
				return WriteStatus.NO_GUARANTEE;
			}
		}

		if (writeStatusAmongChildren == null) {
			// if there are no children, return the status of the current node
			return writeStatus;
		} else if (writeStatusAmongChildren == WriteStatus.GUARANTEE_WRITE) {
			// if the children all have status GUARANTEE_WRITE, then the overall status is
			// GUARANTEE_WRITE
			return writeStatusAmongChildren;
		}

		return writeStatus;
	}

	/**
	 * Write Status at one step can only be GUARANTEE_WRITE or GUARANTEE_NO_WRITE.
	 */
	private WriteStatus getWriteStatusAtNode(CFGNode node, String varName) {
		Instruction instruction = node.getInstructionHandle().getInstruction();

		if (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
			FieldInstruction putInstruction = (FieldInstruction) instruction;
			int varIndex = putInstruction.getIndex();
			Constant constant = this.constantPool.getConstant(varIndex);
			// TODO: if constant name == varName : guarantee write
			return WriteStatus.GUARANTEE_WRITE;
		} else if (instruction instanceof InvokeInstruction) {
			// TODO: expand method
			return WriteStatus.NO_GUARANTEE;
		} else {
			return WriteStatus.GUARANTEE_NO_WRITE;
		}
	}

//	private String className;
//	private ClassReader classReader;
//
//	public CandidateVarVerifier(String className) throws CandidateVarVerificationException {
//		if (!TraceRecovUtils.shouldBeChecked(className)) {
//			throw new CandidateVarVerificationException("class:" + className + " shouldn't be checked");
//		}
//		this.className = className;
//		this.classReader = this.loadClass(className);
//	}
//
//	private ClassReader loadClass(String className) throws CandidateVarVerificationException {
//		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
//		InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");
//		try {
//			return new ClassReader(inputStream);
//		} catch (IOException e) {
//			throw new CandidateVarVerificationException(className.replace('.', '/') + ".class is not found");
//		}
//	}
//
//	public WriteStatus verifyCandidateVariable(String invokingMethod, String fieldName)
//			throws CandidateVarVerificationException {
//		// invalid invoking method
//		if (invokingMethod == null || invokingMethod.startsWith("%")) {
//			throw new CandidateVarVerificationException("invalid invoking method: " + invokingMethod);
//		}
//
//		String declaredType = invokingMethod.split("#")[0];
//
//		// checking declaredType and runtimeType
//		if (isInvokingTypeValid(declaredType)) {
//			return WriteStatus.NO_GUARANTEE;
//		}
//
//		String methodSignature = invokingMethod.split("#")[1].split("%")[0];
//		int index = methodSignature.indexOf("(");
//		String methodName = methodSignature.substring(0, index);
//		String methodDescriptor = methodSignature.substring(index);
//
//		return verifyCandidateVariable(declaredType, methodName, methodDescriptor, methodSignature, fieldName);
//	}
//
//	private boolean isInvokingTypeValid(String declaredType) {
//		try {
//			Class<?> runtimeClass = Class.forName(className);
//			Class<?> declaredClass = Class.forName(declaredType);
//
//			Set<Class<?>> visitedClasses = new HashSet<>();
//			visitedClasses.add(runtimeClass);
//			
//			List<Class<?>> classesToCheck = new ArrayList<>();
//			classesToCheck.add(runtimeClass);
//			
//			while (!classesToCheck.isEmpty()) {
//				Class<?> currentClass = classesToCheck.remove(0);
//				if (declaredClass == currentClass) {
//					return true;
//				}
//				
//				if (currentClass == null) {
//					continue;
//				}
//				
//				if (!visitedClasses.contains(currentClass.getSuperclass())) {
//					visitedClasses.add(currentClass.getSuperclass());
//					classesToCheck.add(currentClass.getSuperclass());
//				}
//				Class<?>[] interfaces = currentClass.getInterfaces();
//				for (Class<?> i : interfaces) {
//					if (!visitedClasses.contains(i)) {
//						visitedClasses.add(i);
//						classesToCheck.add(i);
//					}
//				}
//			}
//			
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}
//		
//		return false;
//	}
//
//	private WriteStatus verifyCandidateVariable(String declaredType, String methodName, String methodDescriptor,
//			String methodSignature, String fieldName) {
//		// create and accept a classVisitor
//		CandidateVarClassVisitor classVisitor = new CandidateVarClassVisitor(className, methodName, methodDescriptor,
//				fieldName);
//		classReader.accept(classVisitor, 0);
//
//		return CandidateVarMethodVisitor.getWriteStatus();
//	}

}
