package microbat.tracerecov.coderetriever;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.benf.cfr.reader.Main;

import microbat.tracerecov.TraceRecovUtils;
import sav.strategies.dto.AppJavaClassPath;

/**
 * This class is used to retrieve the source code of a method given bytecode.
 */
public class SourceCodeRetriever {

	private BytecodeRetriever bytecodeRetriever;
	private MethodExtractor methodExtractor;

	public SourceCodeRetriever() {
		this.bytecodeRetriever = new BytecodeRetriever();
		this.methodExtractor = new MethodExtractor();
	}

	private String decompileClass(Path classFilePath) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		PrintStream old = System.out;

		System.setOut(ps);

		Main.main(new String[] { classFilePath.toString() });

		System.out.flush();
		System.setOut(old);
		return baos.toString();
	}

	private String getConstructorCode(String sourceCode, String className, String... paramTypes)
			throws CodeRetrieverException {
		return methodExtractor.getConstructorCode(sourceCode, className, paramTypes);
	}

	private String getMethodCode(String sourceCode, String className, String returnType, String methodName,
			String... paramTypes) throws CodeRetrieverException {
		return methodExtractor.getMethodCode(sourceCode, className, returnType, methodName, paramTypes);
	}

	public String getMethodCode(String signature, AppJavaClassPath appJavaClassPath) {
		/* class name */
		String fullClassName = signature.split("#")[0];
		String className = TraceRecovUtils.getSimplifiedTypeName(fullClassName);
		
		/* A patch for CSVPrinter 
		 * This is an issue with javaparser 3.26.1
		 * TODO: update javaparser to newer version if released */
		if (className.equals("CSVPrinter")) {
			return signature;
		}

		/* method name */
		String methodSignature = signature.split("#")[1];
		String methodName = methodSignature.split("\\(")[0];

		/* parameter types */
		String inputsAndOutput = methodSignature.split("\\(")[1];
		String[] inputs = getParameterTypes(inputsAndOutput.split("\\)")[0]);

		/* return type */
		String returnType = inputsAndOutput.split("\\)")[1];
		returnType = TraceRecovUtils.getSimplifiedTypeName(TraceRecovUtils.getReadableType(returnType));

		Path classFile = null;
		try {
			// save bytecode to temp file
			classFile = this.bytecodeRetriever.getBytecodeInFile(fullClassName, appJavaClassPath);

			String sourceCode = decompileClass(classFile);
			String methodSourceCode = "";
			if (methodName.equals("<init>")) {
				methodSourceCode = getConstructorCode(sourceCode, className, inputs);
			} else {
				methodSourceCode = getMethodCode(sourceCode, className, returnType, methodName, inputs);
			}

			Files.delete(classFile);
			return methodSourceCode;
		} catch (ClassNotFoundException | IOException | CodeRetrieverException e) {
			try {
				if (classFile != null) {
					Files.delete(classFile);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return signature;
		}
	}

	private String[] getParameterTypes(String inputs) {
		ArrayList<String> parameterTypes = new ArrayList<>();
		int length = inputs.length();
		int i = 0;
		while (i < length) {
			char character = inputs.charAt(i);
			if (character >= 65 && character <= 90 && character != 'L') {
				String type = String.valueOf(character);
				String readableType = TraceRecovUtils.getReadableType(type);
				parameterTypes.add(readableType);
				i++;
			} else if (character == 'L' || character == '[') {
				String type = inputs.substring(i).split(";")[0];
				String readableType = TraceRecovUtils.getSimplifiedTypeName(TraceRecovUtils.getReadableType(type));
				parameterTypes.add(readableType);
				i += type.length() + 1;
			}
		}

		String[] types = new String[parameterTypes.size()];
		for (int j = 0; j < parameterTypes.size(); j++) {
			types[j] = parameterTypes.get(j);
		}

		return types;
	}

}
