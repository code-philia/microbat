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

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = createReplayJob();
		job.schedule();
		return null;
	}
	
	public ConcurrentReplayJob createReplayJob() {
		return new ConcurrentReplayJob();
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
