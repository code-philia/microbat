package microbat.handler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;

import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.StepLimitException;
import microbat.handler.callbacks.HandlerCallbackManager;
import microbat.instrumentation.output.RunningInfo;
import microbat.log.Log;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.preference.AnalysisScopePreference;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.coderetriever.SourceCodeRetriever;
import microbat.tracerecov.executionsimulator.SourceCodeDatabase;
import microbat.util.JavaUtil;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import microbat.views.DebugFeedbackView;
import microbat.views.MicroBatViews;
import microbat.views.TraceView;
//import microbat.views.TraceView;
import sav.common.core.utils.FileUtils;
import sav.strategies.dto.AppJavaClassPath;

public class ConstructBenchmarkHandler extends AbstractHandler {
	private final String BENCHMARK_FOLDER_PATH = "C:\\java-collections";
	private final String EXAMPLE_FILE = "C:\\Users\\admin\\in-context_learning_datasets\\definition_inference.txt";

	private void clearOldData() {
		Settings.interestedVariables.clear();
		Settings.wrongPathNodeOrder.clear();
		Settings.potentialCorrectPatterns.clear();
		Settings.checkingStateStack.clear();

		Settings.compilationUnitMap.clear();
		Settings.iCompilationUnitMap.clear();

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				DebugFeedbackView view = MicroBatViews.getDebugFeedbackView();
				view.clear();
			}

		});
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job(DebugPilotHandler.JOB_FAMALY_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Path sourceFolder = Paths.get(BENCHMARK_FOLDER_PATH + "\\src");
				try (Stream<Path> stream = Files.list(sourceFolder)) {
					List<String> dataStructureNames = stream.filter(file -> Files.isDirectory(file))
							.map(path -> path.getFileName().toString()).collect(Collectors.toList());

					for (String dataStructure : dataStructureNames) {
						Path dataStructureFolder = Paths.get(BENCHMARK_FOLDER_PATH + "\\src\\" + dataStructure);
						Stream<Path> dataStructureProjectsStream = Files.list(dataStructureFolder);
						List<String> projectNames = dataStructureProjectsStream.filter(file -> !Files.isDirectory(file))
								.map(path -> path.getFileName().toString()).collect(Collectors.toList());

						for (String projectName : projectNames) {
							// get launch class names
							String className = projectName.split("\\.")[0];
							String launchClassName = dataStructure + "." + className;

							clearOldData();
							// Clear the DebugPilot debugging process if there are any
							HandlerCallbackManager.getInstance().runDebugPilotTerminateCallbacks();
							Job.getJobManager().cancel(DebugPilotHandler.JOB_FAMALY_NAME);

							// Clear the path view and program output form
//							MicroBatViews.getPathView().updateFeedbackPath(null);
//							MicroBatViews.getDebugPilotFeedbackView().clearProgramOutput();

							execute(launchClassName);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return this.getName().equals(family);
			}
		};
		job.schedule();
		return null;
	}

	protected void execute(String launchClass) {
		Log.printMsg(getClass(), "");
		Log.printMsg(getClass(), "=====================================");
		Log.printMsg(getClass(), "Launch Class: " + launchClass);
		Log.printMsg(getClass(), "=====================================");
		Log.printMsg(getClass(), "");
		Settings.launchClass = launchClass;

		final AppJavaClassPath appClassPath = MicroBatUtil.constructClassPaths();
		List<String> srcFolders = MicroBatUtil.getSourceFolders(Settings.projectName);
		appClassPath.setSourceCodePath(MicroBatUtil.getSourceFolder(Settings.launchClass, Settings.projectName));
		for (String srcFolder : srcFolders) {
			if (!srcFolder.equals(appClassPath.getTestCodePath())) {
				appClassPath.getAdditionalSourceFolders().add(srcFolder);
			}
		}

		this.generateTrace(appClassPath);
	}

	/*
	 * generate benchmark or example after generating trace
	 */
	protected void generateTrace(final AppJavaClassPath appClassPath) {
		try {

			Job job = new Job("Preparing for Debugging ...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Settings.interestedVariables.clear();
					Settings.potentialCorrectPatterns.clear();

					try {
						monitor.beginTask("Construct Trace Model", 100);

						List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
						List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
						InstrumentationExecutor executor = new InstrumentationExecutor(appClassPath,
								generateTraceDir(appClassPath), "trace", includedClassNames, excludedClassNames);
						final RunningInfo result = executor.run();
						Trace mainTrace = result.getMainTrace();

						/*
						 * generate benchmark or example after getting trace
						 */
//						generateDefPositiveExample(mainTrace);
//						generateDefNegtiveExample(mainTrace);
						generateDefBenchmark(mainTrace);
						generateDefBenchmark_Neg(mainTrace);

						monitor.worked(80);
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								TraceView traceView = MicroBatViews.getTraceView();
								Trace trace = result.getMainTrace();
								trace.setAppJavaClassPath(appClassPath);

								List<Trace> traces = result.getTraceList();

								traceView.setMainTrace(trace);
								traceView.setTraceList(traces);
								traceView.updateData();
							}
						});
					} catch (StepLimitException e) {
						System.out.println("Step limit exceeded");
						e.printStackTrace();
					} catch (Exception e) {
						System.out.println("Debug failed");
						e.printStackTrace();
					} finally {
						monitor.done();
					}

					return Status.OK_STATUS;
				}
			};
			job.schedule();

			try {
				job.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void generateDefBenchmark(Trace trace) {
		if (trace == null) {
			return;
		}
		List<HashMap<String, String>> result = new ArrayList<>();

		for (int i = 0; i < trace.getExecutionList().size(); i++) {
			TraceNode step = trace.getExecutionList().get(i);
			Set<String> invokedMethods = getInvokedMethodsToBeChecked(step.getInvokingMethod());
			if (invokedMethods.isEmpty()) {
				continue;
			}

			for (VarValue targetVar : step.getWrittenVariables()) {
				String fieldName = targetVar.getVarName();
				if (fieldName.startsWith("arg") || fieldName.startsWith("ConditionResult") || fieldName.contains("#")) {
					continue;
				}
				if (isRootVar(targetVar, step)) {
					continue;
				}

				String fullFieldName = targetVar.getVarName();
				VarValue rootVar = targetVar; // here "rootVar" is a pointer, and finally it points to the actual
												// rootVar
				Set<VarValue> visited = new HashSet<VarValue>();
				visited.add(targetVar);

				/*
				 * starting at targetVar, find its parent, until reach to a variable that has no
				 * parent -> rootVar
				 */
				while (true) {
					boolean found = false;
					if (step.getAllVariables().isEmpty()) {
						break;
					}
					for (VarValue var : step.getAllVariables()) {
						if (visited.contains(var)) {
							continue;
						}
						if (isDirectChild(rootVar, var)) { // find parent of current var
							found = true;
							fullFieldName = var.getVarName() + "." + fullFieldName;
							rootVar = var;
							visited.add(var);
							break;
						}
					}
					if (!found) { // reached rootVar
						break;
					}
				}

				if (rootVar == targetVar) {
					continue;
				}

				// rootVar targetVar fullFieldName
				HashMap<String, String> e = new HashMap<>();
				e.put("Order", String.valueOf(step.getOrder()));
				e.put("FieldName", fullFieldName);
				e.put("RootVarName", rootVar.getVarName());
				e.put("RootVarStructure", rootVar.toJSON().toString());
				e.put("Written", "T");
				result.add(e);
			}
		}

		JSONArray jsonArray = new JSONArray();
		for (HashMap<String, String> map : result) {
			JSONObject jsonObject = new JSONObject(map);
			jsonArray.put(jsonObject);
		}

		String className = Settings.launchClass;
		className = className.substring(className.indexOf(".") + 1);

		try {
			Files.write(Paths.get(BENCHMARK_FOLDER_PATH + "\\benchmark", className + ".json"),
					jsonArray.toString(4).getBytes());
			System.out.println("JSON file created successfully.");
		} catch (IOException e) {
			System.out.println("An error occurred while writing JSON to file: " + e.getMessage());
		}
	}

	private void generateDefBenchmark_Neg(Trace trace) {
		if (trace == null) {
			return;
		}
		List<HashMap<String, String>> result = new ArrayList<>();

		for (int i = 0; i < trace.getExecutionList().size(); i++) {
			TraceNode step = trace.getExecutionList().get(i);
			Set<String> invokedMethods = getInvokedMethodsToBeChecked(step.getInvokingMethod());
			if (invokedMethods.isEmpty()) {
				continue;
			}

			for (VarValue targetVar : step.getReadVariables()) {
				String fieldName = targetVar.getVarName();
				if (fieldName.startsWith("arg") || fieldName.startsWith("ConditionResult") || fieldName.contains("#")) {
					continue;
				}
				if (isRootVar(targetVar, step)) {
					continue;
				}
				if (containsVar(step.getWrittenVariables(), targetVar)) {
					continue;
				} // only read

				String fullFieldName = targetVar.getVarName();
				VarValue rootVar = targetVar;
				Set<VarValue> visited = new HashSet<VarValue>();
				visited.add(targetVar);
				while (true) {
					boolean found = false;
					if (step.getAllVariables().isEmpty()) {
						break;
					}
					for (VarValue var : step.getAllVariables()) {
						if (visited.contains(var)) {
							continue;
						}
						if (isDirectChild(rootVar, var)) {
							found = true;
							fullFieldName = var.getVarName() + "." + fullFieldName;
							rootVar = var;
							visited.add(var);
							break;
						}
					}
					if (!found) {
						break;
					}
				}

				if (rootVar == targetVar) {
					continue;
				}

				// rootVar targetVar fullFieldName
				HashMap<String, String> e = new HashMap<>();
				e.put("Order", String.valueOf(step.getOrder()));
				e.put("FieldName", fullFieldName);
				e.put("RootVarName", rootVar.getVarName());
				e.put("RootVarStructure", rootVar.toJSON().toString());
				e.put("Written", "F");
				result.add(e);
			}
		}

		JSONArray jsonArray = new JSONArray();
		for (HashMap<String, String> map : result) {
			JSONObject jsonObject = new JSONObject(map);
			jsonArray.put(jsonObject);
		}

		String className = Settings.launchClass;
		className = className.substring(className.indexOf(".") + 1);

		try {
			Files.write(Paths.get(BENCHMARK_FOLDER_PATH + "\\benchmark", className + "_Neg.json"),
					jsonArray.toString(4).getBytes());
			System.out.println("JSON file created successfully.");
		} catch (IOException e) {
			System.out.println("An error occurred while writing JSON to file: " + e.getMessage());
		}
	}

	public void generateDefPositiveExample(Trace trace) {
		if (trace == null) {
			return;
		}
		for (int i = 0; i < trace.getExecutionList().size(); i++) {
			TraceNode step = trace.getExecutionList().get(i);
			Set<String> invokedMethods = getInvokedMethodsToBeChecked(step.getInvokingMethod());
			if (invokedMethods.isEmpty()) {
				continue;
			}

			for (VarValue targetVar : step.getWrittenVariables()) {
				String fieldName = targetVar.getVarName();
				if (fieldName.startsWith("arg") || fieldName.startsWith("ConditionResult") || fieldName.contains("#")) {
					continue;
				}

				String fullFieldName = targetVar.getVarName();
				VarValue rootVar = targetVar;
				Set<VarValue> visited = new HashSet<VarValue>();
				visited.add(targetVar);
				while (true) {
					boolean found = false;
					for (VarValue var : step.getAllVariables()) {
						if (isDirectChild(rootVar, var)) {
							found = true;
							fullFieldName = var.getVarName() + "." + fullFieldName;
							rootVar = var;
							visited.add(var);
							break;
						}
					}
					if (!found) {
						break;
					}
				}

				if (rootVar == targetVar) {
					continue;
				}

				// rootVar targetVar fullFieldName
				String result = getSavedInfo(step, rootVar, targetVar, fullFieldName, "T");
				try {
					Files.write(Paths.get(EXAMPLE_FILE), result.getBytes(), StandardOpenOption.CREATE,
							StandardOpenOption.APPEND);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void generateDefNegtiveExample(Trace trace) {
		if (trace == null) {
			return;
		}

		for (int i = 0; i < trace.getExecutionList().size(); i++) {
			TraceNode step = trace.getExecutionList().get(i);
			Set<String> invokedMethods = getInvokedMethodsToBeChecked(step.getInvokingMethod());
			if (invokedMethods.isEmpty()) {
				continue;
			}

			for (VarValue targetVar : step.getReadVariables()) {
				String fieldName = targetVar.getVarName();
				if (fieldName.startsWith("arg") || fieldName.startsWith("ConditionResult") || fieldName.contains("#")) {
					continue;
				}
				if (isRootVar(targetVar, step)) {
					continue;
				}
				if (containsVar(step.getWrittenVariables(), targetVar)) {
					continue;
				} // only read

				String fullFieldName = targetVar.getVarName();
				VarValue rootVar = targetVar;
				Set<VarValue> visited = new HashSet<VarValue>();
				visited.add(targetVar);
				while (true) {
					boolean found = false;
					for (VarValue var : step.getAllVariables()) {
						if (isDirectChild(rootVar, var)) {
							found = true;
							fullFieldName = var.getVarName() + "." + fullFieldName;
							rootVar = var;
							visited.add(var);
							break;
						}
					}
					if (!found) {
						break;
					}
				}

				if (rootVar == targetVar) {
					continue;
				}

				// rootVar targetVar fullFieldName
				String result = getSavedInfo(step, rootVar, targetVar, fullFieldName, "F");
				try {
					Files.write(Paths.get(EXAMPLE_FILE), result.getBytes(), StandardOpenOption.CREATE,
							StandardOpenOption.APPEND);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public HashMap<String, String> getBenchmarkSample(TraceNode step, VarValue rootVar, VarValue targetVar,
			String fullFieldName, boolean written) {
		HashMap<String, String> e = new HashMap<>();
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = processInputStringForLLM(getSourceCodeOfALine(location, lineNo).trim());
		e.put("SourceCode", sourceCode);

		/* variable properties */
		String rootVarName = rootVar.getVarName();

		/* type structure */
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		return e;

	}

	public String getSavedInfo(TraceNode step, VarValue rootVar, VarValue targetVar, String fullFieldName, String gt) {
		StringBuilder s = new StringBuilder();
		String delimiter = "###";

		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = processInputStringForLLM(getSourceCodeOfALine(location, lineNo).trim());

		/* variable properties */
		String rootVarName = rootVar.getVarName();

		/* type structure */
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		/* all variables */
//		Set<VarValue> variablesInStep = step.getAllVariables();

		/* invoked methods to be checked */
		Set<String> invokedMethods = getInvokedMethodsToBeChecked(step.getInvokingMethod());

		s.append(fullFieldName);
		s.append(delimiter);

		s.append(rootVarName);
		s.append(delimiter);

		s.append(jsonString);
		s.append(delimiter);

		s.append(sourceCode);
		s.append(delimiter);

		// invoked methods
		SourceCodeRetriever sourceCodeRetriever = new SourceCodeRetriever();
		if (!invokedMethods.isEmpty()) {
			for (String methodSig : invokedMethods) {
				String methodSourceCode = methodSig;
				if (SourceCodeDatabase.sourceCodeMap.containsKey(methodSig)) {
					methodSourceCode = SourceCodeDatabase.sourceCodeMap.get(methodSig);
				} else {
					methodSourceCode = sourceCodeRetriever.getMethodCode(methodSig,
							step.getTrace().getAppJavaClassPath());
					SourceCodeDatabase.sourceCodeMap.put(methodSig, methodSourceCode);
				}
				s.append(processInputStringForLLM(methodSourceCode));
				s.append("***");
			}
		}
		s.append(" ");
		s.append(delimiter);

		// read variables
		for (int i = 0; i < step.getReadVariables().size(); i++) {
			VarValue var = step.getReadVariables().get(i);
			if (isRootVar(var, step)) {
				s.append("{" + var.getVarName() + "|" + var.getType() + ":" + var.getStringValue() + "}");
				if (i < step.getReadVariables().size() - 1) {
					s.append(",");
				}
			}
		}
		s.append(delimiter);
		s.append(gt);
		s.append("\n");

		return s.toString();
	}

	public boolean containsVar(List<VarValue> vars, VarValue var) {
		for (VarValue v : vars) {
			if (v.getVarName().equals(var.getVarName())) {
				return true;
			}
		}
		return false;
	}

	public boolean isDirectChild(VarValue child, VarValue parent) {
		for (VarValue v : parent.getChildren()) {
			if (child.getVarName().equals(v.getVarName())) {
				return true;
			}
		}
		return false;
	}

	public boolean isRootVar(VarValue var, TraceNode step) {
		for (VarValue v : step.getAllVariables()) {
			if (isDirectChild(var, v)) {
				return false;
			}
		}
		return true;

	}

	public static Set<String> getInvokedMethodsToBeChecked(String invokingMethods) {
		Set<String> methods = new HashSet<String>();

		String[] invokedMethods = invokingMethods.split("%");
		for (String methodSig : invokedMethods) {
			if (methodSig == null || !methodSig.contains("#")) {
				continue;
			}
			String type = methodSig.split("#")[0];
			if (shouldBeChecked(type)) {
				methods.add(methodSig);
			}
		}

		return methods;
	}

	public static boolean shouldBeChecked(String className) {
		if (className == null || isPrimitiveType(className) || isString(className)
				|| className.equals("java.lang.Object")) {
			return false;
		}

		if (isArray(className)) {
			return true;
		}

		return className.contains(".");
	}

	public static boolean isPrimitiveType(String className) {
		Set<String> primitiveTypes = new HashSet<>(
				Arrays.asList("int", "long", "short", "byte", "char", "boolean", "float", "double", "void"));
		return className != null && primitiveTypes.contains(className);
	}

	public static boolean isString(String className) {
		return className != null && (className.equals("java.lang.String") || className.equals("String"));
	}

	public static boolean isArray(String className) {
		return className.endsWith("[]");
	}

	public static String getSourceCodeOfALine(String filePath, int lineNumber) {
		String line = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			int currentLine = 0;
			while ((line = reader.readLine()) != null) {
				currentLine++;
				if (currentLine == lineNumber) {
					return line;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String processInputStringForLLM(String input) {
//		return input.replace("\n", "\\n").replace("\r", "\\r").replace("<>", "\\<\\>");
		return input.replace("\n", "\\n").replace("\r", "\\r");
	}

	protected String generateTraceDir(AppJavaClassPath appPath) {
		String traceFolder;
		if (appPath.getOptionalTestClass() != null) {
			traceFolder = FileUtils.getFilePath(MicroBatUtil.getTraceFolder(), Settings.projectName,
					appPath.getOptionalTestClass(), appPath.getOptionalTestMethod());
		} else {
			traceFolder = FileUtils.getFilePath(MicroBatUtil.getTraceFolder(), Settings.projectName,
					appPath.getLaunchClass());
		}
		FileUtils.createFolder(traceFolder);
		return traceFolder;
	}

	class MethodFinder extends ASTVisitor {
		CompilationUnit cu;
		MethodDeclaration methodDeclaration;
		int lineNumber;

		public MethodFinder(CompilationUnit cu, int lineNumber) {
			super();
			this.cu = cu;
			this.lineNumber = lineNumber;
		}

		public boolean visit(MethodDeclaration md) {
			int startLine = cu.getLineNumber(md.getStartPosition());
			int endLine = cu.getLineNumber(md.getStartPosition() + md.getLength());

			if (startLine <= lineNumber && lineNumber <= endLine) {
				methodDeclaration = md;
			}

			return false;
		}
	}

	private String convertSignature(String classQulifiedName, int lineNumber, AppJavaClassPath appPath) {
		CompilationUnit cu = JavaUtil.findCompilationUnitInProject(classQulifiedName, appPath);

		MethodFinder finder = new MethodFinder(cu, lineNumber);
		cu.accept(finder);

		MethodDeclaration methodDeclaration = finder.methodDeclaration;
		IMethodBinding mBinding = methodDeclaration.resolveBinding();
		String methodSig = JavaUtil.generateMethodSignature(mBinding);

		return methodSig;
	}
}
