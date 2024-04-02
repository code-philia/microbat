package microbat.instrumentation.benchmark;

import java.io.IOException;
import java.io.InputStream;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.Type;
import org.apache.commons.io.IOUtils;

public class QueryRequestGenerator {

	public QueryRequestGenerator() {}
	
	public static String getQueryRequest(String methodSig, String runtimeType) throws IOException {
		String clazz = methodSig.split("#")[0];
		String methodInfo = methodSig.split("#")[1];
		
		// try runtime type
		ClassGen classGen = null;
		Method method = null;
		boolean shouldCheckCompiledType = true;
		if (runtimeType != null) {
			classGen = QueryRequestGenerator.loadClass(runtimeType);
			method = QueryRequestGenerator.getMethod(classGen, methodInfo);
			if (method != null) {
				shouldCheckCompiledType = false;
			}
		}

		// try clazz
		if (shouldCheckCompiledType) {
			classGen = QueryRequestGenerator.loadClass(clazz);
			method = QueryRequestGenerator.getMethod(classGen, methodInfo);
			if (method == null) {
				return "";
			}
		}

		String methodName = method.getName();
		Type[] arguments = method.getArgumentTypes();
		
		String decompiledCode = getDecompiledCode(runtimeType == null ? clazz : runtimeType, methodName, arguments);
		
		StringBuilder stringBuilder = new StringBuilder("\"");
		stringBuilder.append(decompiledCode);
		stringBuilder.append("\" with signature \"");
		stringBuilder.append(methodSig);
		stringBuilder.append("\":");
		
		return stringBuilder.toString();
	}
	
	public static String getQueryRequestV2(String varName, String varInfo, String code) throws IOException {
		if (code.startsWith("//")) {
			return "";
		}
		
		StringBuilder stringBuilder = new StringBuilder("Given variable ");
		stringBuilder.append(varInfo);
		stringBuilder.append(" After calling \"");
		stringBuilder.append(code);
		stringBuilder.append("\" once, ");
		stringBuilder.append(varName);
		stringBuilder.append(".value becomes:");
		
		return stringBuilder.toString();
	}

	private static ClassGen loadClass(String className) throws IOException {
		String classFName = className.replace('.', '/');
		String fileName = classFName  + ".class";
		byte[] bytecode = QueryRequestGenerator.loadByteCode(fileName);
		
		ClassParser cp = new ClassParser(new java.io.ByteArrayInputStream(bytecode), classFName);
		JavaClass jc = cp.parse();
		return new ClassGen(jc);
	}
	
	private static byte[] loadByteCode(String fileName) throws IOException {
		InputStream inputStream = ClassLoader
        		.getSystemClassLoader()
        		.getResourceAsStream(fileName);
		if (inputStream != null) {
			byte[] bytecode = IOUtils.toByteArray(inputStream);
            inputStream.close();
            return bytecode;
        }
		return null;
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
