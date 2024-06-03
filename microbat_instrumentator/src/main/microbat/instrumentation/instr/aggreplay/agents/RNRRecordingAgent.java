package microbat.instrumentation.instr.aggreplay.agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.filter.GlobalFilterChecker;
import microbat.instrumentation.instr.SystemClassTransformer;
import microbat.instrumentation.instr.aggreplay.TimeoutThread;
import microbat.instrumentation.instr.aggreplay.output.SharedVariableOutput;
import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.RecordingOutput;
import microbat.instrumentation.instr.aggreplay.shared.SharedDataParser;
import microbat.instrumentation.model.generator.ObjectIdGenerator;
import microbat.instrumentation.model.generator.SharedMemoryGenerator;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.storage.FileStorage;
import microbat.instrumentation.model.storage.Storable;
import microbat.instrumentation.output.StorableReader;
import microbat.instrumentation.output.StorableWriter;
import microbat.instrumentation.runtime.ExecutionTracer;
import sav.strategies.dto.AppJavaClassPath;

/**
 * Abstract class representing a recording class
 * @author Gabau
 *
 */
public abstract class RNRRecordingAgent extends Agent {
	private static RNRRecordingAgent recordingAgent;
	protected ObjectIdGenerator objectIdGenerator = new ObjectIdGenerator();
	protected SharedMemoryGenerator sharedGenerator = new SharedMemoryGenerator(objectIdGenerator);
	protected ThreadLocal<Event> lastEvent = ThreadLocal.withInitial(new Supplier<Event>() {
		@Override
		public Event get() {
			return null;
		}
	});
	protected AgentParams agentParams;
	protected TimeoutThread timeoutThread;
	

	protected static boolean cannotRecord() {
		return !ExecutionTracer.isRecordingOrStarted();
	}
	
	protected void assertObjectExists(Object object) {
		if (objectIdGenerator.getId(object) != null) {
			return;
		}
		objectIdGenerator.createId(object);
	}
	
	public static void _assertObjectExists(Object object) {
		if (cannotRecord()) return;
		recordingAgent.assertObjectExists(object);
	}
	
	public static void _assertArrayExists(Object object) {
		if (cannotRecord()) return;
		recordingAgent.assertArrayExists(object);
	}
	
	protected void assertArrayExists(Object object) {
		sharedGenerator.assertArray(object);
	}
	
	
	public static void _acquireLock() {
		if (cannotRecord()) return;
		recordingAgent.acquireLock();
	}
	
	public static void _releaseLock() {
		if (cannotRecord()) return;
		recordingAgent.releaseLock();
	}
	
	public static RNRRecordingAgent getAttached(CommandLine cml) {
		if (recordingAgent != null) {
			recordingAgent.agentParams = AgentParams.initFrom(cml);
			return recordingAgent;
		}
		recordingAgent = new LaxRecordingAgent(cml);
		return recordingAgent;
	}
	
	
	protected RNRRecordingAgent(CommandLine cml) {
		this.agentParams = AgentParams.initFrom(cml);
	}
	
	public static void _onNewObject(Object object) {
		if (cannotRecord()) return;
		recordingAgent.onObjectCreate(object);
	}
	
	public static void _onNewArray(Object obj) {
		if (cannotRecord()) return;
		recordingAgent.onNewArray(obj);
	}
	
	private void onNewArray(Object obj) {
		sharedGenerator.newArray(obj);
	}
	
	public static void _onObjectRead(Object object, String field) {
		if (cannotRecord()) return;
		_assertObjectExists(object);
		recordingAgent.onObjectRead(object, field);
	}
	
	public static void _onObjectWrite(Object object, String field) {
		if (cannotRecord()) return;
		_assertObjectExists(object);
		recordingAgent.onObjectWrite(object, field);
	}
	
	
	public static void _afterObjectWrite() {
		if (cannotRecord()) return;
		recordingAgent._afterObjectWriteInner();
	}
	
	
	
	
	private void _afterObjectWriteInner() {
		if (this.lastEvent.get() == null) {
			return;
		}
		this.afterWrite();
	}
	
	private void _afterObjectReadInner() {
		if (this.lastEvent.get() == null) {
			return;
		}
		this.afterRead();
	}
	
	public static void _afterObjectRead() {
		if (cannotRecord()) return;
		recordingAgent._afterObjectReadInner();
	}
	
	public static void _onStaticRead(String className, String fieldName) {
		if (cannotRecord()) return;
		recordingAgent.onStaticRead(className, fieldName);
	}
	
	public static void _onStaticWrite(String className, String fieldName) {
		if (cannotRecord()) return;
		recordingAgent.onStaticWrite(className, fieldName);
	}
	
	public static void _onArrayRead(Object arrayRef, int index) {
		if (cannotRecord()) return;
		_assertArrayExists(arrayRef);
		recordingAgent.onArrayRead(arrayRef, index);
	}
	
	public static void _onArrayWrite(Object arrayRef, int index) {
		if (cannotRecord()) return;
		_assertArrayExists(arrayRef);
		recordingAgent.onArrayWrite(arrayRef, index);
	}
	
	/**
	 * Instrumented after the lock has been acquired
	 * @param object
	 */
	public static void _onLockAcquire(Object object) {
		if (cannotRecord()) return;
		recordingAgent.onLockAcquire(object);
	}
	
	
	protected abstract void onRead(SharedMemoryLocation sml);
	
	protected abstract void onWrite(SharedMemoryLocation sml);
	
	protected abstract void acquireLock();
	protected abstract void releaseLock();
	
	/**
	 * After object creation
	 * @param object
	 */
	protected final void onObjectCreate(Object object) {
		objectIdGenerator.createId(object);
	}
	
	/**
	 * Before object read.
	 * @param object
	 * @param field
	 */
	protected final void onObjectRead(Object object, String field) {
		if (!sharedGenerator.isSharedObject(object, field)) {
			lastEvent.set(null);
			return;
		}
		SharedMemoryLocation sml = sharedGenerator.ofField(object, field);
		this.onRead(sml);
	}
	
	/**
	 * Called before object write
	 * @param object
	 * @param field
	 */
	protected final void onObjectWrite(Object object, String field) {
		if (!sharedGenerator.isSharedObject(object, field)) {
			lastEvent.set(null);
			return;
		}
		SharedMemoryLocation sml = sharedGenerator.ofField(object, field);
		this.onWrite(sml);
	}
	
	/**
	 * Called after object write
	 */
	protected abstract void afterWrite();
	
	/**
	 * Called after object read
	 */
	protected abstract void afterRead();
	
	protected abstract void onLockAcquire(Object object);
	
	protected final void onStaticRead(String className, String fieldName) {
		if (!sharedGenerator.isSharedStaticField(className, fieldName)) {
			lastEvent.set(null);
			return;
		}
		SharedMemoryLocation sml = sharedGenerator.ofStaticField(className, fieldName);
		this.onRead(sml);
	}
	
	protected final void onStaticWrite(String className, String fieldName) {
		if (!sharedGenerator.isSharedStaticField(className, fieldName)) {
			lastEvent.set(null);
			return;
		}
		SharedMemoryLocation sml = sharedGenerator.ofStaticField(className, fieldName);
		this.onWrite(sml);
	}
	
	protected final void onArrayRead(Object arrayRef, int arrayIndex) {
		if (!sharedGenerator.isSharedArray(arrayRef, arrayIndex)) {
			lastEvent.set(null);
			return;
		}
		SharedMemoryLocation sml = sharedGenerator.ofArray(arrayRef, arrayIndex);
		this.onRead(sml);
	}
	
	protected final void onArrayWrite(Object arrayRef, int arrayIndex) {
		if (!sharedGenerator.isSharedArray(arrayRef, arrayIndex)) {
			lastEvent.set(null);
			return;
		}
		SharedMemoryLocation sml = sharedGenerator.ofArray(arrayRef, arrayIndex);
		this.onWrite(sml);
	}
	
	@Override
	public void startup0(long vmStartupTime, long agentStartupTime) {
		timeoutThread = new TimeoutThread(this);
		timeoutThread.setTimeout(agentParams.getTimeOut());
		timeoutThread.start();
		AppJavaClassPath appPath = agentParams.initAppClassPath();
		GlobalFilterChecker.setup(appPath, agentParams.getIncludesExpression(), agentParams.getExcludesExpression());
		recordingAgent = this;
		SystemClassTransformer.attachThreadId(getInstrumentation());
		String dumpFileStr = agentParams.getDumpFile();
		if (dumpFileStr == null) dumpFileStr = "temp.txt";
		File dumpFile = new File(dumpFileStr);
		StorableReader fileReader = null;
		try {
			fileReader = new StorableReader(dumpFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to find dump file");
		}
		
		try {
			List<ParseData> data = fileReader.read();
			SharedVariableOutput svOutput = new SharedVariableOutput(data.get(0));
			sharedGenerator.init(svOutput);
			parseSharedOutput(svOutput);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void shutdown() {
		File file = new File(this.agentParams.getConcDumpFile());
		try {
			StorableWriter writer = new StorableWriter(file);
			RecordingOutput output = getRecordingOutput();
			writer.writeStorable(output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected abstract RecordingOutput getRecordingOutput();

	protected abstract void parseSharedOutput(SharedVariableOutput svo);

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

}
