package microbat.instrumentation.ondemandtrace.tracestatus;

import org.apache.bcel.generic.MethodGen;

/**
 * This class assigns unique key to code blocks.
 * 
 * @author HongshuW
 */
public class CodeBlockKeyIssuer {

	public static String getKeyForMethod(MethodGen method) {
		return getKeyForMethod(method.getClassName(), method.getName(), method.getSignature());
	}

	public static String getKeyForMethod(String className, String methodName, String methodSignature) {
		return getClassNameWithCorrectFormat(className) + "#" + methodName + methodSignature;
	}

	public static String getKeyForMethod(String className, String methodNameAndSignature) {
		return getClassNameWithCorrectFormat(className) + "#" + methodNameAndSignature;
	}

	private static String getClassNameWithCorrectFormat(String className) {
		return className.replace('.', '/');
	}

}
