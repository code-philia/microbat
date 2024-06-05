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

	private String fieldName;
	private int layer;

	public CandidateVarClassVisitor(String className, String methodName, String methodDescriptor, String fieldName) {
		this(className, methodName, methodDescriptor, fieldName, true);
	}

	public CandidateVarClassVisitor(String className, String methodName, String methodDescriptor, String fieldName,
			boolean reset) {
		this(className, methodName, methodDescriptor, fieldName, 1, reset);
	}

	public CandidateVarClassVisitor(String className, String methodName, String methodDescriptor, String fieldName,
			int layer, boolean reset) {
		super(className, methodName, methodDescriptor, reset);
		this.fieldName = fieldName;
		this.layer = layer;
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
			return new CandidateVarMethodVisitor(fieldName, layer);
		}
		return null;
	}

}
