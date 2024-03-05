package microbat.instrumentation.instr.aggreplay.agents;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import microbat.instrumentation.AgentParams;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.instr.aggreplay.output.SharedVariableOutput;
import microbat.instrumentation.instr.aggreplay.record.RecordingInstrumentor;
import microbat.instrumentation.instr.aggreplay.shared.BasicTransformer;
import microbat.instrumentation.instr.aggreplay.shared.RecordingOutput;
import microbat.instrumentation.model.RecorderObjectId;
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

/**
 * Implementration of AggrePlay for non-blocking wait.
 * @author Gabau
 *
 */
public class LaxRecordingAgent extends RNRRecordingAgent {
	private ReadCountVector rcVector = new ReadCountVector();
	private ReadWriteAccessList rwal = new ReadWriteAccessList();
	private ClassFileTransformer transformer = new BasicTransformer(new RecordingInstrumentor(RNRRecordingAgent.class));
	private Map<ObjectId, List<Event>> lockAcquisitionListMap = new HashMap<>();


	public static final Semaphore LOCK_OBJECT = new Semaphore(1);

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
	
	protected LaxRecordingAgent(CommandLine cml) {
		super(cml);
	}
	
	@Override
	protected void onRead(SharedMemoryLocation smLocation) {
		Event readEvent = new Event(smLocation);
		lr.set(readEvent);
		lastEvent.set(readEvent);
		rcVector.increment(Thread.currentThread().getId(), smLocation.getLocation());
		_acquireLock();
		Event lastWrite = smLocation.getLastWrite();
		lw.set(lastWrite);
		wasShared = true;
	}
	

	private void updateReadVectors(Event event) {
		rcVector.updateReadVectors(
				event.getLocation().getLocation(), Thread.currentThread().getId());
		rwal.add(event.getLocation().getLocation(), event, rcVector);
	}

	@Override
	protected void onWrite(SharedMemoryLocation sml) {
		wasShared = true;
		Event writeEvent = new Event(sml);
		this.updateReadVectors(writeEvent);
		sml.write(writeEvent);
	}

	@Override
	protected void acquireLock() {
		try {
			LOCK_OBJECT.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void releaseLock() {

		LOCK_OBJECT.release();
	}

	@Override
	protected void afterWrite() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void afterRead() {
		if (!this.wasShared) return;
		_releaseLock();
		Event lw = this.lw.get();
		Event lr = this.lr.get();
		lr.getLocation().appendExList(lw, lr);
	}

	@Override
	protected void onLockAcquire(Object lockObject) {
		ObjectId oId = sharedGenerator.ofObjectOrArray(lockObject);
		if (!lockAcquisitionListMap.containsKey(oId)) {
			lockAcquisitionListMap.put(oId, new LinkedList<Event>());
		}
		lockAcquisitionListMap.get(oId).add(new Event(null));
	}
	
	@Override
	protected RecordingOutput getRecordingOutput() {
		List<ThreadId> threadIds = ThreadIdGenerator.threadGenerator.getThreadIds();
		RecordingOutput output = new RecordingOutput(rwal, threadIds, 
				sharedGenerator.getAllLocations(),
				lockAcquisitionListMap);
		return output;
	}

	@Override
	protected void parseSharedOutput(SharedVariableOutput svo) {
		
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
