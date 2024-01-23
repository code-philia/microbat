package microbat.instrumentation.instr.aggreplay.agents;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Supplier;

import microbat.instrumentation.Agent;
import microbat.instrumentation.instr.SystemClassTransformer;
import microbat.instrumentation.instr.aggreplay.TimeoutThread;
import microbat.instrumentation.model.generator.IdGenerator;
import microbat.instrumentation.model.generator.ReferenceObjectIdGenerator;
import microbat.instrumentation.model.generator.SharedMemoryGenerator;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.MemoryLocation;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.ReadCountVector;
import microbat.instrumentation.model.id.ReadWriteAccessList;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.model.id.ThreadId;

public class AggrePlayRecordingAgent extends Agent {

	public static AggrePlayRecordingAgent attachedAgent;
	private HashSet<MemoryLocation> sharedMemoryLocations = new HashSet<>();
	private ReferenceObjectIdGenerator objectIdGenerator = new ReferenceObjectIdGenerator();
	private SharedMemoryGenerator sharedGenerator = new SharedMemoryGenerator(objectIdGenerator);
	private TimeoutThread timeoutThread = new TimeoutThread();
	private ReadCountVector rcVector = new ReadCountVector();
	private ReadWriteAccessList rwal = new ReadWriteAccessList();
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
	
	
	public static void attachAgent(AggrePlayRecordingAgent agent) {
		attachedAgent = agent;
	}
	
	/**
	 * Checks if a given object field is shared or not
	 * @param object
	 * @return
	 */
	private boolean isShared(Object object, String fieldName) {
		SharedMemoryLocation location = this.sharedGenerator.ofField(object, fieldName);
		return sharedMemoryLocations.contains(location.getLocation());
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
		Event writeEvent = new Event(smLocation);
		attachedAgent.updateReadVectors(writeEvent);
		Event lastWrite = smLocation.getLastWrite();
		attachedAgent.lw.set(lastWrite);
	}
	

	/**
	 * Called before GETFIELD
	 * @param object
	 * @param field
	 */
	public static void _onObjectRead(Object object, String field) {
		if (!attachedAgent.isShared(object, field)) {
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
	}
	
	
	/**
	 * Function called after object read.
	 */
	public static void _afterObjectRead() {
		Event lw = attachedAgent.lw.get();
		Event lr = attachedAgent.lw.get();
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
		sharedGenerator.setObjectIdGenerator(objectIdGenerator);
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
