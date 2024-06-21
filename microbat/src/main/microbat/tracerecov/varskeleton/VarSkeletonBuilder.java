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
		
		
		// load the class
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");

		if(inputStream == null) {
			ClassLoader classLoader2 = appJavaClassPath.getClassLoader();
			inputStream = classLoader2.getResourceAsStream(className.replace('.', '/') + ".class");
		}
		
		System.currentTimeMillis();
		
		try {
			ClassReader classReader = new ClassReader(inputStream);

			// create and accept a classVisitor
			VarSkeletonClassVisitor classVisitor = new VarSkeletonClassVisitor(appJavaClassPath, className, true);
			classReader.accept(classVisitor, 0);
			
			System.currentTimeMillis();
			
			return classVisitor.getVariableStructure();
		} catch (IOException e) {
			// do nothing
//			e.printStackTrace();
		}
		
		return null;
	}

}
