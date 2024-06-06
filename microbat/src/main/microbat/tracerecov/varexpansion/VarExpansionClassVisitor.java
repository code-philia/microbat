package microbat.tracerecov.varexpansion;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import microbat.tracerecov.AbstractClassVisitor;
import microbat.tracerecov.TraceRecovUtils;
import sav.strategies.dto.AppJavaClassPath;

public class VarExpansionClassVisitor extends AbstractClassVisitor {

	private static final int MAX_LAYER = 2;

	private static Set<String> visitedClasses = new HashSet<String>();

	private VariableSkeleton root;
	private String className;
	private int layer;
	private AppJavaClassPath appJavaClassPath;

	public VarExpansionClassVisitor(AppJavaClassPath appJavaClassPath, String className2, boolean b) {
		this(className2, new VariableSkeleton(className2), b);
		this.appJavaClassPath = appJavaClassPath;
	}
	
	public VarExpansionClassVisitor(String className) {
		this(className, false);
	}

	public VarExpansionClassVisitor(String className, boolean reset) {
		this(className, new VariableSkeleton(className), reset);
	}

	public VarExpansionClassVisitor(String className, VariableSkeleton root, boolean reset) {
		this(className, root, reset, 1);
	}

	public VarExpansionClassVisitor(String className, VariableSkeleton root, boolean reset, int layer) {
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
		this.layer = layer;
	}


	/**
	 * Record parent class in classesOfInterest.
	 */
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (superName == null) {
			return;
		}
		String newClassName = superName.replace('/', '.');
		if (TraceRecovUtils.shouldBeChecked(newClassName)) {
			classesOfInterest.add(newClassName);
		}
	}

	@Override
	public void visitEnd() {
		visitedClasses.add(this.className);
		for (String className : classesOfInterest) {
			if (!visitedClasses.contains(className) && layer < MAX_LAYER) {
				try {
					
					// load the class
					ClassLoader classLoader = ClassLoader.getSystemClassLoader();
					InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");

					if(inputStream == null) {
						ClassLoader classLoader2 = appJavaClassPath.getClassLoader();
						inputStream = classLoader2.getResourceAsStream(className.replace('.', '/') + ".class");
					}
					
					ClassReader classReader = new ClassReader(inputStream);
					classReader.accept(new VarExpansionClassVisitor(className, this.root, false, layer + 1), 0);
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
			VariableSkeleton child = new VariableSkeleton(TraceRecovUtils.getTypeNameFromDescriptor(descriptor), name);
			this.root.addChild(child);

			String newClassName = TraceRecovUtils.getValidClassNameFromDescriptor(descriptor);
			if (newClassName != null && TraceRecovUtils.shouldBeChecked(newClassName)
					&& !visitedClasses.contains(className) && layer < MAX_LAYER) {
				// expand inner field further
				try {
					ClassReader classReader = new ClassReader(newClassName);
					classReader.accept(new VarExpansionClassVisitor(newClassName, child, false, layer + 1), 0);
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

}
