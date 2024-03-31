package microbat.instrumentation.instr.aggreplay;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.runtime.ExecutionTracer;

public class TimeoutThread extends Thread {
	
	private Agent attachedAgent;
	
	public TimeoutThread(Agent attachedAgent) { 
		this.attachedAgent = attachedAgent;
	}
	
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
		if (attachedAgent != null) {
			Agent._exitProgram("false;Timeout");
		} else {
//			try {
//				attachedAgent.shutdown();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
		}
		System.exit(1);
	}
}
