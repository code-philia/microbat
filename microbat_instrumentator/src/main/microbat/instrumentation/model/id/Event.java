package microbat.instrumentation.model.id;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import microbat.instrumentation.instr.aggreplay.ThreadIdInstrumenter;
import microbat.instrumentation.model.storage.Storable;

public class Event extends Storable {
	private static ThreadLocal<Integer> eventCounterLocal = ThreadLocal.withInitial(new Supplier<Integer>() {
		@Override
		public Integer get() {
			return 0;
		}
	});
	private long threadId;
	private int eventId;
	// on write, the location written to
	// on read, the location read from.
	private SharedMemoryLocation relevantLocation;
	public Event(SharedMemoryLocation location) {
		threadId = Thread.currentThread().getId();
		eventId = eventCounterLocal.get();
		eventCounterLocal.set(eventId + 1);
		this.relevantLocation = location;
	}
	
	public SharedMemoryLocation getLocation() {
		return relevantLocation;
	}
	
	public long getThreadId() {
		return threadId;
	}

	@Override
	protected Map<String, String> store() {
		Map<String, String> result = new HashMap<>();
		ThreadId threadId = ThreadIdInstrumenter.threadGenerator.getId(this.threadId);
		result.put("threadId", fromObject(threadId));
		result.put("eventId", eventId + "");
		return result;
		
	}
	
	
	
}
