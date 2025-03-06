package microbat.tracerecov.autoprompt.incontextlearning;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.StepLimitException;
import microbat.instrumentation.CommonParams;
import microbat.instrumentation.output.RunningInfo;
import microbat.model.trace.Trace;
import microbat.util.StringFormatUtils;
import sav.strategies.dto.AppJavaClassPath;

public class InContextExecutor {
    public static Logger log = LoggerFactory.getLogger(InContextExecutor.class);

    public static final String IN_CONTEXT_LEARNING_FOLDER = "in-context-learning";
    public static final String SRC_FOLDER = "src";
    public static final String BIN_FOLDER = "bin";
    public static final String TRACE_FOLDER = "trace";
    public static final String MAIN_JAVA_NAME = "SampleTest.java";
    public static final String TRACE_FILE_NAME = "trace";

    private static AtomicInteger counter = new AtomicInteger(0);

    private String currentWorkingDirName;
    private String srcDirName;
    private String binDirName;
    private String srcFileName;
    private String traceDirName;
    private File currentWorkingDir;
    private File srcDir;
    private File binDir;
    private File srcFile;
    private File traceDir;

    private AppJavaClassPath appClassPath;

    private SourceCodeWritter sourceCodeWritter;
    
    public InContextExecutor(AppJavaClassPath appJavaClassPath) {
    	this.appClassPath = appJavaClassPath;
    	this.appClassPath = appJavaClassPath.duplicate();
    }

    @FunctionalInterface
    public static interface SourceCodeWritter {
        public void writeSourceCode(FileWriter writer) throws IOException;

        public static SourceCodeWritter fromString(String sourceCode) {
            return writer -> writer.write(sourceCode);
        }
    }

    private static int getNextCounter() {
        return counter.getAndIncrement();
    }

    private static String getNewWorkingDirName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String formatted = now.format(formatter);
        return formatted + "-" + getNextCounter();
    }

    public InContextExecutor(SourceCodeWritter sourceCodeWritter, AppJavaClassPath appJavaClassPath) {
        this.sourceCodeWritter = sourceCodeWritter;
        this.appClassPath = appJavaClassPath.duplicate();

        String separator = File.separator;

        currentWorkingDirName = appJavaClassPath.getWorkingDirectory()
                + separator + IN_CONTEXT_LEARNING_FOLDER
                + separator + getNewWorkingDirName();
        currentWorkingDir = new File(currentWorkingDirName);

        srcDirName = currentWorkingDir
                + separator + SRC_FOLDER;
        srcDir = new File(srcDirName);
        if (!srcDir.exists()) {
            srcDir.mkdirs();
        }

        binDirName = currentWorkingDir
                + separator + BIN_FOLDER;
        binDir = new File(binDirName);
        if (!binDir.exists()) {
            binDir.mkdirs();
        }

        traceDirName = currentWorkingDir
                + separator + TRACE_FOLDER;
        traceDir = new File(traceDirName);
        if (!traceDir.exists()) {
            traceDir.mkdirs();
        }

        srcFileName = srcDirName + separator + MAIN_JAVA_NAME;
        srcFile = new File(srcFileName);

        log.info("Java home: {}", appJavaClassPath.getJavaHome());
        log.info("working path: {}", currentWorkingDirName);
        log.info("src path: {}", srcDirName);
        log.info("bin path: {}", binDirName);
    }

    public Trace run() {
        try {
            return runInner();
        } catch (Exception e) {
            log.error("Failed to run gpt.", e);
        }
        return null;
    }

    public Trace runInner() {
        writeSrcFile();
        initializeAppClassPath();
        compileFile();
        return runTarget();
    }

    public void writeSrcFile() {
        try (FileWriter writer = new FileWriter(srcFile)) {
            sourceCodeWritter.writeSourceCode(writer);
        } catch (IOException e) {
            String msg = "Failed to write source file: " + srcFileName;
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public void compileFile() {
        String javaHome = appClassPath.getJavaHome();
        String javac = javaHome + File.separator + "bin" + File.separator + "javac";

        ArrayList<String> command = new ArrayList<>();
        command.add(javac);
        String classpaths = String.join(File.pathSeparator, appClassPath.getClasspaths());
        if (!classpaths.isEmpty()) {
            command.add("-cp");
            command.add(classpaths);
        }
        command.add("-d");
        command.add(binDirName);
        command.add(srcFileName);
        runCommand(command);
    }

	public void initializeAppClassPath() {

		appClassPath.setOptionalTestClass("SampleTest");
		appClassPath.setOptionalTestMethod("testWrapper");

		appClassPath.setWorkingDirectory(binDirName);

		List<String> classPaths = new ArrayList<>();
		classPaths.add(binDirName);
		List<String> originalClassPaths = appClassPath.getClasspaths();
		for (String p : originalClassPaths) {
			classPaths.add(p);
		}
		appClassPath.setClasspaths(classPaths);

		appClassPath.setSourceCodePath(srcDirName);
		appClassPath.setTestCodePath(binDirName);
	}

    public Trace runTarget() {
        List<String> includeLibs = new ArrayList<>();
        List<String> excludeLibs = new ArrayList<>();
        includeLibs.add("*");

        InstrumentationExecutor executor = new InstrumentationExecutor(
                appClassPath,
                traceDirName,
                TRACE_FILE_NAME,
                includeLibs,
                excludeLibs);
        executor.getAgentRunner().addAgentParam(CommonParams.OPT_FORCE_EXIT_WITHOUT_WAIT_OTHER_THREADS, "true");
        executor.getAgentRunner().addAgentParam(CommonParams.OPT_MANUALLY_TEST_RUNNING_CLASS, "SampleTest");
        RunningInfo results = null;
        try {
            results = executor.run();
        } catch (StepLimitException e) {
            log.error("Step limit exceeded", e);
            throw new RuntimeException("Step limit exceeded", e);
        }

        return results.getMainTrace();
    }

    public void runCommand(List<String> cmdline) {
        ProcessBuilder pb = new ProcessBuilder(cmdline);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        int exitCode = -1;
        try {
            ReadFromStream readStdout = null, readStderr = null;
            Process p = pb.start();
            try {
                readStdout = new ReadFromStream(p.getInputStream());
                readStderr = new ReadFromStream(p.getErrorStream());
                executor.submit(readStdout);
                executor.submit(readStderr);
                boolean ret = p.waitFor(10, TimeUnit.SECONDS);
                if (!ret) {
                    throw new RuntimeException("Timeout while waiting for process to finish");
                }
                exitCode = p.exitValue();
            } finally {
                p.destroyForcibly();
            }

            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout while waiting for process to finish");
            }

            if (exitCode != 0) {
                log.error("Command line failed: {}. STDOUT: {}, STDERR: {}",
                        cmdline, readStdout.getOutput(), readStderr.getOutput());
                throw new RuntimeException("Command line failed: " + cmdline);
            }
        } catch (IOException | InterruptedException e) {
            String msg = "Failed to compile source file: " + srcFileName;
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            executor.shutdown();
        }
    }

    public static String getTestSampleSource(int idx) {
        try (
                InputStream is = InContextExecutor.class.getClassLoader()
                        .getResourceAsStream("run_sample/sample" + idx + "/SampleTest.java")) {
            ReadFromStream readFromStream = new ReadFromStream(is);
            readFromStream.call();
            return readFromStream.getOutput();
        } catch (Exception e) {
            log.error("Failed to read test sample source: {}", idx, e);
            throw new RuntimeException("Failed to read test sample source", e);
        }
    }

    public static class ReadFromStream implements Callable<Void> {
        public ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        public InputStream inputStream;

        public ReadFromStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public Void call() throws Exception {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            return null;
        }

        public String getOutput() {
            byte[] bytes = byteArrayOutputStream.toByteArray();
            return StringFormatUtils.decodeWithIgnore(bytes);
        }
    }
}
