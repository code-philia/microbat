package microbat.instrumentation.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.Type;
import org.apache.commons.io.IOUtils;

import microbat.model.value.VarValue;
import microbat.util.PrimitiveUtils;

public class QueryRequestGenerator {

	public QueryRequestGenerator() {}
	
	public static String getQueryRequest(String methodSig, String runtimeType) throws IOException {
		String decompiledCode = getCode(methodSig, runtimeType);
		
		StringBuilder stringBuilder = new StringBuilder("\"");
		stringBuilder.append(decompiledCode);
		stringBuilder.append("\" with signature \"");
		stringBuilder.append(methodSig);
		stringBuilder.append("\":");
		
		return stringBuilder.toString();
	}
	
	public static String getQueryRequestV2(String varName, String varInfo, String code) throws IOException {
		if (code == null || code.equals("")) {
			return "";
		}

		if (code.startsWith("//")) {
			return "";
		}
		
		StringBuilder stringBuilder = new StringBuilder("Given variable ");
		stringBuilder.append(varInfo);
		stringBuilder.append(" After calling \"");
		stringBuilder.append(code);
		stringBuilder.append("\" once, the following fields of \"");
		stringBuilder.append(varName);
		stringBuilder.append("\" are modified:");
		
		return stringBuilder.toString();
	}
	
	public static String getQueryRequestFromParams(List<VarValue> parameters, String code) {
		if (code == null || code.equals("") || code.startsWith("//")) {
			return "";
		}
		if (parameters == null || parameters.size() == 0) {
			return "";
		}
		
		StringBuilder stringBuilder = new StringBuilder("Given variables");
		for (VarValue param : parameters) {
			stringBuilder.append(" ");
			stringBuilder.append(param.getJsonString());
		}
		stringBuilder.append(" After calling \"");
		stringBuilder.append(code);
		stringBuilder.append("\" once, the following fields of ");
		for (int i = 0; i < parameters.size(); i++) {
			stringBuilder.append("\"");
			stringBuilder.append(parameters.get(i).getVarName());
			if (i < parameters.size() - 1) {
				stringBuilder.append("\",");
			}
		}
		stringBuilder.append("\" are modified:");
		
		return stringBuilder.toString();
	}
	
	public static String getVariableJsonString(String variableName, Object variableValue) {
		StringBuilder stringBuilder = new StringBuilder();
		getJsonStringRecur(stringBuilder, variableName, variableValue.getClass().getName(), variableValue, 3);
		return stringBuilder.toString();
	}
	
	private static void getJsonStringRecur(StringBuilder sb, String name, String type, Object variableValue, int layer) {
		if (layer < 0) {
			return;
		} else if (layer == 0) {
			sb.append("{name:");
			sb.append(name);
			sb.append(",type:");
			sb.append(type);
			sb.append(",value:");
			if (variableValue == null) {
				sb.append("null};");
				return;
			} else {
				sb.append(getStringValue(variableValue, type));
				sb.append("};");
				return;
			}
		}
		
		sb.append("{name:");
		sb.append(name);
		sb.append(",type:");
		sb.append(type);
		sb.append(",value:");
		if (variableValue == null) {
			sb.append("null};");
			return;
		}
		
		if (PrimitiveUtils.isPrimitiveTypeOrString(type)) {
			/* primitive or string */
			sb.append(getStringValue(variableValue, type));
			sb.append("};");
		} else if (type.contains("[")) {
			/* array */
			int length = Array.getLength(variableValue);
			sb.append("[");
			for (int i = 0; i < length; i++) {
				String varName = name + "[" + i + "]";
				Object elementValue = Array.get(variableValue, i);
				String componentType = elementValue == null ? type.substring(1): elementValue.getClass().getName();
				getJsonStringRecur(sb, varName, componentType, elementValue, layer - 1);
			}
			sb.append("]};");
		} else {
			/* reference type */
			sb.append("[");
			Class<?> clazz = variableValue.getClass();
			while (clazz != null) {
				Field[] fields = clazz.getDeclaredFields();
				for (Field f : fields) {
					boolean isFinal = Modifier.isFinal(f.getModifiers());
					if (isFinal) {
						continue;
					}
					f.setAccessible(true);
					try {
						String varName = f.getName();
						String componentType = f.getType().getName();
						Object val = f.get(variableValue);
						getJsonStringRecur(sb, varName, componentType, val, layer - 1);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
				clazz = clazz.getSuperclass();
			}
			sb.append("]};");
		}

	}
	
	private static String getStringValue(final Object obj, String type) {
		try {
			if (obj == null) {
				return "null";
			}

			String simpleType = null;
			if (type != null && type.contains("[")) {
				simpleType = type.substring(1);
			}

			if (simpleType != null) {
				if (simpleType.equals("char")) {
					char[] charArray = (char[]) obj;
					return String.valueOf(charArray);
				}
			}

			String value = String.valueOf(obj);
			
			return value;
		} catch (Throwable t) {
			return null;
		}
	}

	private static ClassGen loadClass(String className) {
		try {
			String classFName = className.replace('.', '/');
			String fileName = classFName + ".class";
			byte[] bytecode = QueryRequestGenerator.loadByteCode(fileName);

			ClassParser cp = new ClassParser(new java.io.ByteArrayInputStream(bytecode), classFName);
			JavaClass jc = cp.parse();
			return new ClassGen(jc);
		} catch (IOException | NullPointerException e) {
			return null;
		}
	}
	
	private static byte[] loadByteCode(String fileName) {
		try {
			InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName);
			if (inputStream != null) {
				byte[] bytecode = IOUtils.toByteArray(inputStream);
				inputStream.close();
				return bytecode;
			}
			return null;
		} catch (IOException e) {
			return null;
		}
	}
	
	private static Method getMethod(ClassGen classGen, String methodInfo) {
		Method[] methods = classGen.getMethods();
		for (Method method : methods) {
			String name = method.getName();
			String signature = method.getSignature();
			if (methodInfo.equals(name + signature)) {
				return method;
			}
		}
		return null;
	}

	public static String getCode(String varName, String methodSig, Object[] args) {
		String methodName = methodSig.split("#")[1].split("\\(")[0];

		StringBuilder stringBuilder = new StringBuilder(varName);
		stringBuilder.append(".");
		stringBuilder.append(methodName);
		stringBuilder.append("(");
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (arg == null) {
				stringBuilder.append("null");
			} else {
				stringBuilder.append(arg.toString());
			}
			if (i < args.length - 1) {
				stringBuilder.append(",");
			}
		}
		stringBuilder.append(")");
		return stringBuilder.toString();
	}

	public static String getCode(String varName, String methodSig, String runtimeType) {
		Method method = getMethod(methodSig, runtimeType);
		if (method == null) {
			return "";
		}

		StringBuilder stringBuilder = new StringBuilder(varName);
		stringBuilder.append(".");
		stringBuilder.append(method.getName());
		stringBuilder.append("(");
		Type[] arguments = method.getArgumentTypes();
		for (int i = 0; i < arguments.length; i++) {
			Type argument = arguments[i];
			stringBuilder.append(getDecompiledObjectName(argument.toString()));
			if (i < arguments.length - 1) {
				stringBuilder.append(",");
			}
		}
		stringBuilder.append(")");

		return stringBuilder.toString();
	}

	public static String getCode(String methodSig, String runtimeType) {
		Method method = getMethod(methodSig, runtimeType);
		if (method == null) {
			return "";
		}

		String methodName = method.getName();
		Type[] arguments = method.getArgumentTypes();

		String clazz = methodSig.split("#")[0];
		return getDecompiledCode(runtimeType == null ? clazz : runtimeType, methodName, arguments);
	}
	
	public static String getCodeStatic(String methodSig) {
		String type = methodSig.split("#")[0];
		Method method = getMethod(methodSig, type);
		if (method == null) {
			return "";
		}

		StringBuilder stringBuilder = new StringBuilder(type);
		stringBuilder.append(".");
		stringBuilder.append(method.getName());
		stringBuilder.append("(");
		Type[] arguments = method.getArgumentTypes();
		for (int i = 0; i < arguments.length; i++) {
			Type argument = arguments[i];
			stringBuilder.append(getDecompiledObjectName(argument.toString()));
			if (i < arguments.length - 1) {
				stringBuilder.append(",");
			}
		}
		stringBuilder.append(")");

		return stringBuilder.toString();
	}
	
	public static String getCodeStatic(String methodSig, Object[] args) {
		String methodName = methodSig.replace("#", ".").split("\\(")[0];

		StringBuilder stringBuilder = new StringBuilder(methodName);
		stringBuilder.append("(");
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (arg == null) {
				stringBuilder.append("null");
			} else {
				stringBuilder.append(arg.toString());
			}
			if (i < args.length - 1) {
				stringBuilder.append(",");
			}
		}
		stringBuilder.append(")");
		return stringBuilder.toString();
	}

	private static Method getMethod(String methodSig, String runtimeType) {
		String clazz = methodSig.split("#")[0];
		String methodInfo = methodSig.split("#")[1];

		// try runtime type
		ClassGen classGen = null;
		Method method = null;
		boolean shouldCheckCompiledType = true;
		if (runtimeType != null) {
			classGen = QueryRequestGenerator.loadClass(runtimeType);
			if (classGen != null) {
				method = QueryRequestGenerator.getMethod(classGen, methodInfo);
			}
			if (method != null) {
				shouldCheckCompiledType = false;
			}
		}

		// try clazz
		if (shouldCheckCompiledType) {
			classGen = QueryRequestGenerator.loadClass(clazz);
			if (classGen != null) {
				method = QueryRequestGenerator.getMethod(classGen, methodInfo);
			}
		}

		return method;
	}

	private static String getDecompiledCode(String invokingClass, String methodName, Type[] arguments) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(getDecompiledObjectName(invokingClass));
		stringBuilder.append(".");
		stringBuilder.append(methodName);
		stringBuilder.append("(");
		for (int i = 0; i < arguments.length; i++) {
			Type argument = arguments[i];
			stringBuilder.append(getDecompiledObjectName(argument.toString()));
			if (i < arguments.length - 1) {
				stringBuilder.append(",");
			}
		}
		stringBuilder.append(")");
		return stringBuilder.toString();
	}
	
	private static String getDecompiledObjectName(String typeName) {
		if (typeName.startsWith("java")) {
			String[] info = typeName.split("\\.");
			return info[info.length - 1].toLowerCase();
		} else {
			return typeName;
		}
	}
}
