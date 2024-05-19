package microbat.tracerecov;

import org.objectweb.asm.MethodVisitor;

/**
 * This class defines a class visitor with {@link VarMappingMethodVisitor} as method visitor.
 * 
 * @author hongshuwang
 */
public class VarMappingClassVisitor extends AbstractClassVisitor {

	public VarMappingClassVisitor(String className, String methodName, String methodDescriptor) {
		super(className, methodName, methodDescriptor);
	}
	
	public VarMappingClassVisitor(String className, String methodName, String methodDescriptor, boolean reset) {
		super(className, methodName, methodDescriptor, reset);
	}
	
	/**
	 * Only visit specific method.
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (name.equals(this.methodName) && descriptor.equals(this.methodDescriptor)) {
			if (reset) {
				VarMappingMethodVisitor.reset();
			}
			return new VarMappingMethodVisitor();
		}
		return null;
	}

}
