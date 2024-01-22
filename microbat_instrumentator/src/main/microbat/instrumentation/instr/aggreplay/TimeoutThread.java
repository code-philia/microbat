package microbat.instrumentation.instr.aggreplay;

import microbat.instrumentation.AgentLogger;

public class TimeoutThread extends Thread {
	@Override
	public void run() {
		try {
			Thread.sleep(2000L);
		} catch (InterruptedException e) {
		}
		AgentLogger.debug("Interrupted program due to timeout");
		// kill all other threads
		System.exit(0);
	}
}
