package microbat.tracerecov;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.objectweb.asm.ClassReader;

public class VariableMapper {

	/**
	 * Given invokingMethod, map the candidate variables.
	 * 
	 * @param invokingMethod
	 * @return
	 */
	public static void mapVariables(String invokingMethod, List<String> candidateVariables) {
		if (invokingMethod == null || invokingMethod.startsWith("%")) {
			return;
		}
		
		String className = invokingMethod.split("#")[0];
		String methodSignature = invokingMethod.split("#")[1].split("%")[0];
		int index = methodSignature.indexOf("(");
		String methodName = methodSignature.substring(0, index);
		String methodDescriptor = methodSignature.substring(index);
		
		mapVariables(className, methodName, methodDescriptor, methodSignature, candidateVariables);
	}
	
	private static void mapVariables(String className, String methodName, String methodDescriptor, String methodSignature, List<String> candidateVariables) {
		// load the class
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");
		
		try {
			ClassReader classReader = new ClassReader(inputStream);
			
			// create and accept a classVisitor
			VarMappingClassVisitor classVisitor = new VarMappingClassVisitor(className, methodName, methodDescriptor);
			classReader.accept(classVisitor, 0);
			
			String fieldName = VarMappingMethodVisitor.getReturnedField();
			
			System.out.println();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
