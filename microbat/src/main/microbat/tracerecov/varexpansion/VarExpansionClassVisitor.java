package microbat.tracerecov.varexpansion;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import microbat.tracerecov.AbstractClassVisitor;

public class VarExpansionClassVisitor extends AbstractClassVisitor {
	
	private static Set<String> visitedClasses = new HashSet<String>();

	private VariableSkeleton root;
	private String className;

	public VarExpansionClassVisitor(String className, boolean reset) {
		super(className, reset);
		if (reset) {
			visitedClasses = new HashSet<String>();
		}
		this.root = new VariableSkeleton(className);
		this.className = className.replace('/', '.');
	}

	public VarExpansionClassVisitor(String className, VariableSkeleton root, boolean reset) {
		super(className, reset);
		if (reset) {
			visitedClasses = new HashSet<String>();
		}
		if (root == null) {
			this.root = new VariableSkeleton(className);
		} else {
			this.root = root;
		}
		this.className = className.replace('/', '.');
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

	@Override
	public void visitEnd() {
		visitedClasses.add(this.className);
		for (String className : classesOfInterest) {
			if (!visitedClasses.contains(className)) {
				try {
					ClassReader classReader = new ClassReader(className);
					classReader.accept(new VarExpansionClassVisitor(className, this.root, false), 0);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		// exclude static fields
		if ((access & Opcodes.ACC_STATIC) == 0) {
			VariableSkeleton child = new VariableSkeleton(descriptor, name);
			this.root.addChild(child);
			
			String newClassName = getClassNameFromDescriptor(descriptor);
			if (newClassName != null) {
				// expand inner field further
				try {
					ClassReader classReader = new ClassReader(newClassName);
					classReader.accept(new VarExpansionClassVisitor(newClassName, child, false), 0);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions) {
		return null;
	}

	public VariableSkeleton getVariableStructure() {
		return this.root;
	}
	
	private static String getClassNameFromDescriptor(String descriptor) {
        Type type = Type.getType(descriptor);
        if (type.getSort() == Type.OBJECT) {
            return type.getInternalName();
        }
        return null;
    }
}
