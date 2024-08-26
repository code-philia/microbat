package microbat.tracerecov.coderetriever;

import microbat.tracerecov.TraceRecovUtils;
import sav.strategies.dto.AppJavaClassPath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * This class is used to retrieve the complete bytecode of a given class.
 */
public class BytecodeRetriever {

	public BytecodeRetriever() {
	}

	public Path getBytecodeInFile(String className, AppJavaClassPath appJavaClassPath)
			throws ClassNotFoundException, IOException {
		// load the class
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");

		if (inputStream == null) {
			if (appJavaClassPath == null) {
				return null;
			}
			ClassLoader classLoader2 = appJavaClassPath.getClassLoader();
			inputStream = classLoader2.getResourceAsStream(className.replace('.', '/') + ".class");
		}
		
		if (inputStream == null) {
			return null;
		}

		byte[] bytecode = inputStream.readAllBytes();

		Path tempFile = Files.createTempFile(TraceRecovUtils.getSimplifiedTypeName(className), ".class");
		Files.write(tempFile, bytecode, StandardOpenOption.CREATE);
		return tempFile;
	}

}