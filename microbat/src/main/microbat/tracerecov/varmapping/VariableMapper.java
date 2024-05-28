package microbat.tracerecov.varmapping;

import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;

import microbat.tracerecov.TraceRecovUtils;

/**
 * This class performs static analysis and gets the returned variable.
 * 
 * @author hongshuwang
 */
public class VariableMapper {

	/**
	 * Given invokingMethod, get the return type of the candidate variable.
	 */
	public static String getReturnedField(String invokingMethod) {
		if (invokingMethod == null || invokingMethod.startsWith("%")) {
			return null;
		}

		String className = invokingMethod.split("#")[0];
		String methodSignature = invokingMethod.split("#")[1].split("%")[0];
		int index = methodSignature.indexOf("(");
		String methodName = methodSignature.substring(0, index);
		String methodDescriptor = methodSignature.substring(index);

		return getReturnedField(className, methodName, methodDescriptor, methodSignature);
	}

	private static String getReturnedField(String className, String methodName, String methodDescriptor,
			String methodSignature) {
		if (!TraceRecovUtils.shouldBeChecked(className)) {
			return null;
		}
		
		// load the class
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");

		try {
			ClassReader classReader = new ClassReader(inputStream);

			// create and accept a classVisitor
			VarMappingClassVisitor classVisitor = new VarMappingClassVisitor(className, methodName, methodDescriptor);
			classReader.accept(classVisitor, 0);

			return VarMappingMethodVisitor.getReturnedField();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
