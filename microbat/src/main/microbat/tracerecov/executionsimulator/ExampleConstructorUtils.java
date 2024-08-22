package microbat.tracerecov.executionsimulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import microbat.Activator;
import microbat.preference.MicrobatPreference;
import microbat.util.MicroBatUtil;
import sav.strategies.dto.AppJavaClassPath;

public class ExampleConstructorUtils {
	private static final String BACKGROUND = """
			Given the name, type and runtime value of a variable, your task is to generate a piece of java code which
			creates a variable with the specified name and type, and let it contain the same runtime value.\n
			""";
	
	private static final String RULES = """
			Strictly follow the following rules:
			1.The launch class generated should be named 'Example' containing a 'main' method;
			2.Only return the generated java code enclosed with ```, including the 'import' statement.\n
			""";

	private static final String EXAMPLE = """
			For example, given a variable named 'list' of type 'java.util.ArrayList' with runtime value '[1,2,3]',
			you may return:
			```
			import java.util.ArrayList;
			public class Example {
			    public static void main(String[] args) {
			        ArrayList<Integer> list = new ArrayList<>();
			        list.add(1);
			        list.add(2);
			        list.add(3);
			        System.out.println(list);
			    }
			}
			```\n
			""";

	/*
	 * MODIFY this path which contains the generated code and compiled version
	 */
	public static final String CODE_GEN_BASE_FOLDER = "D:\\data_dependency_recovery\\example";
	public static final String EXAMPLE_BASE_FOLDER = "D:\\data_dependency_recovery\\exampleDB";
	
	public static final String CODE_GEN_FILENAME = "Example.java";
	public static final String CODE_SRC_FOLDER = "src";
	public static final String CODE_BUILD_FOLDER = "build";
	public static final String EXAMPLE_FILENAME = "example.txt";
	
	public static String getBackground() {
		return BACKGROUND;
	}
	
	public static String getRules() {
		return RULES;
	}
	
	public static String getExample() {
		return EXAMPLE;
	}
	
	public static PrintWriter createWriter(String path) throws IOException {
		FileWriter fileWriter = new FileWriter(path, true);
		PrintWriter writer = new PrintWriter(fileWriter);
		return writer;
	}
	
	public static void saveToFile(String filePath, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public static String createDir() {
        File directory = new File(CODE_GEN_BASE_FOLDER);
        File[] subdirectories = directory.listFiles(File::isDirectory);

        int maxNumber = 0;
        if (subdirectories != null && subdirectories.length > 0) {
            for (File subdirectory : subdirectories) {
                String name = subdirectory.getName();
                try {
                    int number = Integer.parseInt(name);
                    if (number > maxNumber) {
                        maxNumber = number;
                    }
                } catch (NumberFormatException e) {
                	// do nothing
                }
            }
        }

        File newSubdirectory = new File(CODE_GEN_BASE_FOLDER, String.valueOf(maxNumber + 1));
        newSubdirectory.mkdir();
        
        File srcDir = new File(newSubdirectory.getAbsolutePath(),CODE_SRC_FOLDER);
        srcDir.mkdir();
        
        File buildDir = new File(newSubdirectory.getAbsolutePath(),CODE_BUILD_FOLDER);
        buildDir.mkdir();
        
        return String.valueOf(maxNumber + 1);
	}
	
	public static void compileJavaFile(String sourceFilePath, String targetFilePath) {
        Path sourcePath = Paths.get(sourceFilePath);// path/to/src/code.java
        Path targetPath = Paths.get(targetFilePath);// path/to/build
        File targetDirectory = targetPath.toFile();
        
        String javaHome = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.JAVA7HOME_PATH);
        
        ProcessBuilder processBuilder = new ProcessBuilder(
        	    javaHome+"\\bin\\javac", "-d", targetFilePath, sourceFilePath);
        
        Process process;
        try {
        	process = processBuilder.start();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
        
        int exitCode;
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
        System.out.println("Compilation exited with code: " + exitCode);

        File classFile = new File(sourcePath.toString().replace(".java", ".class"));
        if (classFile.exists()) {
            classFile.renameTo(new File(targetDirectory, classFile.getName()));
        }
	}
	
	
	public static AppJavaClassPath createAppClassPath(String srcFilePath, String targetFilePath, String dirName) {
		AppJavaClassPath appClassPath = MicroBatUtil.constructClassPaths();
		appClassPath.addClasspath(srcFilePath);
		appClassPath.addClasspath(targetFilePath);
		appClassPath.addClasspath(ExampleConstructorUtils.CODE_GEN_BASE_FOLDER+File.separator+dirName);
		
		appClassPath.setSourceCodePath(ExampleConstructorUtils.CODE_GEN_BASE_FOLDER+File.separator+
				dirName+File.separator+
				ExampleConstructorUtils.CODE_SRC_FOLDER);
		appClassPath.setTestCodePath(ExampleConstructorUtils.CODE_GEN_BASE_FOLDER+File.separator+
				dirName+File.separator+
				ExampleConstructorUtils.CODE_SRC_FOLDER);
		
		appClassPath.setWorkingDirectory(ExampleConstructorUtils.CODE_GEN_BASE_FOLDER+File.separator+dirName);
		appClassPath.setLaunchClass("Example");
        String javaHome = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.JAVA7HOME_PATH);
		appClassPath.setJavaHome(javaHome);
				
		return appClassPath;
	}
	
}
