package microbat.tracerecov;

import org.objectweb.asm.Type;

public class TraceRecovUtils {

	public static boolean shouldBeChecked(String className) {
		if (className == null) {
			return false;
		}
		
		switch (className) {
		case "int":
		case "long":
		case "short":
		case "byte":
		case "char":
		case "boolean":
		case "float":
		case "double":
		case "void":
			return false;
		}

		// Check if the type is an array type
		if (className.endsWith("[]")) {
			return true;
		}
		
		if (className.equals("java.lang.String") || className.equals("java.lang.Object")) {
			return false;
		}

		return className.contains(".");
	}

	public static String getClassNameFromDescriptor(String descriptor) {
		Type type = Type.getType(descriptor);
		if (type.getSort() == Type.OBJECT) {
			return type.getInternalName().replace('/', '.');
		}
		return null;
	}

}
