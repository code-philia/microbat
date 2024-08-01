package microbat.tracerecov;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.objectweb.asm.Type;

import microbat.Activator;
import microbat.codeanalysis.bytecode.CFG;
import microbat.codeanalysis.bytecode.CFGConstructor;
import microbat.model.value.VarValue;
import microbat.preference.MicrobatPreference;
import sav.strategies.dto.AppJavaClassPath;

/**
 * @author hongshuwang
 */
public class TraceRecovUtils {

	public static boolean isPrimitiveType(String className) {
		Set<String> primitiveTypes = new HashSet<>(
				Arrays.asList("int", "long", "short", "byte", "char", "boolean", "float", "double", "void"));
		return className != null && primitiveTypes.contains(className);
	}

	public static String getWrapperType(String primitiveTypeName) {
		if (primitiveTypeName.equals("int")) {
			return "java.lang.Integer";
		} else if (primitiveTypeName.equals("long")) {
			return "java.lang.Long";
		} else if (primitiveTypeName.equals("short")) {
			return "java.lang.Short";
		} else if (primitiveTypeName.equals("byte")) {
			return "java.lang.Byte";
		} else if (primitiveTypeName.equals("char")) {
			return "java.lang.Character";
		} else if (primitiveTypeName.equals("boolean")) {
			return "java.lang.Boolean";
		} else if (primitiveTypeName.equals("float")) {
			return "java.lang.Float";
		} else if (primitiveTypeName.equals("double")) {
			return "java.lang.Double";
		}
		return "";
	}

	public static boolean isString(String className) {
		return className != null && (className.equals("java.lang.String") || className.equals("String"));
	}

	public static boolean isArray(String className) {
		return className.endsWith("[]");
	}

	public static boolean isIterator(String className) {
		return className.endsWith("Itr") && className.contains("$");
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

	public static boolean isUnrecorded(String type, AppJavaClassPath appJavaClassPath) {
		List<String> pathsToCheck = new ArrayList<>();
		pathsToCheck.add(appJavaClassPath.getSoureCodePath());
		pathsToCheck.add(appJavaClassPath.getTestCodePath());
		pathsToCheck.addAll(appJavaClassPath.getAdditionalSourceFolders());

		for (String sourcePath : pathsToCheck) {
			if (isClassInFolder(sourcePath, type)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isClassInFolder(String folderPath, String className) {
		if(folderPath == null) {
			return false;
		}
		
		String fileName = className.replace(".", File.separator).concat(".java");
		Path filePath = Paths.get(folderPath, fileName);

		return Files.exists(filePath);
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

	public static String processInputStringForLLM(String input) {
//		return input.replace("\n", "\\n").replace("\r", "\\r").replace("<>", "\\<\\>");
		return input.replace("\n", "\\n").replace("\r", "\\r");
	}

	public static CFG getCFGFromMethodSignature(String className, String methodSig) throws CannotBuildCFGException {
		className = className.replace(".", "/");

		String rtJarPath = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.JAVA7HOME_PATH)
				+ File.separator + "jre" + File.separator + "lib" + File.separator + "rt.jar";

		try (JarFile rtJar = new JarFile(rtJarPath)) {
			// Get class
			String resourcePath = className + ".class";
			ZipEntry entry = rtJar.getEntry(resourcePath);

			if (entry == null) {
				throw new CannotBuildCFGException("Class: `" + className + "` is not found in " + rtJarPath);
			}

			ClassParser parser = new ClassParser(rtJar.getInputStream(entry), className);
			JavaClass javaClass = parser.parse();

			// Find target method
			Method targetMethod = null;
			for (Method method : javaClass.getMethods()) {
				String methodSignature = method.getName() + method.getSignature();
				if (methodSignature.equals(methodSig)) {
					targetMethod = method;
					break;
				}
			}

			if (targetMethod == null) {
				throw new CannotBuildCFGException(
						"Method `" + methodSig + "` is not found in class `" + className + "`");
			}

			// Build CFG
			Code code = targetMethod.getCode();
			if (code == null) {
				throw new CannotBuildCFGException(
						"Bytecode is not available in `" + className + ": " + methodSig + "`");
			}
			CFGConstructor cfgConstructor = new CFGConstructor();
			CFG cfg = cfgConstructor.buildCFGWithControlDomiance(code);

			return cfg;
		} catch (IOException e) {
			throw new CannotBuildCFGException(e.getMessage());
		}
	}

	public static CFG getCFGFromMethodSignature(String invokedMethod) throws CannotBuildCFGException {
		if (!invokedMethod.contains("#")) {
			throw new CannotBuildCFGException(
					"invoked method: `" + invokedMethod + "` doesn't follow format `type#method`");
		}

		String className = invokedMethod.split("#")[0];
		String methodSig = invokedMethod.split("#")[1];

		return getCFGFromMethodSignature(className, methodSig);
	}

	public static Set<String> getInvokedMethodsToBeChecked(String invokingMethods) {
		Set<String> methods = new HashSet<>();

		String[] invokedMethods = invokingMethods.split("%");
		for (String methodSig : invokedMethods) {
			if (methodSig == null || !methodSig.contains("#")) {
				continue;
			}
			String type = methodSig.split("#")[0];
			if (shouldBeChecked(type)) {
				methods.add(methodSig);
			}
		}

		return methods;
	}

	public static boolean isAssignable(VarValue variableWithGeneralType, VarValue variableWithSpecificType,
			AppJavaClassPath appJavaClassPath) {
		String generalType = variableWithGeneralType.getType();
		if (generalType.contains("class ")) {
			generalType = generalType.split(" ")[1];
		}
		// element in array doesn't have an inferred type (TODO: type of each element?)
		if (generalType == null || generalType.equals("")) {
			return true;
		}

		String specificType = variableWithSpecificType.getType();
		if (specificType.contains("class ")) {
			specificType = specificType.split(" ")[1];
		}
		if (!isUnrecorded(generalType, appJavaClassPath) || !isUnrecorded(specificType, appJavaClassPath)
				|| specificType == null) {
			return false;
		}

		// primitive types: must be the same
		if (isPrimitiveType(generalType) || isString(generalType) || isString(specificType)) {
			return generalType.equals(specificType);
		}

		if (isPrimitiveType(specificType)) {
			return generalType.equals(specificType) || generalType.equals(getWrapperType(specificType));
		}

		// composite types: general type must be a parent of specific type
		List<String> classesToCheck = new ArrayList<>();
		Set<String> visitedClasses = new HashSet<>();
		classesToCheck.add(specificType);
		while (!classesToCheck.isEmpty()) {
			String typeToCheck = classesToCheck.remove(0);
			visitedClasses.add(typeToCheck);

			if (generalType.equals(typeToCheck)) {
				return true;
			}

			try {
				Class<?> clazz = Class.forName(typeToCheck);

				Class<?> superClass = clazz.getSuperclass();
				if (superClass != null) {
					String superClassName = superClass.getName();
					if (!visitedClasses.contains(superClassName) && shouldBeChecked(superClassName)) {
						classesToCheck.add(superClassName);
					}
				}

				Class<?>[] interfaces = clazz.getInterfaces();
				for (Class<?> i : interfaces) {
					String interfaceName = i.getName();
					if (!visitedClasses.contains(interfaceName) && shouldBeChecked(interfaceName)) {
						classesToCheck.add(interfaceName);
					}
				}

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return false;
	}
	

	public static String[] parseArrayFromString(String stringValue) {
		stringValue = stringValue.substring(1, stringValue.length() - 1);
		return stringValue.split(",");
	}
}
