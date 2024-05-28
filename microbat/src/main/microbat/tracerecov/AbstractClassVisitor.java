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
	
	public AbstractClassVisitor(String className, boolean reset) {
		super(Opcodes.ASM9);
		this.classesOfInterest = new HashSet<>();
		this.classesOfInterest.add(className.replace('/', '.'));
		this.methodName = "";
		this.methodDescriptor = "";
		this.reset = reset;
	}

	public AbstractClassVisitor(String className, String methodName, String methodDescriptor) {
		super(Opcodes.ASM9);
		this.classesOfInterest = new HashSet<>();
		this.classesOfInterest.add(className.replace('/', '.'));
		this.methodName = methodName;
		this.methodDescriptor = methodDescriptor;
		this.reset = true;
	}

	public AbstractClassVisitor(String className, String methodName, String methodDescriptor, boolean reset) {
		super(Opcodes.ASM9);
		this.classesOfInterest = new HashSet<>();
		this.classesOfInterest.add(className.replace('/', '.'));
		this.methodName = methodName;
		this.methodDescriptor = methodDescriptor;
		this.reset = reset;
	}

	@Override
	public abstract MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions);

}
