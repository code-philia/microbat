package microbat.tracerecov.varexpansion;

import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;

/**
 * This class performs static analysis and determines the structure of a
 * variable given its type.
 * 
 * @author hongshuwang
 */
public class VarSkeletonBuilder {

	public static VariableSkeleton getVariableStructure(String className) {
		// load the class
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");

		try {
			ClassReader classReader = new ClassReader(inputStream);

			// create and accept a classVisitor
			VarExpansionClassVisitor classVisitor = new VarExpansionClassVisitor(className, true);
			classReader.accept(classVisitor, 0);
			
			return classVisitor.getVariableStructure();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
