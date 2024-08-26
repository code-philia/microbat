package microbat.tracerecov.varskeleton;

import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;

import microbat.tracerecov.TraceRecovUtils;
import sav.strategies.dto.AppJavaClassPath;

/**
 * This class performs static analysis and determines the structure of a
 * variable given its type.
 * 
 * @author hongshuwang
 */
public class VarSkeletonBuilder {

	public static VariableSkeleton getVariableStructure(String className, AppJavaClassPath appJavaClassPath) {
		if (!TraceRecovUtils.shouldBeChecked(className)) {
			return null;
		}
		
		if (TraceRecovUtils.isArray(className)) {
			return new VariableSkeleton(className);
		}

		// load the class
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");

		if (inputStream == null) {
			if (appJavaClassPath == null) {
				return null;
			}
			ClassLoader classLoader2 = appJavaClassPath.getClassLoader();
			inputStream = classLoader2.getResourceAsStream(className.replace('.', '/') + ".class");
		}
		
		try {
			// create and accept a classVisitor
			ClassReader classReader = new ClassReader(inputStream);
			VarSkeletonClassVisitor classVisitor = new VarSkeletonClassVisitor(appJavaClassPath, className, true);
			classReader.accept(classVisitor, 0);

			return classVisitor.getVariableStructure();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
