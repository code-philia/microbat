package microbat.instrumentation.instr.aggreplay.agents;

import java.lang.instrument.ClassFileTransformer;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Semaphore;

import microbat.instrumentation.CommandLine;
import microbat.instrumentation.model.RecorderObjectId;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.SharedMemoryLocation;

/**
 * Different mode of replay, used to test stricter replay.
 * Use blocking yield instead.
 * For every shm, we have a list of writes, and the reads associated to each write.
 * So each write, we can simply wait for the read to occur
 * before we allow another write.
 * 
 * @author Gabau
 *
 */
public class StrictRWReplay extends AggrePlayReplayAgent {

	private Semaphore smp = new Semaphore(1);

	public StrictRWReplay(CommandLine cmd) {
		super(cmd);
	}
	

	@Override
	protected void onLockAcquire(Object obj) {

		ObjectId oid = this.sharedMemGenerator.ofObjectOrArray(obj);
		
		Stack<Event> eventStack = this.lockAcquisitionMap.get(oid);
		Event currEvent = new Event(null, getPreviousThreadId());
		while (eventStack == null || !currEvent.equals(eventStack.peek())) {
			Thread.yield();
		}
		lastObjStackLocal.set(eventStack);
	}


	@Override
	protected void onRead(long previousThreadId, SharedMemoryLocation sharedMemLocation) {
		// TODO Auto-generated method stub
		Event readEvent = new Event(sharedMemLocation, previousThreadId);
		lastEventLocal.set(readEvent);
		while (!sharedMemLocation.canRead(readEvent)) {
			Thread.yield();
		}
	}

	@Override
	protected void onWrite(long p_tid, SharedMemoryLocation shm) {
		Event writeEvent = new Event(shm, p_tid);
		while (!shm.canWrite(writeEvent)) {
			Thread.yield();
		}
		lastEventLocal.set(writeEvent);
	}
	
	private void acquireLock() {
		try {
			this.smp.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void releaseLock() {
		this.smp.release();
	}
	

	@Override
	protected void afterObjectWrite() {
		if (!wasShared()) {
			return;
		}
		SharedMemoryLocation lastLocation = lastEventLocal.get().getLocation();
		lastLocation.setLastWrite(lastEventLocal.get());
		lastLocation.popEvent();
	}

	@Override
	protected void afterObjectRead() {
		if (!wasShared()) {
			return;
		}
		SharedMemoryLocation lastLocation = lastEventLocal.get().getLocation();
		lastLocation.read(lastEventLocal.get());
	}

	@Override
	public void startup0(long vmStartupTime, long agentPreStartup) {
		super.startup0(vmStartupTime, agentPreStartup);
	}
	
}
