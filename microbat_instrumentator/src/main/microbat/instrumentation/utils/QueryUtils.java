package microbat.instrumentation.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class QueryUtils {
	
	public static String srcPath;
	public static String testPath;

	public static Path getPath(String className) throws ClassNotFoundException {
		String basePath = System.getProperty("user.dir");
		
		int dollarSignIndex = className.indexOf("$");
		className = className.substring(0, dollarSignIndex == -1 ? className.length() : dollarSignIndex);

		StringBuilder pathBuilder = new StringBuilder(basePath);
		pathBuilder.append(File.separator);
		pathBuilder.append(srcPath);
		pathBuilder.append(File.separator);
		pathBuilder.append(className.replace(".", File.separator));
		pathBuilder.append(".java");
		
		Path path = Paths.get(pathBuilder.toString());
		
		if (Files.notExists(path)) {
			pathBuilder = new StringBuilder(basePath);
			pathBuilder.append(File.separator);
			pathBuilder.append(testPath);
			pathBuilder.append(File.separator);
			pathBuilder.append(className.replace(".", File.separator));
			pathBuilder.append(".java");
			
			path = Paths.get(pathBuilder.toString());
		}
		
		if (Files.notExists(path)) {
			return null;
		}
		
		return path;
	}
	
	public static String getCode(Path path, int lineNo) {
		try {
			Charset charset = Charset.forName("UTF-8");

			List<String> lines = Files.readAllLines(path, charset);
			return lines.get(lineNo - 1).trim();
		} catch (IOException e) {
			return "";
		}
	}
}
