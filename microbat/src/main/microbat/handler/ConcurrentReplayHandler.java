package microbat.handler;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter.Status;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.instrumentation.output.RunningInfo;
import microbat.model.trace.Trace;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import microbat.views.MicroBatViews;
import microbat.views.TraceView;
import sav.common.core.utils.FileUtils;
import sav.strategies.dto.AppJavaClassPath;

public class ConcurrentReplayHandler extends AbstractHandler {
	
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

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Job job = new Job("Replay aggr") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				InstrumentationExecutor executor = new InstrumentExecutorSupplierImpl().get();
				CancelThread ct = new CancelThread(monitor, executor);
				File concDumpFile = null;
				File outputFile = null;
				// the absolute path to the dump file.
				String concFileNameString = null;
				if (Settings.concurrentDumpFile.isPresent()) {
					concFileNameString = Settings.concurrentDumpFile.get();
					concDumpFile = new File(concFileNameString);
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
				ct.stopMonitoring();
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
				// TODO Auto-generated method stub
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
			
		};
		job.schedule();
		return null;
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

}
