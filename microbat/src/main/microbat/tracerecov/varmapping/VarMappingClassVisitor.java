package microbat.tracerecov.varmapping;

import java.util.List;

import org.objectweb.asm.MethodVisitor;

import microbat.tracerecov.AbstractClassVisitor;

/**
 * This class defines a class visitor with {@link VarMappingMethodVisitor} as
 * method visitor.
 * 
 * @author hongshuwang
 */
public class VarMappingClassVisitor extends AbstractClassVisitor {

	private List<String> candidateVariables;

	public VarMappingClassVisitor(String className, String methodName, String methodDescriptor,
			List<String> candidateVariables) {
		super(className, methodName, methodDescriptor);
		this.candidateVariables = candidateVariables;
	}

	public VarMappingClassVisitor(String className, String methodName, String methodDescriptor, boolean reset,
			List<String> candidateVariables) {
		super(className, methodName, methodDescriptor, reset);
		this.candidateVariables = candidateVariables;
	}

	/**
	 * Only visit specific method.
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions) {
		if (name.equals(this.methodName) && descriptor.equals(this.methodDescriptor)) {
			if (this.reset) {
				VarMappingMethodVisitor.reset();
			}
			return new VarMappingMethodVisitor(this.candidateVariables);
		}
		return null;
	}

}
