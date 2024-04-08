package microbat.handler.replayexp;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.handler.CancelThread;
import microbat.handler.InstrumentExecutorSupplierImpl;
import microbat.handler.ReplayStats;
import microbat.instrumentation.output.RunningInfo;
import microbat.model.trace.Trace;
import microbat.util.Settings;
import microbat.views.MicroBatViews;
import microbat.views.TraceView;
import sav.common.core.utils.SingleTimer;

public class ConcurrentReplayJob extends ReplayJob {
	
	boolean finishedExecution = false;
	boolean requireVisualisation = true;
	public ConcurrentReplayJob() {
		super("Run replay");
	}
	
	public static ConcurrentReplayJob withoutVis() {
		ConcurrentReplayJob job = new ConcurrentReplayJob();
		job.requireVisualisation = false;
		return job;
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
		RunningInfo result = executor.runReplayTracer(concFileNameString, outputFile.getPath(), Settings.stepLimit);
		Trace mainTrace = result.getMainTrace();
		if (mainTrace != null) {
			this.stats.memoryUsed = mainTrace.getMemoryUsed();
		}
		ct.stopMonitoring();
		long executionTime = timer.getExecutionTime();
		this.stats.setRunTime(executionTime);
		this.stats.setDumpFileSize(concDumpFile.length());
		this.stats.setTraceFileSize(outputFile.length());
		this.stats.setStdError(executor.getProcessError());
		if (requireVisualisation) {
			Display.getDefault().asyncExec(new Runnable() {
				
				@Override
				public void run() {
					TraceView traceView = MicroBatViews.getTraceView();
					if (result == null) {
						traceView.setMainTrace(null);
						traceView.setTraceList(null);
						return;
					}
					Trace trace = result.getMainTrace();
					trace.setAppJavaClassPath(executor.getAppPath());
					List<Trace> traces = result.getTraceList();
					
					traceView.setMainTrace(trace);
					traceView.setTraceList(traces);
					traceView.updateData();
				}
			});	
		}
		
		// TODO Auto-generated method stub
		return org.eclipse.core.runtime.Status.OK_STATUS;
	}
	
	public ReplayStats getReplayStats() {
		return this.stats;
	}
}
