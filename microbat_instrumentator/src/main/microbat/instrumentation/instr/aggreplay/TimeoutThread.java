package microbat.instrumentation.instr.aggreplay;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.runtime.ExecutionTracer;

public class TimeoutThread extends Thread {
	
	private Agent attachedAgent = null;
	public static final String ID = "Timeout-thread$$";
	public static final String TIMEOUT_MSG = "false;Timeout";
	
	private long timeOut = 10000L;
	
	public TimeoutThread(Agent attachedAgent) { 
		this.attachedAgent = attachedAgent;
		super.setName(ID);
		super.setDaemon(true);
	}
	
	public void setTimeout(long timeOut) {
		this.timeOut = timeOut;
	}
	
	public TimeoutThread() {
		super.setName(ID);
		super.setDaemon(true);
	}
	@Override
	public void run() {
		// the timeout is forever
		if (timeOut < 0) return;
		try {
			Thread.sleep(timeOut);
		} catch (InterruptedException e) {
		}
		AgentLogger.debug("Interrupted program due to timeout");
		ExecutionTracer.shutdown();
		Agent._forceProgramStop(TIMEOUT_MSG);
//		if (attachedAgent == null) {
//			Agent._exitProgram("false;Timeout");
//		} else {
////			try {
////				attachedAgent.shutdown();
////			} catch (Exception e) {
////				e.printStackTrace();
////			}
//		}
//		System.exit(1);
	}
}
