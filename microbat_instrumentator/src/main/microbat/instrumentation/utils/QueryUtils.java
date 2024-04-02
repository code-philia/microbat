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

	public static Path getPath(String className) {
		String basePath = System.getProperty("user.dir");
		
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
		
		return path;
	}
	
	public static String getCode(String className, int lineNo) throws IOException {
		Path path = QueryUtils.getPath(className);
		Charset charset = Charset.forName("UTF-8");
		
		List<String> lines = Files.readAllLines(path, charset);
		return lines.get(lineNo - 1).trim();
	}
	
	public static boolean isValidVar(String code, String methodSignature) {
		String actualMethodName = getMethodNameFromCode(code);
		return methodSignature.contains(actualMethodName) && !methodSignature.contains("valueOf");
	}
	
	private static String getMethodNameFromCode(String code) {
		String[] subStrings = code.split("\\.");
		if (subStrings.length == 0) {
			// empty string
			return "";
		}
		
		String methodInfo = "";
		if (subStrings.length == 1) {
			methodInfo = subStrings[0];
		} else {
			methodInfo = subStrings[1];
		}
		
		return methodInfo.split("\\(")[0];
	}
}
