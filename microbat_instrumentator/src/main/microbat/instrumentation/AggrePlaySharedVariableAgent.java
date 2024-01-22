package microbat.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Map;

import microbat.instrumentation.instr.SystemClassTransformer;
import microbat.instrumentation.instr.aggreplay.SharedVariableTransformer;
import microbat.instrumentation.instr.aggreplay.TimeoutThread;
import microbat.instrumentation.model.generator.IdGenerator;
import microbat.instrumentation.model.generator.ObjectIdGenerator;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.storage.FileStorage;
import microbat.instrumentation.model.storage.Storable;

/**
 * Dynamically determines whether a memory location is shared or not
 * @author Gabau
 *
 */
public class AggrePlaySharedVariableAgent extends Agent {
	
	private ClassFileTransformer transformer = new SharedVariableTransformer();
	private ThreadIdGenerator threadGenerator = new ThreadIdGenerator();
	private ObjectIdGenerator objectIdGenerator = new ObjectIdGenerator();
	private static AggrePlaySharedVariableAgent agent = new AggrePlaySharedVariableAgent();
	private TimeoutThread timeoutThread = new TimeoutThread();
	
	/**
	 * Generates id for thread on thread start.
	 * @param thread
	 */
	public static void _onThreadStart(Thread thread) {
		agent.threadGenerator.createId(thread);
	}
	
	/**
	 * Generate object id on object creation
	 * @param object
	 */
	public static void _onObjectCreation(Object object) {
		agent.objectIdGenerator.createId(object);
	}
	
	
	public static void _onObjectAccess(Object object, String field) {
		agent.objectIdGenerator.getId(object).addAccess(Thread.currentThread().getId(), field);
	}

	@Override
	public void startup0(long vmStartupTime, long agentPreStartup) {
		SystemClassTransformer.attachThreadId(getInstrumentation());
		agent.timeoutThread.setDaemon(true);
		agent.timeoutThread.start();
	}

	@Override
	public void shutdown() throws Exception {
		// TODO Auto-generated method stub
		write();
		AgentLogger.debug("Ended program");
	}	
	
	private void write() {
		// todo: read this from args
		FileStorage fileStorage = new FileStorage("temp.txt");
		HashSet<Storable> toStoreHashSet = new HashSet<>();
		toStoreHashSet.add(agent.threadGenerator);
		toStoreHashSet.addAll(agent.objectIdGenerator.generateToStoreHashSet());
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
