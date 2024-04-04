package microbat.instrumentation.utils;

import org.apache.bcel.classfile.Method;

public class MicrobatUtils {
	
	public static String getMicrobatMethodFullName(String className, Method method) {
		StringBuilder sb = new StringBuilder();
		sb.append(className).append("#").append(method.getName())
			.append(method.getSignature().replace(";", ":"));
		return sb.toString();
	}
	
	public static boolean checkTestResult(String msg) {
		int sIdx = msg.indexOf(";");
		if (sIdx < 0 || msg.length() < sIdx) {
			return false;
		}
		return Boolean.valueOf(msg.substring(0, sIdx));
	}
}
