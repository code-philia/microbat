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
	
	public static RNRRecordingAgent getAttached(CommandLine cml) {
		if (recordingAgent != null) {
			recordingAgent.agentParams = AgentParams.initFrom(cml);
			return recordingAgent;
		}
		recordingAgent = new AggrePlayRecordingRWAgent(cml);
		return recordingAgent;
	}
	
	
	protected RNRRecordingAgent(CommandLine cml) {
		this.agentParams = AgentParams.initFrom(cml);
	}
	
	public static void _onNewObject(Object object) {
		recordingAgent.onObjectCreate(object);
	}
	
	public static void _onObjectRead(Object object, String field) {
		recordingAgent.onObjectRead(object, field);
	}
	
	public static void _onObjectWrite(Object object, String field) {
		recordingAgent.onObjectWrite(object, field);
	}
	
	
	public static void _afterObjectWrite() {
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
		recordingAgent._afterObjectReadInner();
	}
	
	public static void _onStaticRead(String className, String fieldName) {
		recordingAgent.onStaticRead(className, fieldName);
	}
	
	public static void _onStaticWrite(String className, String fieldName) {
		recordingAgent.onStaticWrite(className, fieldName);
	}
	
	public static void _onArrayRead(Object arrayRef, int index) {
		recordingAgent.onArrayRead(arrayRef, index);
	}
	
	public static void _onArrayWrite(Object arrayRef, int index) {
		recordingAgent.onArrayWrite(arrayRef, index);
	}
	
	public static void _onLockAcquire(Object object) {
		recordingAgent.onLockAcquire(object);
	}
	
	
	protected abstract void onRead(SharedMemoryLocation sml);
	
	protected abstract void onWrite(SharedMemoryLocation sml);
	
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
		AppJavaClassPath appPath = agentParams.initAppClassPath();
		GlobalFilterChecker.setup(appPath, agentParams.getIncludesExpression(), agentParams.getExcludesExpression());
		recordingAgent = this;
		SystemClassTransformer.attachThreadId(getInstrumentation());
//		timeoutThread.start();
		SharedDataParser parser = new SharedDataParser();
		String dumpFileStr = agentParams.getDumpFile();
		if (dumpFileStr == null) dumpFileStr = "temp.txt";
		File dumpFile = new File(dumpFileStr);
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(dumpFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to find dump file");
		}
		
		try {
			List<ParseData> data = parser.parse(fileReader);
			SharedVariableOutput svOutput = new SharedVariableOutput(data.get(0));
			sharedGenerator.init(svOutput);
			parseSharedOutput(svOutput);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void shutdown() {
		FileStorage fileStorage = new FileStorage(this.agentParams.getConcDumpFile());
		RecordingOutput output = getRecordingOutput();
		List<Storable> values = new LinkedList<>();
		values.add(output);
		fileStorage.store(values);
	}
	
	protected abstract RecordingOutput getRecordingOutput();

	protected abstract void parseSharedOutput(SharedVariableOutput svo);
}
