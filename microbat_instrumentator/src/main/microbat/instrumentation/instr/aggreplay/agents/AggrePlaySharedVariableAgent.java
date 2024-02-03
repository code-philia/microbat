package microbat.instrumentation.instr.aggreplay.agents;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Map;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.instr.SystemClassTransformer;
import microbat.instrumentation.instr.aggreplay.TimeoutThread;
import microbat.instrumentation.instr.aggreplay.output.SharedVariableOutput;
import microbat.instrumentation.instr.aggreplay.shared.BasicTransformer;
import microbat.instrumentation.model.SharedVariableObjectId;
import microbat.instrumentation.model.generator.IdGenerator;
import microbat.instrumentation.model.generator.ObjectIdGenerator;
import microbat.instrumentation.model.generator.SharedVariableObjectGenerator;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.storage.FileStorage;
import microbat.instrumentation.model.storage.Storable;

/**
 * Dynamically determines whether a memory location is shared or not
 * @author Gabau
 *
 */
public class AggrePlaySharedVariableAgent extends Agent {
	
	private ClassFileTransformer transformer = new BasicTransformer();
	private SharedVariableObjectGenerator shObjectIdGenerator = new SharedVariableObjectGenerator();
	private static AggrePlaySharedVariableAgent agent = new AggrePlaySharedVariableAgent();
	private AgentParams agentParams = null;
	
	public static AggrePlaySharedVariableAgent getAgent(CommandLine cmd) {
		agent.agentParams = AgentParams.initFrom(cmd);
		return agent;
	}
	
	// TODO(Gab): mark lock objects as shared
	public static void _onLockAcquire(Object object) {

	}
	
	/**
	 * Generate object id on object creation
	 * @param object
	 */
	public static void _onObjectCreation(Object object) {
		agent.shObjectIdGenerator.createId(object);
	}
	
	
	public static void _onObjectAccess(Object object, String field) {
		agent.shObjectIdGenerator.getId(object).addAccess(Thread.currentThread().getId(), field);
	}
	


	@Override
	public void startup0(long vmStartupTime, long agentPreStartup) {
		System.out.println("Started shared variable detection");
		SystemClassTransformer.attachThreadId(getInstrumentation());
	}

	@Override
	public void shutdown() throws Exception {
		// TODO Auto-generated method stub
		write();
		AgentLogger.debug("Ended program");
	}	
	
	private void write() {
		// todo: read this from args
		FileStorage fileStorage = new FileStorage(agentParams.getDumpFile());
		
		HashSet<Storable> toStoreHashSet = new HashSet<>();
		SharedVariableOutput output = new SharedVariableOutput(shObjectIdGenerator);
		toStoreHashSet.add(output);
		toStoreHashSet.add(ThreadIdGenerator.threadGenerator);
		fileStorage.store(toStoreHashSet);
		
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
		return transformer;
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
