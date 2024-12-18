package microbat.instrumentation.ondemandtrace.tracestatus;

import org.apache.bcel.generic.MethodGen;

/**
 * @author HongshuW
 */
public class CodeBlockKeyIssuer {

	public static String getKeyForMethod(MethodGen method) {
		return method.getClassName() + "%" + method.getName() + method.getSignature();
	}

}
