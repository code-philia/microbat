package microbat.handler.replayexp;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.StepLimitException;
import microbat.handler.CancelThread;
import microbat.handler.InstrumentExecutorSupplierImpl;
import microbat.instrumentation.output.RunningInfo;
import microbat.model.trace.Trace;
import microbat.util.Settings;
import microbat.views.MicroBatViews;
import microbat.views.TraceView;
import sav.common.core.utils.SingleTimer;

public class NormalTraceJob extends ReplayJob {
	
	
	boolean finishedExecution = false;
	
	public NormalTraceJob() {
		super("Run normal trace");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SingleTimer timer = SingleTimer.start("Run replay");
		InstrumentationExecutor executor = new InstrumentExecutorSupplierImpl().get();
		CancelThread ct = new CancelThread(monitor, executor);
		File concDumpFile = null;
		File outputFile = null;
		// the absolute path to the dump file.
		String concFileNameString = null;
		if (Settings.concurrentDumpFile.isPresent()) {
			concFileNameString = Settings.concurrentDumpFile.get();
			concDumpFile = new File(concFileNameString);
			System.out.println("Used recording in " + concFileNameString);
		}
		try {
			if (concDumpFile == null) {
				concDumpFile = File.createTempFile("concTemp", ".txt");
			}
			outputFile = File.createTempFile("outputFile", ".txt");
			concFileNameString = concDumpFile.getPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ct.start();
		RunningInfo result = null;
		try {
			result = executor.run();
			this.stats.updateFromRunningInfo(result);
		} catch (StepLimitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ct.stopMonitoring();
		long executionTime = timer.getExecutionTime();
		this.stats.setRunTime(executionTime);
		// when there is a trace avail
		if (result != null && result.getMainTrace() != null) {
			Trace mainTrace = result.getMainTrace();
			this.stats.memoryUsed = mainTrace.getMemoryUsed();
			this.stats.setRunTime(mainTrace.getConstructTime());
		}
		this.stats.setDumpFileSize(-1);
		this.stats.setTraceFileSize(outputFile.length());
		this.stats.setStdError(executor.getProcessError());
		// TODO Auto-generated method stub
		return org.eclipse.core.runtime.Status.OK_STATUS;
	}
	

}
