package microbat.handler;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.AbstractHandleObjectEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.google.common.base.Optional;

import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.evaluation.junit.TestCaseAnalyzer;
import microbat.instrumentation.output.RunningInfo;
import microbat.model.trace.Trace;
import microbat.preference.AnalysisScopePreference;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import microbat.views.DebugFeedbackView;
import microbat.views.MicroBatViews;
import microbat.views.TraceView;
import sav.common.core.utils.FileUtils;
import sav.strategies.dto.AppJavaClassPath;

/**
 * 
 * @author Gabau
 *
 */
public class ConcurrentRecordHandler extends AbstractHandler {
	

	/**
	 * Used to check if the job is canceled
	 * @author Gabau
	 *
	 */
	private static class CancelThread extends Thread {
		public boolean stopped = false;
		IProgressMonitor monitor;
		InstrumentationExecutor executor;
		public CancelThread(IProgressMonitor monitor,
				InstrumentationExecutor executor) {
			this.setName("Cancel thread");
			this.monitor = monitor;
			this.executor = executor;
			this.setDaemon(true);
		}
		
		@Override
		public void run() {
			while (!stopped) {
				if (monitor.isCanceled()) {
					executor.interrupt();
					stopped = true;
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		public void stopMonitoring() {
			this.stopped = true;
		}
		
	}
	protected String generateTraceDir(AppJavaClassPath appPath) {
		String traceFolder;
		if (appPath.getOptionalTestClass() != null) {
			traceFolder = FileUtils.getFilePath(MicroBatUtil.getTraceFolder(), 
					Settings.projectName,
					appPath.getOptionalTestClass(), 
					appPath.getOptionalTestMethod());
		} else {
			traceFolder = FileUtils.getFilePath(MicroBatUtil.getTraceFolder(), 
					Settings.projectName, 
					appPath.getLaunchClass()); 
		}
		FileUtils.createFolder(traceFolder);
		return traceFolder;
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {

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
		return executeAggrRecord(event, appClassPath);
		
	}

	
	
	private Object executeAggrRecord(ExecutionEvent event, final AppJavaClassPath appJavaClassPath) {
		List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
		List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
		InstrumentationExecutor executor = new InstrumentationExecutor(appJavaClassPath,
				generateTraceDir(appJavaClassPath), "trace", includedClassNames, excludedClassNames);
		Job runningJob = new Job("Run aggr") {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				String fileName = null;
				File dumpFile = null;
				File concDumpFile = null;
				// the absolute path to the dump file.
				String concFileNameString = null;
				if (Settings.concurrentDumpFile.isPresent()) {
					concFileNameString = Settings.concurrentDumpFile.get();
					concDumpFile = new File(concFileNameString);
				}
				try {
					dumpFile = File.createTempFile("temp", ".txt");
					if (concDumpFile == null) {
						concDumpFile = File.createTempFile("concTemp", ".txt");
						Settings.concurrentDumpFile = Optional.of(concDumpFile.getPath());
					}
					fileName = dumpFile.getPath();
					concFileNameString = concDumpFile.getPath();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				CancelThread ctThread = new CancelThread(monitor, executor);
				ctThread.start();
				executor.runSharedVariable(fileName, Settings.stepLimit);
				executor.runRecordConc(fileName, concFileNameString, Settings.stepLimit);
				ctThread.stopMonitoring();
				return Status.OK_STATUS;
			}
		};
		runningJob.schedule();
		return null;
	}
	
}
