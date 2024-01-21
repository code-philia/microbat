package microbat.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import microbat.instrumentation.model.generator.IdGenerator;
import microbat.instrumentation.model.generator.ObjectIdGenerator;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.model.id.ListId;
import microbat.instrumentation.model.id.ObjectId;

/**
 * Dynamically determines whether a memory location is shared or not
 * @author Gabau
 *
 */
public class AggrePlaySharedVariableAgent extends Agent {
	
	private ClassFileTransformer transformer;
	private IdGenerator<Thread, ListId> threadGenerator = new ThreadIdGenerator();
	private IdGenerator<Object, ObjectId> objectIdGenerator = new ObjectIdGenerator();
	private static AggrePlaySharedVariableAgent agent = new AggrePlaySharedVariableAgent();
	
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown() throws Exception {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		return null;
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
