package microbat.tracerecov;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Type;

/**
 * @author hongshuwang
 */
public class TraceRecovUtils {

	public static boolean isPrimitiveType(String className) {
		Set<String> primitiveTypes = new HashSet<>(
				Arrays.asList("int", "long", "short", "byte", "char", "boolean", "float", "double", "void"));
		return className != null && primitiveTypes.contains(className);
	}

	public static boolean isString(String className) {
		return className != null && (className.equals("java.lang.String") || className.equals("String"));
	}
	
	public static boolean isComposite(String className) {
		return !isPrimitiveType(className) && !isString(className);
	}

	public static boolean isArray(String className) {
		return className.endsWith("[]");
	}

	public static boolean shouldBeChecked(String className) {
		if (className == null || isPrimitiveType(className) || isString(className)
				|| className.equals("java.lang.Object")) {
			return false;
		}

		if (isArray(className)) {
			return true;
		}

		return className.contains(".");
	}
	
	public static boolean isCompositeType(String className) {
		if (className == null || isPrimitiveType(className) || className.equals("null")) {
			return false;
		}
		return className.contains(".");
	}

	public static String getTypeNameFromDescriptor(String descriptor) {
		if (descriptor == null) {
			return "null";
		}
		Type type = Type.getType(descriptor);
		return getReadableTypeName(type);
	}

	public static String getValidClassNameFromDescriptor(String descriptor) {
		Type type = Type.getType(descriptor);
		if (type.getSort() == Type.OBJECT) {
			return type.getInternalName().replace('/', '.');
		}
		return null;
	}

	private static String getReadableTypeName(Type type) {
		switch (type.getSort()) {
		case Type.VOID:
			return "void";
		case Type.BOOLEAN:
			return "boolean";
		case Type.CHAR:
			return "char";
		case Type.BYTE:
			return "byte";
		case Type.SHORT:
			return "short";
		case Type.INT:
			return "int";
		case Type.FLOAT:
			return "float";
		case Type.LONG:
			return "long";
		case Type.DOUBLE:
			return "double";
		case Type.ARRAY:
			StringBuilder arrayDescriptor = new StringBuilder();
			Type elementType = type.getElementType();
			arrayDescriptor.append(getReadableTypeName(elementType));
			for (int i = 0; i < type.getDimensions(); i++) {
				arrayDescriptor.append("[]");
			}
			return arrayDescriptor.toString();
		case Type.OBJECT:
			return type.getClassName().replace('/', '.');
		default:
			return type.getDescriptor();
		}
	}

	public static String getSourceCode(String filePath, int lineNumber) {
		String line = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			int currentLine = 0;
			while ((line = reader.readLine()) != null) {
				currentLine++;
				if (currentLine == lineNumber) {
					return line;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}