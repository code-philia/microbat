package microbat.tracerecov.candidatevar;

import org.objectweb.asm.MethodVisitor;

import microbat.tracerecov.AbstractClassVisitor;

/**
 * This class defines a class visitor with {@link CandidateVarMethodVisitor} as
 * method visitor.
 * 
 * @author hongshuwang
 */
public class CandidateVarClassVisitor extends AbstractClassVisitor {

	public CandidateVarClassVisitor(String className, String methodName, String methodDescriptor) {
		super(className, methodName, methodDescriptor);
	}

	public CandidateVarClassVisitor(String className, String methodName, String methodDescriptor, boolean reset) {
		super(className, methodName, methodDescriptor, reset);
	}

	/**
	 * Record parent class in classesOfInterest.
	 */
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (superName == null) {
			return;
		}
		classesOfInterest.add(superName.replace('/', '.'));
	}

	/**
	 * Only visit specific method.
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions) {
		if (name.equals(this.methodName) && descriptor.equals(this.methodDescriptor)) {
			if (reset) {
				CandidateVarMethodVisitor.reset();
			}
			return new CandidateVarMethodVisitor(classesOfInterest);
		}
		return null;
	}

}
