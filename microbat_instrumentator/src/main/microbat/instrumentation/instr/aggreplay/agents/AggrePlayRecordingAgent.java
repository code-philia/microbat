package microbat.instrumentation.instr.aggreplay.agents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentFactory;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.filter.GlobalFilterChecker;
import microbat.instrumentation.instr.SystemClassTransformer;
import microbat.instrumentation.instr.aggreplay.TimeoutThread;
import microbat.instrumentation.instr.aggreplay.output.SharedVariableOutput;
import microbat.instrumentation.instr.aggreplay.record.RecordingInstrumentor;
import microbat.instrumentation.instr.aggreplay.shared.BasicTransformer;
import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.RecordingOutput;
import microbat.instrumentation.instr.aggreplay.shared.SharedDataParser;
import microbat.instrumentation.model.RecorderObjectId;
import microbat.instrumentation.model.generator.IdGenerator;
import microbat.instrumentation.model.generator.ObjectIdGenerator;
import microbat.instrumentation.model.generator.SharedMemoryGenerator;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.MemoryLocation;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.ReadCountVector;
import microbat.instrumentation.model.id.ReadWriteAccessList;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.storage.FileStorage;
import microbat.instrumentation.model.storage.Storable;
import microbat.instrumentation.runtime.ExecutionTracer;
import microbat.instrumentation.runtime.IExecutionTracer;
import microbat.model.trace.Trace;
import microbat.sql.Recorder;
import sav.strategies.dto.AppJavaClassPath;

public class AggrePlayRecordingAgent extends Agent {

	private static AggrePlayRecordingAgent attachedAgent = new AggrePlayRecordingAgent();
	private HashSet<MemoryLocation> sharedMemoryLocations = new HashSet<>();
	private ObjectIdGenerator objectIdGenerator = new ObjectIdGenerator();
	private SharedMemoryGenerator sharedGenerator = new SharedMemoryGenerator(objectIdGenerator);
	private ReadCountVector rcVector = new ReadCountVector();
	private ReadWriteAccessList rwal = new ReadWriteAccessList();
	private ClassFileTransformer transformer = new BasicTransformer(new RecordingInstrumentor());
	/**
	 * This is passed into the next agent
	 */
	private SharedVariableOutput sharedVariableOutput;

	public static final Semaphore LOCK_OBJECT = new Semaphore(1);
	private AgentParams agentParams;
	
	public void setAgentParams(AgentParams params) {
		this.agentParams = params;
	}
	
	public static AggrePlayRecordingAgent getAttached(CommandLine cmd) {
		attachedAgent.agentParams = new AgentParams(cmd);
		return attachedAgent;
	}
	
	// last write, these two variables are used for computation,
	// not for the actual last write, last read
	private ThreadLocal<Event> lw = ThreadLocal.withInitial(new Supplier<Event>() {
		@Override
		public Event get() {
			return null;
		}
	});
	// last read 
	private ThreadLocal<Event> lr = ThreadLocal.withInitial(new Supplier<Event>() {
		@Override
		public Event get() {
			return null;
		}
	});
	private boolean wasShared = false;
	

	public static void _acquireLock() {
		try {
			LOCK_OBJECT.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void _releaseLock() {
		LOCK_OBJECT.release();
	}
	
	// TODO: to call after monitorenter
	// use locking objects -> we keep track of the lock per object
	public static void _onLockAcquire(Object lockObject) {
		RecorderObjectId recorderObjectId = attachedAgent.sharedGenerator.ofObject(lockObject);
		recorderObjectId.acquireLock(new Event(null));
	}
	
	
	public static void attachAgent(AggrePlayRecordingAgent agent) {
		attachedAgent = agent;
	}
	
	/**
	 * Checks if a given object field is shared or not
	 * @param object
	 * @return
	 */
	private boolean isShared(Object object, String fieldName) {
		if (object == null) return false;
		return sharedGenerator.isSharedObject(object, fieldName);
	}
	
	private boolean isSharedStatic(String className, String fieldName) {
		SharedMemoryLocation location = this.sharedGenerator.ofStaticField(className, fieldName);
		return sharedMemoryLocations.contains(location);
	}
	
	/**
	 * Called after NEW instruction.
	 * @param object
	 */
	public static void _onNewObject(Object object) {
		attachedAgent.objectIdGenerator.createId(object);
	}
	
	/**
	 * Called before PUTFIELD
	 * @param object
	 * @param field
	 */
	public static void _onObjectWrite(Object object, String field) {
		if (!attachedAgent.isShared(object, field)) {
			attachedAgent.wasShared = false;
			return;
		}
		SharedMemoryLocation smLocation = attachedAgent.sharedGenerator.ofField(object, field);
		onWrite(smLocation);
	}

	private static void onWrite(SharedMemoryLocation smLocation) {
		Event writeEvent = new Event(smLocation);
		attachedAgent.updateReadVectors(writeEvent);
		smLocation.write(writeEvent);
	}
	

	/**
	 * Called before GETFIELD
	 * @param object
	 * @param field
	 */
	public static void _onObjectRead(Object object, String field) {
		if (!attachedAgent.isShared(object, field)) {
			attachedAgent.wasShared = false;
			return;
		}
		SharedMemoryLocation smLocation = attachedAgent.sharedGenerator.ofField(object, field);
		attachedAgent.onRead(smLocation);
	}
	
	private void onRead(SharedMemoryLocation smLocation) {
		Event readEvent = new Event(smLocation);
		attachedAgent.lr.set(readEvent);
		rcVector.increment(Thread.currentThread().getId(), smLocation.getLocation());
		_acquireLock();
		Event lastWrite = smLocation.getLastWrite();
		attachedAgent.lw.set(lastWrite);
		attachedAgent.wasShared = true;
	}
	
	
	/**
	 * Function called after object read.
	 */
	public static void _afterObjectRead() {
		if (!attachedAgent.wasShared) return;
		_releaseLock();
		Event lw = attachedAgent.lw.get();
		Event lr = attachedAgent.lr.get();
		lr.getLocation().appendExList(lw, lr);
	}
	
	private void updateReadVectors(Event event) {
		rcVector.updateReadVectors(
				event.getLocation().getLocation(), Thread.currentThread().getId());
		rwal.add(event.getLocation().getLocation(), event, rcVector);
	}
	
	
	
	@Override
	public void startup0(long vmStartupTime, long agentPreStartup) {
		AppJavaClassPath appPath = agentParams.initAppClassPath();
		GlobalFilterChecker.setup(appPath, agentParams.getIncludesExpression(), agentParams.getExcludesExpression());
		
		AggrePlayRecordingAgent.attachAgent(this);
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
			sharedVariableOutput = new SharedVariableOutput(data.get(0));
			sharedGenerator.init(sharedVariableOutput);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// TODO: grab the shared object ids
	private List<ObjectId> getObjectIds() {
		Collection<ObjectId> objectIds = objectIdGenerator.getObjects();
		return objectIds.stream().filter(new Predicate<ObjectId>() {
			@Override
			public boolean test(ObjectId o) {
				return sharedGenerator.isSharedObject(o);
			}
		}).collect(Collectors.<ObjectId>toList());
	}
	
	private List<SharedMemoryLocation> getSharedMemoryLocations() {
		return sharedGenerator.getAllLocations();
	}

	@Override
	public void shutdown() throws Exception {
		FileStorage fileStorage = new FileStorage(this.agentParams.getConcDumpFile());
		HashSet<Storable> toStore = new HashSet<>();
		List<ThreadId> threadIds = ThreadIdGenerator.threadGenerator.getThreadIds();
		List<ObjectId> objectIds = getObjectIds();
		List<SharedMemoryLocation> sharedMemoryLocations = getSharedMemoryLocations();
		RecordingOutput output = new RecordingOutput(rwal, threadIds, 
				sharedMemoryLocations,
				this.sharedGenerator.getLockAcquisitionMap(),
				this.sharedVariableOutput);
		List<Storable> values = new LinkedList<>();
		values.add(output);
		fileStorage.store(values);
	}


	@Override
	public void startTest(String junitClass, String junitMethod) {
		
	}

	@Override
	public void finishTest(String junitClass, String junitMethod) {
		
	}

	@Override
	public ClassFileTransformer getTransformer0() {
		return transformer;
	}

	@Override
	public void retransformBootstrapClasses(Instrumentation instrumentation, Class<?>[] retransformableClasses)
			throws Exception {
		
	}

	@Override
	public void exitTest(String testResultMsg, String junitClass, String junitMethod, long threadId) {
		
	}

	@Override
	public boolean isInstrumentationActive0() {
		return false;
	}
	

}
