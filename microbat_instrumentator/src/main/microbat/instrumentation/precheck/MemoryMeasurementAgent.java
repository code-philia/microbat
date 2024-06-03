package microbat.instrumentation.precheck;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.instr.aggreplay.TimeoutThread;
import microbat.instrumentation.instr.aggreplay.shared.BasicTransformer;

public class MemoryMeasurementAgent extends Agent {
	
	private TimeoutThread timeoutThread;
	private static MemoryMeasurementAgent attachedAgent = null;
	private AgentParams agentParams;
	public static MemoryMeasurementAgent getMeasurementAgent(CommandLine cmLine) {
		if (attachedAgent == null) {
			attachedAgent = new MemoryMeasurementAgent();
			attachedAgent.agentParams = AgentParams.initFrom(cmLine);
		}
		return attachedAgent;
	}
	
	@Override
	public void startup0(long vmStartupTime, long agentPreStartup) {
		this.timeoutThread = new TimeoutThread(this);
		timeoutThread.start();
	}

	@Override
	public void shutdown() throws Exception {
		long memoryUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		File dumpFile = new File(agentParams.getDumpFile());
		FileWriter fw = new FileWriter(dumpFile);
		fw.write(memoryUsed + "");
		fw.flush();
		fw.close();
	}

	@Override
	public void startTest(String junitClass, String junitMethod) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finishTest(String junitClass, String junitMethod) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ClassFileTransformer getTransformer0() {
		return new ClassFileTransformer() {
		
		};
	}

	@Override
	public void retransformBootstrapClasses(Instrumentation instrumentation, Class<?>[] retransformableClasses)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exitTest(String testResultMsg, String junitClass, String junitMethod, long threadId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isInstrumentationActive0() {
		// TODO Auto-generated method stub
		return false;
	}

}
