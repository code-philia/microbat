package microbat.instrumentation.model.id;

import java.util.function.Supplier;

public class Event {
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
	
}
