package microbat.tracerecov.candidatevar;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import microbat.tracerecov.candidatevar.CandidateVarVerifier.WriteStatus;

/**
 * This class checks whether a field is visited by a method call.
 * 
 * @author hongshuwang
 */
public class CandidateVarMethodVisitor extends MethodVisitor {
	private static final int MAX_LAYER = 3;

	private static WriteStatus writeStatus = WriteStatus.NO_GUARANTEE;
	private static Set<String> visitedMethods = new HashSet<>();
	private static boolean reachedControlBranch = false;
	private static boolean visitedTargetField = false;

	private String fieldName;
	private int layer;

	public CandidateVarMethodVisitor(String fieldName, int layer) {
		super(Opcodes.ASM9, null);
		this.fieldName = fieldName;
		this.layer = layer;
	}

	/**
	 * Check put field instructions
	 */
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		super.visitFieldInsn(opcode, owner, name, descriptor);

		if (name.equals(this.fieldName) && isPutInstruction(opcode)) {
			visitedTargetField = true;
			if (!reachedControlBranch) {
				// field written before reaching control branch
				writeStatus = WriteStatus.GUARANTEE_WRITE;
			} else {
				// TODO: field written after reaching control branch
			}
		}
	}

	private boolean isPutInstruction(int opcode) {
		return opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC;
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		super.visitJumpInsn(opcode, label);
		// TODO: track branches
		reachedControlBranch = true;
	}

	/**
	 * Visit invoked methods in the current class and the parent class. Skip visited
	 * methods.
	 */
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		String methodKey = owner + "." + name + descriptor;
		if (!visitedMethods.contains(methodKey)) {
			visitedMethods.add(methodKey);
			visitMethod(owner, name, descriptor);
		}
	}

	private void visitMethod(String owner, String methodName, String methodDescriptor) {
		try {
			if (layer < MAX_LAYER && writeStatus == WriteStatus.NO_GUARANTEE) {
				String className = owner.replace('/', '.');
				ClassReader classReader = new ClassReader(className);
				classReader.accept(new CandidateVarClassVisitor(className, methodName, methodDescriptor, fieldName,
						layer + 1, false), 0);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void reset() {
		writeStatus = WriteStatus.NO_GUARANTEE;
		visitedMethods = new HashSet<>();
		reachedControlBranch = false;
		visitedTargetField = false;
	}

	public static WriteStatus getWriteStatus() {
		// TODO: implement a more complex version for guaranteeNoWrite
		if (writeStatus == WriteStatus.NO_GUARANTEE && !visitedTargetField) {
			writeStatus = WriteStatus.GUARANTEE_NO_WRITE;
		}

		return writeStatus;
	}
}
