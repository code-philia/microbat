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
		return className + "%" + methodName + methodSignature;
	}

}
