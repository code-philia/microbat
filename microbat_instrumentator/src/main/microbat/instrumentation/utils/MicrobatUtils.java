package microbat.instrumentation.utils;

import org.apache.bcel.classfile.Method;

public class MicrobatUtils {
	
	public static String getMicrobatMethodFullName(String className, Method method) {
		return getMicrobatMethodFullName(className, method.getName(), method.getSignature());
	}
	
	public static String getMicrobatMethodFullName(String className, String methodName, String methodSignature) {
		StringBuilder sb = new StringBuilder();
		sb.append(className).append("#").append(methodName).append(methodSignature.replace(";", ":"));
		return sb.toString();
	}
}
