package microbat.instrumentation.instr.aggreplay;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.runtime.ExecutionTracer;

public class TimeoutThread extends Thread {
	
	private Agent attachedAgent = null;
	public static final String ID = "Timeout-thread$$";
	public static final String TIMEOUT_MSG = "false;Timeout";
	
	public TimeoutThread(Agent attachedAgent) { 
		this.attachedAgent = attachedAgent;
		super.setName(ID);
	}
	
	public TimeoutThread() {
		super.setDaemon(true);
	}
	@Override
	public void run() {
		try {
			Thread.sleep(300000L);
		} catch (InterruptedException e) {
		}
		AgentLogger.debug("Interrupted program due to timeout");
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
