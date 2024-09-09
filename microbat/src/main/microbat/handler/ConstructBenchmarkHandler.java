package microbat.handler;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import microbat.codeanalysis.runtime.Condition;
import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.StepLimitException;
import microbat.evaluation.junit.TestCaseAnalyzer;
import microbat.instrumentation.output.RunningInfo;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.preference.AnalysisScopePreference;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import sav.common.core.utils.FileUtils;
import sav.strategies.dto.AppJavaClassPath;

public class ConstructBenchmarkHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job(DebugPilotHandler.JOB_FAMALY_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				execute();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
	
	public void execute() {
		System.out.println("Construct RQ2 benchmark");
		final AppJavaClassPath appClassPath = MicroBatUtil.constructClassPaths();
		if (Settings.isRunTest) {
			appClassPath.setOptionalTestClass(Settings.launchClass);
			appClassPath.setOptionalTestMethod(Settings.testMethod);
			appClassPath.setLaunchClass(TestCaseAnalyzer.TEST_RUNNER);
			appClassPath.setTestCodePath(MicroBatUtil.getSourceFolder(Settings.launchClass, Settings.projectName));
		}
		List<String> srcFolders = MicroBatUtil.getSourceFolders(Settings.projectName);
		appClassPath.setSourceCodePath(appClassPath.getTestCodePath());
		for (String srcFolder : srcFolders) {
			if (!srcFolder.equals(appClassPath.getTestCodePath())) {
				appClassPath.getAdditionalSourceFolders().add(srcFolder);
			}
		}
		Trace trace = this.generateTrace(appClassPath, null);
		
		String className = Settings.launchClass.substring(Settings.launchClass.indexOf(".") + 1);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			Path gtPath = Paths.get("C:\\Users\\Kwy\\Desktop\\RQ2\\RQ2_missing", className + ".json");
			List<List<String>> gtData = objectMapper.readValue(new File(gtPath.toString()),
					new TypeReference<List<List<String>>>() {});

			ArrayList<HashMap<String, String>> result = new ArrayList<>();

			for (List<String> d : gtData) {
				int idx = Integer.parseInt(d.get(0));
				TraceNode node = trace.getExecutionList().get(idx - 1);

				boolean found = false;
				for (VarValue readVar : node.getReadVariables()) {
					if (readVar.getVarName().equals(d.get(2))) {
						found = true;
						break;
					}
				}
				if (found) {
					continue;
				}

				for (VarValue readVar : node.getReadVariables()) {
					if(readVar.getVarName().equals(d.get(3))) {
						Condition condition = new Condition(readVar.getVarName(), readVar.getType(), readVar.getStringValue(), "null");
						System.out.println("Re-execution of condition: "+condition);
						Trace newTrace = generateTrace(appClassPath, condition);
						VarValue varExpanded = condition.getMatchedGroundTruthVar(newTrace);
						if(varExpanded == null) {
							continue;
						}
						HashMap<String, String> mp = new HashMap<>();
						mp.put("Order", d.get(0));
						mp.put("Dependency", d.get(1));
						mp.put("Name", readVar.getVarName());
						mp.put("Type", readVar.getType());
						mp.put("Value", readVar.getStringValue());
						mp.put("Expansion", varExpanded.toJSON().toString());
						mp.put("Field", d.get(2));
						result.add(mp);
						break;
					}
//					Condition condition = new Condition(readVar.getVarName(), readVar.getType(), readVar.getStringValue(), "null");
//					System.out.println("Re-execution of condition: "+condition);
//					Trace newTrace = generateTrace(appClassPath, condition);
//					VarValue varExpanded = condition.getMatchedGroundTruthVar(newTrace);
//					if(varExpanded == null) {
//						continue;
//					}
//					if (containVar(d.get(2), varExpanded)) {
//						HashMap<String, String> mp = new HashMap<>();
//						mp.put("Order", d.get(0));
//						mp.put("Dependency", d.get(1));
//						mp.put("Name", readVar.getVarName());
//						mp.put("Type", readVar.getType());
//						mp.put("Value", readVar.getStringValue());
//						mp.put("Expansion", varExpanded.toJSON().toString());
//						mp.put("Field", d.get(2));
//						result.add(mp);
//						break;
//					}
				}

			}

			Path gtPath1 = Paths.get("C:\\Users\\Kwy\\Desktop\\RQ2\\RQ2_benchmark", className + ".json");
			objectMapper.writeValue(new File(gtPath1.toString()), result);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected Trace generateTrace(final AppJavaClassPath appClassPath, Condition condition) {
		List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
		List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
		InstrumentationExecutor executor;
		if (condition == null) {
			executor = new InstrumentationExecutor(appClassPath, generateTraceDir(appClassPath), "trace",
					includedClassNames, excludedClassNames);
		} else {
			executor = new InstrumentationExecutor(appClassPath, generateTraceDir(appClassPath), "trace",
					includedClassNames, excludedClassNames, condition);
		}

		try {
			final RunningInfo result = executor.run();
			if (result == null) {
				return null;
			}
			Trace trace = result.getMainTrace();
			trace.setAppJavaClassPath(appClassPath);
			return trace;
		} catch (StepLimitException e) {
			e.printStackTrace();
		}
		return null;
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

	protected boolean containVar(String target, VarValue var) {
		String simpleTarget = "";
		if(target.contains("[")) {
			simpleTarget = target.substring(0,target.indexOf("["));
		}
			
		if (target.equals(var.getVarName()) || simpleTarget.equals(var.getVarName())) {
			return true;
		}
		for (VarValue child : var.getChildren()) {
			if (containVar(target, child)) {
				return true;
			}
		}
		return false;
	}
}
