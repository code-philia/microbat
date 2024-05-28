package microbat.tracerecov.candidatevar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;

import microbat.tracerecov.TraceRecovUtils;

/**
 * This class performs static analysis and retrieves candidate variables given a
 * variable of interest.
 * 
 * @author hongshuwang
 */
public class CandidateVarRetriever {

	/**
	 * Given invokingMethod, return the variables that could have been read or
	 * written.
	 * 
	 * @param invokingMethod
	 * @return
	 */
	public static List<String> getCandidateVariables(String invokingMethod) {
		if (invokingMethod == null || invokingMethod.startsWith("%")) {
			return null;
		}

		String className = invokingMethod.split("#")[0];
		String methodSignature = invokingMethod.split("#")[1].split("%")[0];
		int index = methodSignature.indexOf("(");
		String methodName = methodSignature.substring(0, index);
		String methodDescriptor = methodSignature.substring(index);

		return getCandidateVariables(className, methodName, methodDescriptor, methodSignature);
	}

	private static List<String> getCandidateVariables(String className, String methodName, String methodDescriptor,
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
			CandidateVarClassVisitor classVisitor = new CandidateVarClassVisitor(className, methodName,
					methodDescriptor);
			classReader.accept(classVisitor, 0);

			Set<String> variables = CandidateVarMethodVisitor.getRelevantFields();
			return new ArrayList<>(variables);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
