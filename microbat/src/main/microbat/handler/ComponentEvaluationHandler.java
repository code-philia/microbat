package microbat.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import microbat.log.Log;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.preference.AnalysisScopePreference;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.TraceRecoverer;
import microbat.tracerecov.executionsimulator.ExecutionSimulator;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import sav.strategies.dto.AppJavaClassPath;
import sav.common.core.Pair;

public class ComponentEvaluationHandler extends StartDebugHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job(DebugPilotHandler.JOB_FAMALY_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				execute();
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

	protected void execute() {
		Log.printMsg(getClass(), "");
		Log.printMsg(getClass(), "=====================================");
		Log.printMsg(getClass(), "Component Evaluation of Tracerecov");
		Log.printMsg(getClass(), "=====================================");
		Log.printMsg(getClass(), "");

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
		Path bmPath = Paths.get("C:\\Users\\Kwy\\Desktop\\RQ2\\RQ2_benchmark", className + ".json");
		Path gtPath = Paths.get("C:\\Users\\Kwy\\Desktop\\RQ2\\RQ2_groundtruth", className + ".json");

		List<Map<String, String>> benchmark = null;
		Map<String, List<String>> groundtruth = null;
		try {
			benchmark = objectMapper.readValue(new File(bmPath.toString()),
					new TypeReference<List<Map<String, String>>>() {
					});
			groundtruth = objectMapper.readValue(new File(gtPath.toString()),
					new TypeReference<Map<String, List<String>>>() {
					});
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (benchmark == null || groundtruth == null) {
			Log.printMsg(getClass(), "ERROR: load benchmark or groundtruth failed");
			return;
		}

		evaluateComponent(trace, benchmark, groundtruth);

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

	protected void evaluateComponent(Trace trace, List<Map<String, String>> benchmark,
			Map<String, List<String>> groundtruth) {
		// var-expansion
		Set<String> checkedVar = new HashSet<String>();
		List<Double> precisionList = new ArrayList<Double>();
		List<Double> recallList = new ArrayList<Double>();

		// def-inference
		Map<Integer, Set<Integer>> predictedDataDep = new HashMap<>();
		int succ = 0;

		// for each selected data dependency to be recovered
		for (Map<String, String> element : benchmark) {
			int currentOrder = Integer.valueOf(element.get("Order"));
			TraceNode currentStep = trace.getExecutionList().get(currentOrder - 1);

			// variable expansion score
			VarValue readVar = null;
			for (VarValue var : currentStep.getReadVariables()) {
				if (var.getVarName().equals(element.get("Name"))) {
					String gtExpansion = element.get("Expansion");
					String prediction = null;

					ExecutionSimulator simulator = new ExecutionSimulator();
					try {
						prediction = simulator.expandVariable(var, currentStep, null);
					} catch (IOException e) {
						e.printStackTrace();
					}

					if (prediction == null) {
						Log.printMsg(getClass(), "ERROR: no proper prediction!");
						break;
					}
					Pair<Double, Double> pr = calculatePR(gtExpansion, prediction);
					precisionList.add(pr.first());
					recallList.add(pr.second());
					checkedVar.add(var.getVarName() + ":" + var.getStringValue());

					readVar = var;
					break;
				}
			}

			// definition inference precision & score(pr)
			if (!predictedDataDep.containsKey(currentOrder)) {
				TraceRecoverer traceRecoverer = new TraceRecoverer();
				Set<Integer> dataDominatorsAfterRecovery = new HashSet<>();

				for (VarValue targetVar : readVar.getAllDescedentChildren()) {
					traceRecoverer.recoverDataDependency(currentStep, targetVar, readVar);
					TraceNode dataDominator = trace.findProducer(readVar, currentStep);
					if (dataDominator != null) {
						dataDominatorsAfterRecovery.add(dataDominator.getOrder());
					}
				}

				predictedDataDep.put(currentOrder, dataDominatorsAfterRecovery);

			}
			Set<Integer> dds = predictedDataDep.get(currentOrder);
			if (dds.contains(Integer.valueOf(element.get("Dependency")))) {
				succ += 1;
			}
		}
		
		StringBuilder result = new StringBuilder();
		result.append("Checked var_value number:"+checkedVar.size()+"\n");
		result.append("Variable expansion precision list:"+precisionList.toString()+"\n");
		result.append("Variable expansion average precision:"+getAvg(precisionList)+"\n");
		result.append("Variable expansion recall list:"+recallList.toString()+"\n");
		result.append("Variable expansion average recall:"+getAvg(recallList)+"\n");
		
		int total = benchmark.size();
		double rate = (double)succ/total;
		result.append("Checked data dependency number:"+total+"\n");
		result.append("Definition inference successfull number:"+succ+"\n");
		result.append("Definition inference successfull rate:"+rate+"\n");
	}

	protected Pair<Double, Double> calculatePR(String groundtruth, String prediction) {

		return null;
	}
	
	protected double getAvg(List<Double> precisionList) {
		double sum = 0.0;
        if (!precisionList.isEmpty()) {
            for (Double number : precisionList) {
                sum += number;
            }
            double average = sum / precisionList.size();
            return average;
        } else {
            return 0.0;
        }
	}

}
