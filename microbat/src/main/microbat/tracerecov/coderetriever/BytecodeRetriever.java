package microbat.tracerecov.coderetriever;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * This class is used to retrieve the complete bytecode of a given class.
 */
public class BytecodeRetriever {

	public BytecodeRetriever() {
	}

	private byte[] getBytecode(Class<?> clazz) throws IOException {
		ClassReader classReader = new ClassReader(clazz.getName());
		ClassWriter classWriter = new ClassWriter(0);
		classReader.accept(classWriter, 0);

		return classWriter.toByteArray();
	}

	public Path getBytecodeInFile(String className) throws ClassNotFoundException, IOException {
		Class<?> clazz = Class.forName(className);
		byte[] bytecode = getBytecode(clazz);

		Path tempFile = Files.createTempFile(clazz.getSimpleName(), ".class");
		Files.write(tempFile, bytecode, StandardOpenOption.CREATE);
		return tempFile;
	}

}