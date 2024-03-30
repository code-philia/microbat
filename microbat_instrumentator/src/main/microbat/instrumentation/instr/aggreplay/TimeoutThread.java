package microbat.instrumentation.instr.aggreplay;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.runtime.ExecutionTracer;

public class TimeoutThread extends Thread {
	
	
	public TimeoutThread() {
		super.setDaemon(true);
	}
	@Override
	public void run() {
		try {
			Thread.sleep(30000L);
		} catch (InterruptedException e) {
		}
		AgentLogger.debug("Interrupted program due to timeout");
		Agent._exitProgram("false;Timeout");
		// kill all other threads
		System.exit(1);
	}
}
