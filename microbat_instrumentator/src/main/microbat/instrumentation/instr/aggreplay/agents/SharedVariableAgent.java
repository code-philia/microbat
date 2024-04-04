package microbat.instrumentation.instr.aggreplay.agents;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.print.CancelablePrintJob;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.filter.GlobalFilterChecker;
import microbat.instrumentation.instr.SystemClassTransformer;
import microbat.instrumentation.instr.aggreplay.TimeoutThread;
import microbat.instrumentation.instr.aggreplay.output.SharedVariableOutput;
import microbat.instrumentation.instr.aggreplay.shared.BasicTransformer;
import microbat.instrumentation.instr.aggreplay.shared.SharedObjectAccessInstrumentator;
import microbat.instrumentation.model.SharedVariableObjectId;
import microbat.instrumentation.model.generator.IdGenerator;
import microbat.instrumentation.model.generator.ObjectIdGenerator;
import microbat.instrumentation.model.generator.SharedVariableArrayRef;
import microbat.instrumentation.model.generator.SharedVariableObjectGenerator;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.StaticFieldLocation;
import microbat.instrumentation.model.storage.FileStorage;
import microbat.instrumentation.model.storage.Storable;
import microbat.instrumentation.output.StorableWriter;
import microbat.instrumentation.runtime.ExecutionTracer;
import sav.common.core.Pair;
import sav.strategies.dto.AppJavaClassPath;

/**
 * Dynamically determines whether a memory location is shared or not
 * @author Gabau
 *
 */
public class SharedVariableAgent extends Agent {
	
	private ClassFileTransformer transformer = null;
	private SharedVariableObjectGenerator shObjectIdGenerator = new SharedVariableObjectGenerator();
	private static SharedVariableAgent agent = new SharedVariableAgent();
	private AgentParams agentParams = null;
	private TimeoutThread timeoutThread;
	
	
	public static SharedVariableAgent getAgent(CommandLine cmd) {
		agent.agentParams = AgentParams.initFrom(cmd);
		return agent;
	}
	
	protected static boolean cannotRecord() {
		return !ExecutionTracer.isRecordingOrStarted();
	}
	
	// TODO(Gab)
	public static void _onNewArray(Object arrayRef) {
		if (cannotRecord()) return;
		agent.shObjectIdGenerator.createArrayId(arrayRef);
	}

	public static void _onLockAcquire(Object object) {
		
	}
	
	public static void _onArrayAccess(Object arrayRef, int index) {
		if (cannotRecord()) return;
		_assertArrayExists(arrayRef);
		agent.onArrayAccess(arrayRef, index);
	}
	
	private void onArrayAccess(Object arrayRef, int index) {
		SharedVariableArrayRef arrayVal = shObjectIdGenerator.getArrayId(arrayRef);
		arrayVal.addAccess(index, Thread.currentThread().getId());
	}
	
	public static void _assertObjectExists(Object object) {
		if (cannotRecord()) return;
		agent.shObjectIdGenerator.assertId(object);
	}
	
	public static void _assertArrayExists(Object object) {
		if (cannotRecord()) return;
		agent.shObjectIdGenerator.assertArrayId(object);
	}
	
	/**
	 * Generate object id on object creation
	 * @param object
	 */
	public static void _onObjectCreation(Object object) {
		if (cannotRecord()) return;
		agent.shObjectIdGenerator.createId(object);
	}
	
	
	public static void _onObjectAccess(Object object, String field) {
		if (cannotRecord()) return;
		_assertObjectExists(object);
		agent.shObjectIdGenerator.getId(object).addAccess(Thread.currentThread().getId(), field);
	}
	
	public static void _onStaticAccess(String className, String fieldName) {
		if (cannotRecord()) return;
		agent.onStaticAccess(className, fieldName);
	}
	
	private void onStaticAccess(String className, String fieldName) {
		this.shObjectIdGenerator.addAccessStaticField(new StaticFieldLocation(className, fieldName), 
				Thread.currentThread().getId());
	}
	


	@Override
	public void startup0(long vmStartupTime, long agentPreStartup) {
		this.timeoutThread = new TimeoutThread(this);
		timeoutThread.start();
		AppJavaClassPath appPath = agentParams.initAppClassPath();
		GlobalFilterChecker.setup(appPath, agentParams.getIncludesExpression(), agentParams.getExcludesExpression());
		SystemClassTransformer.attachThreadId(getInstrumentation());
	}

	@Override
	public void shutdown() throws Exception {
		write();
		AgentLogger.debug("Ended program");
	}	
	
	private void write() throws Exception {
		StorableWriter writer = new StorableWriter(new File(agentParams.getDumpFile()));
		SharedVariableOutput output = new SharedVariableOutput(shObjectIdGenerator);
		writer.writeStorable(output);
	}
	
	@Override
	public void startTest(String junitClass, String junitMethod) {
		ExecutionTracer._start();
		ExecutionTracer.appJavaClassPath.setOptionalTestClass(junitClass);
		ExecutionTracer.appJavaClassPath.setOptionalTestMethod(junitMethod);
	}

	@Override
	public void finishTest(String junitClass, String junitMethod) {
		ExecutionTracer.shutdown();
	}

	@Override
	public ClassFileTransformer getTransformer0() {
		SharedObjectAccessInstrumentator instrumentator = new SharedObjectAccessInstrumentator(agentParams);
		return new BasicTransformer(instrumentator);
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
