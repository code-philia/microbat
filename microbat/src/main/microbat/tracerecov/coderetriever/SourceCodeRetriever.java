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

	private String getConstructorCode(AppJavaClassPath appJavaClassPath, String sourceCode, String className,
			String... paramTypes) throws CodeRetrieverException {
		return methodExtractor.getConstructorCode(appJavaClassPath, sourceCode, className, paramTypes);
	}

	private String getMethodCode(AppJavaClassPath appJavaClassPath, String sourceCode, String className,
			String returnType, String methodName, String... paramTypes) throws CodeRetrieverException {
		return methodExtractor.getMethodCode(appJavaClassPath, sourceCode, className, returnType, methodName,
				paramTypes);
	}

	public String getMethodCode(String signature, AppJavaClassPath appJavaClassPath) {
		/* class name */
		String fullClassName = signature.split("#")[0];
		String className = TraceRecovUtils.getSimplifiedTypeName(fullClassName);

		/* method name */
		String methodSignature = signature.split("#")[1];
		String methodName = methodSignature.split("\\(")[0];

		/* parameter types */
		String inputsAndOutput = methodSignature.split("\\(")[1];
		String[] inputs = getParameterTypes(inputsAndOutput.split("\\)")[0]);

		/* return type */
		String returnType = inputsAndOutput.split("\\)")[1];
		returnType = TraceRecovUtils.getReadableType(returnType);
		if (returnType.endsWith(";")) {
			returnType = returnType.substring(0, returnType.indexOf(";"));
		}

		Path classFile = null;
		try {
			// save bytecode to temp file
			classFile = this.bytecodeRetriever.getBytecodeInFile(fullClassName, appJavaClassPath);

			if (classFile == null) {
				return signature;
			}

			String sourceCode = decompileClass(classFile);
			String methodSourceCode = "";
			if (methodName.equals("<init>")) {
				methodSourceCode = getConstructorCode(appJavaClassPath, sourceCode, className, inputs);
			} else {
				methodSourceCode = getMethodCode(appJavaClassPath, sourceCode, className, returnType, methodName,
						inputs);
			}

			return methodSourceCode;
		} catch (ClassNotFoundException | IOException | CodeRetrieverException e) {
			e.printStackTrace();
		} finally {
			try {
				if (classFile != null) {
					Files.delete(classFile);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return signature;
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
			} else if (character == '[') {
				String substring = inputs.substring(i);
				String type = null;
				if (substring.charAt(1) == 'L') {
					type = substring.split(";")[0];
					i += type.length() + 1;
				} else {
					type = inputs.substring(i, i + 2);
					i += 2;
				}
				String readableType = TraceRecovUtils.getReadableType(type);
				parameterTypes.add(readableType);
			} else if (character == 'L') {
				String type = inputs.substring(i).split(";")[0];
				String readableType = TraceRecovUtils.getReadableType(type);
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
