package microbat.tracerecov.varskeleton;

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

public class VarSkeletonClassVisitor extends AbstractClassVisitor {

	private static final int MAX_LAYER = 3;

	private static Set<String> visitedClasses = new HashSet<String>();

	private VariableSkeleton root;
	private String className;
	private int layer;
	private AppJavaClassPath appJavaClassPath;

	public VarSkeletonClassVisitor(AppJavaClassPath appJavaClassPath, String className2, boolean reset) {
		this(className2, new VariableSkeleton(className2), reset);
		this.appJavaClassPath = appJavaClassPath;
	}

	public VarSkeletonClassVisitor(String className) {
		this(className, false);
	}

	public VarSkeletonClassVisitor(String className, boolean reset) {
		this(className, new VariableSkeleton(className), reset);
	}

	public VarSkeletonClassVisitor(String className, VariableSkeleton root, boolean reset) {
		this(className, root, reset, 1);
	}

	public VarSkeletonClassVisitor(String className, VariableSkeleton root, boolean reset, int layer) {
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
			if (!visitedClasses.contains(className)) {
				try {
					// load the class
					ClassLoader classLoader = ClassLoader.getSystemClassLoader();
					InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");

					if (inputStream == null) {
						if (appJavaClassPath == null) {
							continue;
						}
						ClassLoader classLoader2 = appJavaClassPath.getClassLoader();
						inputStream = classLoader2.getResourceAsStream(className.replace('.', '/') + ".class");
					}

					ClassReader classReader = new ClassReader(inputStream);
					classReader.accept(new VarSkeletonClassVisitor(className, this.root, false, this.layer), 0);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		// exclude static and final fields
		if ((access & Opcodes.ACC_STATIC) == 0 && (access & Opcodes.ACC_FINAL) == 0) {
			VariableSkeleton child = new VariableSkeleton(TraceRecovUtils.getTypeNameFromDescriptor(descriptor), name);
			this.root.addChild(child);

			String newClassName = TraceRecovUtils.getValidClassNameFromDescriptor(descriptor);
			if (newClassName != null && TraceRecovUtils.shouldBeChecked(newClassName)
					&& !visitedClasses.contains(className) && layer < MAX_LAYER) {
				// load the class
				ClassLoader classLoader = ClassLoader.getSystemClassLoader();
				InputStream inputStream = classLoader.getResourceAsStream(newClassName.replace('.', '/') + ".class");

				if (inputStream == null) {
					if (appJavaClassPath == null) {
						return null;
					}
					ClassLoader classLoader2 = appJavaClassPath.getClassLoader();
					inputStream = classLoader2.getResourceAsStream(newClassName.replace('.', '/') + ".class");
				}
				
				// expand inner fields further
				try {
					ClassReader classReader = null;
					if (inputStream == null) {
						classReader = new ClassReader(newClassName);
					} else {
						classReader = new ClassReader(inputStream);
					}
					if (classReader != null) {
						classReader.accept(new VarSkeletonClassVisitor(newClassName, child, false, layer + 1), 0);
					}
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
