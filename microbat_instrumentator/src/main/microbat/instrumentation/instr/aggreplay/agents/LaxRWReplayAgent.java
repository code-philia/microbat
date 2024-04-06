package microbat.instrumentation.instr.aggreplay.agents;

import java.util.Stack;
import java.util.concurrent.Semaphore;

import microbat.instrumentation.CommandLine;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.SharedMemoryLocation;

public class LaxRWReplayAgent extends AggrePlayReplayAgent {

	/**
	 * RW replay with non blocking yield
	 * @param cmd
	 */
	public LaxRWReplayAgent(CommandLine cmd) {
		super(cmd);
	}

	private Semaphore smp = new Semaphore(1);


	@Override
	protected void onLockAcquire(Object obj) {

		ObjectId oid = sharedMemGenerator.ofObjectOrArray(obj);
		Stack<Event> eventStack = this.lockAcquisitionMap.get(oid);
		Event currEvent = new Event(null, getPreviousThreadId());
		if (eventStack == null || !currEvent.equals(eventStack.peek())) {
			Thread.yield();
		}
		lastObjStackLocal.set(eventStack);
	}
	
	@Override
	protected void onRead(long previousThreadId, SharedMemoryLocation sharedMemLocation) {
		// TODO Auto-generated method stub
		Event readEvent = new Event(sharedMemLocation, previousThreadId);
		lastEventLocal.set(readEvent);
		if (!sharedMemLocation.canRead(readEvent)) {
			Thread.yield();
		}
	}

	@Override
	protected void onWrite(long p_tid, SharedMemoryLocation shm) {
		Event writeEvent = new Event(shm, p_tid);
		if (!shm.canWrite(writeEvent)) {
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
