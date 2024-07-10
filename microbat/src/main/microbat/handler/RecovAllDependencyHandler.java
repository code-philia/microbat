package microbat.handler;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

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
import microbat.views.DebugFeedbackView;
import microbat.views.MicroBatViews;
import microbat.views.TraceView;
import sav.strategies.dto.AppJavaClassPath;

public class RecovAllDependencyHandler extends StartDebugHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job(DebugPilotHandler.JOB_FAMALY_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				clearOldData();
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
		Log.printMsg(getClass(), "=========================================");
		Log.printMsg(getClass(), "Running TraceRecov for all steps in trace");
		Log.printMsg(getClass(), "=========================================");
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

		recoverDataDependency(appClassPath);
	}

	private void recoverDataDependency(final AppJavaClassPath appClassPath) {
		Settings.interestedVariables.clear();
		Settings.potentialCorrectPatterns.clear();

		try {

			List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
			List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
			InstrumentationExecutor executor = new InstrumentationExecutor(appClassPath, generateTraceDir(appClassPath),
					"trace", includedClassNames, excludedClassNames);
			final RunningInfo result = executor.run();

			TraceView traceView = MicroBatViews.getTraceView();
			if (result == null) {
				traceView.setMainTrace(null);
				traceView.setTraceList(null);
				return;
			}
			Trace trace = result.getMainTrace();
			trace.setAppJavaClassPath(appClassPath);

			List<Trace> traces = result.getTraceList();

			traceView.setMainTrace(trace);
			traceView.setTraceList(traces);
			traceView.updateData();

			System.out.println("==============================================");
			System.out.println("Start Recovering Data Dependency Exhaustively");
			System.out.println("==============================================");

			ExecutionSimulator executionSimulator = new ExecutionSimulator();
			TraceRecoverer traceRecoverer = new TraceRecoverer();
			AppJavaClassPath appJavaClassPath = trace.getAppJavaClassPath();

			int firstStepNo = 1;
			int lastStepNo = trace.getLatestNode().getOrder();
			for (int i = firstStepNo; i < lastStepNo; i++) {
				System.out.println("Checking Step: " + i);
				TraceNode step = trace.getTraceNode(i);
				List<VarValue> readVars = step.getReadVariables();

				// record data dominators without recovery
				Set<Integer> originalDataDominators = new HashSet<>();
				for (VarValue readVar : readVars) {
					TraceNode dataDom = trace.findProducer(readVar, step);
					if (dataDom != null) {
						originalDataDominators.add(dataDom.getOrder());
					}
				}
				if (!originalDataDominators.isEmpty()) {
					System.out.println("Original Data Dominators:");
					originalDataDominators.stream().forEach(d -> System.out.print(d + ","));
				}

				// record data dominators after recovery
				Set<Integer> dataDominatorsAfterRecovery = new HashSet<>();
				for (VarValue readVar : readVars) {
					if (TraceRecovUtils.shouldBeChecked(readVar.getType())
							&& TraceRecovUtils.isUnrecorded(readVar.getType(), appJavaClassPath)) {
						try {
							// variable expansion
							executionSimulator.expandVariable(readVar, step, null);

							for (VarValue targetVar : readVar.getAllDescedentChildren()) {
								// find all data dominators
								traceRecoverer.recoverDataDependency(step, targetVar, readVar);
								TraceNode dataDominator = trace.findProducer(readVar, step);
								if (dataDominator != null) {
									dataDominatorsAfterRecovery.add(dataDominator.getOrder());
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}
				if (!dataDominatorsAfterRecovery.isEmpty()) {
					System.out.println("Data Dominators after Recovery:");
					dataDominatorsAfterRecovery.stream().forEach(d -> System.out.print(d + ","));
				}

			}
		} catch (StepLimitException e) {
			System.out.println("Step limit exceeded");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Debug failed");
			e.printStackTrace();
		}
	}

	private void clearOldData() {
		Settings.interestedVariables.clear();
		Settings.wrongPathNodeOrder.clear();
//		Settings.localVariableScopes.clear();
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

}
