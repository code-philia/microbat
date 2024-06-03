package microbat.tracerecov.candidatevar;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This class records relevant fields visited by a method.
 * 
 * @author hongshuwang
 */
public class CandidateVarMethodVisitor extends MethodVisitor {
	private static Set<String> relevantFields = new HashSet<>();
	private static Set<String> visitedMethods = new HashSet<>();

	private Set<String> classesOfInterest;

	public CandidateVarMethodVisitor(Set<String> classesOfInterest) {
		super(Opcodes.ASM9, null);
		this.classesOfInterest = new HashSet<>();
		this.classesOfInterest.addAll(classesOfInterest);
	}

	/**
	 * Record visited fields.
	 */
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		super.visitFieldInsn(opcode, owner, name, descriptor);
		if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
			relevantFields.add(name);
		}
	}

	/**
	 * Visit invoked methods in the current class and the parent class. Skip visited
	 * methods.
	 */
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		String methodKey = owner + "." + name + descriptor;
		if (classesOfInterest.contains(owner.replace('/', '.')) && !visitedMethods.contains(methodKey)) {
			visitedMethods.add(methodKey);
			visitMethod(owner, name, descriptor);
		}
	}

	private void visitMethod(String owner, String methodName, String methodDescriptor) {
		try {
			String className = owner.replace('/', '.');
			ClassReader classReader = new ClassReader(className);
			classReader.accept(new CandidateVarClassVisitor(className, methodName, methodDescriptor, false), 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reset relevant fields and visited methods to be an empty sets. Consider
	 * variable v with candidate variables to be identified, this method should be
	 * called for each v.
	 */
	public static void reset() {
		relevantFields = new HashSet<>();
		visitedMethods = new HashSet<>();
	}

	public static Set<String> getRelevantFields() {
		return relevantFields;
	}

}
