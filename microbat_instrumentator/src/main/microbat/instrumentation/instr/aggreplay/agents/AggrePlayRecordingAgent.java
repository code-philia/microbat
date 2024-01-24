package microbat.instrumentation.instr.aggreplay.agents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentFactory;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.instr.SystemClassTransformer;
import microbat.instrumentation.instr.aggreplay.TimeoutThread;
import microbat.instrumentation.instr.aggreplay.record.RecordingInstrumentor;
import microbat.instrumentation.instr.aggreplay.shared.BasicTransformer;
import microbat.instrumentation.instr.aggreplay.shared.SharedDataParser;
import microbat.instrumentation.model.generator.IdGenerator;
import microbat.instrumentation.model.generator.ObjectIdGenerator;
import microbat.instrumentation.model.generator.SharedMemoryGenerator;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.MemoryLocation;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.ReadCountVector;
import microbat.instrumentation.model.id.ReadWriteAccessList;
import microbat.instrumentation.model.id.RecorderObjectId;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.storage.FileStorage;
import microbat.instrumentation.model.storage.Storable;

public class AggrePlayRecordingAgent extends Agent {

	private static AggrePlayRecordingAgent attachedAgent = new AggrePlayRecordingAgent();
	private HashSet<MemoryLocation> sharedMemoryLocations = new HashSet<>();
	private ObjectIdGenerator objectIdGenerator = new ObjectIdGenerator();
	private SharedMemoryGenerator sharedGenerator = new SharedMemoryGenerator(objectIdGenerator);
	private TimeoutThread timeoutThread = new TimeoutThread();
	private ReadCountVector rcVector = new ReadCountVector();
	private ReadWriteAccessList rwal = new ReadWriteAccessList();
	private ClassFileTransformer transformer = new BasicTransformer(new RecordingInstrumentor());

	public static final Semaphore LOCK_OBJECT = new Semaphore(1);
	private AgentParams agentParams;
	
	public void setAgentParams(AgentParams params) {
		this.agentParams = agentParams;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void _releaseLock() {
		LOCK_OBJECT.release();
	}
	
	// TODO: to call after monitorenter
	// use locking objects -> we keep track of the lock per object
	public static void _onLockAcquire(Object lockObject) {
		ObjectId lockObjectOther = attachedAgent.objectIdGenerator.getId(lockObject);
		lockObjectOther.lockAcquire();
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
		Event lastWrite = smLocation.getLastWrite();
		attachedAgent.lw.set(lastWrite);
		attachedAgent.wasShared = true;
	}
	
	
	/**
	 * Function called after object read.
	 */
	public static void _afterObjectRead() {
		if (!attachedAgent.wasShared) return;
		Event lw = attachedAgent.lw.get();
		Event lr = attachedAgent.lr.get();
		lr.getLocation().appendExList(lw, lr);;
	}
	
	private void updateReadVectors(Event event) {
		rcVector.updateReadVectors(
				event.getLocation().getLocation(), Thread.currentThread().getId());
		rwal.add(event.getLocation().getLocation(), event, rcVector);
	}
	
	
	
	@Override
	public void startup0(long vmStartupTime, long agentPreStartup) {
		AggrePlayRecordingAgent.attachAgent(this);
		// TODO Auto-generated method stub
		SystemClassTransformer.attachThreadId(getInstrumentation());
		timeoutThread.start();
		SharedDataParser parser = new SharedDataParser();
		// TODO: get from cmd
		String dumpFileStr = agentParams.getDumpFile();
		if (dumpFileStr == null) dumpFileStr = "temp.txt";
		File dumpFile = new File(dumpFileStr);
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(dumpFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Failed to find dump file");
		}
		
		try {
			Map<ObjectId, RecorderObjectId> valueMap = parser.generateObjectIds(parser.parse(fileReader));
			sharedGenerator.setObjectIdRecorderMap(valueMap);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void shutdown() throws Exception {
		// TODO Auto-generated method stub
		FileStorage fileStorage = new FileStorage(this.agentParams.getConcDumpFile());
		HashSet<Storable> toStore = new HashSet<>();
		toStore.add(rcVector);
		toStore.add(rwal);
		fileStorage.store(toStore);
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
