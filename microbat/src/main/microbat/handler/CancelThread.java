package microbat.handler;

import org.eclipse.core.runtime.IProgressMonitor;

import microbat.codeanalysis.runtime.InstrumentationExecutor;

/**
 * Used to check if the job is canceled
 * @author Gabau
 *
 */
public class CancelThread extends Thread {
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
				if (executor != null) executor.interrupt();
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