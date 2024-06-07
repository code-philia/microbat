package microbat.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import microbat.log.Log;
import microbat.model.trace.Trace;
import microbat.views.MicroBatViews;
import microbat.views.TraceView;

public class RecovAllDependencyHandler extends AbstractHandler {
	
	protected TraceView buggyView;
	
	protected Trace trace;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job(DebugPilotHandler.JOB_FAMALY_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				setup();
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
		
		
	}
	
	protected void setup() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				buggyView = MicroBatViews.getTraceView();
				trace = buggyView.getTrace();
			}
		});
	}

}
