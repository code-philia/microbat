package microbat.tracerecov.candidatevar;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This class checks whether a field is visited by a method call.
 * 
 * @author hongshuwang
 */
public class CandidateVarMethodVisitor extends MethodVisitor {
	private static boolean guaranteeWrite = false;
	private static boolean guaranteeNoWrite = false;
	private static Set<String> visitedMethods = new HashSet<>();
	private static boolean reachedControlBranch = false;

	private String fieldName;
	private Set<String> classesOfInterest;

	public CandidateVarMethodVisitor(Set<String> classesOfInterest, String fieldName) {
		super(Opcodes.ASM9, null);
		this.fieldName = fieldName;
		this.classesOfInterest = new HashSet<>();
		this.classesOfInterest.addAll(classesOfInterest);
	}

	/**
	 * Check put field instructions
	 */
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		super.visitFieldInsn(opcode, owner, name, descriptor);

		if (name.equals(this.fieldName) && isPutInstruction(opcode)) {
			if (!reachedControlBranch) {
				// field written before reaching control branch
				guaranteeWrite = true;
				guaranteeNoWrite = false;
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
		String className = owner.split("\\$")[0].replace('/', '.');
		if (classesOfInterest.contains(className) && !visitedMethods.contains(methodKey)) {
			visitedMethods.add(methodKey);
			visitMethod(owner, name, descriptor);
		}
	}

	private void visitMethod(String owner, String methodName, String methodDescriptor) {
		try {
			String className = owner.replace('/', '.');
			ClassReader classReader = new ClassReader(className);
			classReader.accept(new CandidateVarClassVisitor(className, methodName, methodDescriptor, fieldName, false),
					0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void reset() {
		guaranteeWrite = false;
		guaranteeNoWrite = false;
		visitedMethods = new HashSet<>();
		reachedControlBranch = false;
	}

	public static boolean guaranteeWrite() {
		return guaranteeWrite;
	}

	public static boolean guaranteeNoWrite() {
		return guaranteeNoWrite;
	}
}
