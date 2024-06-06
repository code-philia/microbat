package microbat.tracerecov;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This class defines an abstract class visitor that visits the specific method
 * only.
 * 
 * @author hongshuwang
 */
public abstract class AbstractClassVisitor extends ClassVisitor {

	protected String methodName;
	protected String methodDescriptor;
	protected boolean reset;
	protected Set<String> classesOfInterest;
	protected boolean isAbstractOrInterface;
	
	public AbstractClassVisitor(String className, boolean reset) {
		this(className, "", "", reset);
	}

	public AbstractClassVisitor(String className, String methodName, String methodDescriptor) {
		this(className, methodName, methodDescriptor, true);
	}

	public AbstractClassVisitor(String className, String methodName, String methodDescriptor, boolean reset) {
		super(Opcodes.ASM9);
		this.classesOfInterest = new HashSet<>();
		this.classesOfInterest.add(className.replace('/', '.'));
		this.methodName = methodName;
		this.methodDescriptor = methodDescriptor;
		this.reset = reset;
		this.isAbstractOrInterface = false;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
		boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
		isAbstractOrInterface = isAbstract || isInterface;
	}

	@Override
	public abstract MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions);

}
