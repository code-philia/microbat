package microbat.instrumentation.model.id;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.instr.aggreplay.shared.parser.MemoryLocationParser;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.model.storage.Storable;

public class Event extends Storable implements Parser<Event> {
	private static ThreadLocal<Integer> eventCounterLocal = ThreadLocal.withInitial(new Supplier<Integer>() {
		@Override
		public Integer get() {
			return 0;
		}
	});
	private long threadId;
	private int eventId;
	
	
	/**
	 * Needed to update thread Id which is obtained from
	 * serialized data.
	 * @param threadId
	 */
	public void updateThreadId(long threadId) {
		this.threadId = threadId;
	}
	
	// on write, the location written to
	// on read, the location read from.
	private SharedMemoryLocation relevantLocation;
	public Event(SharedMemoryLocation location) {
		threadId = Thread.currentThread().getId();
		eventId = eventCounterLocal.get();
		eventCounterLocal.set(eventId + 1);
		this.relevantLocation = location;
	}

	public Event(SharedMemoryLocation location, final long threadId) {
		this.threadId = threadId;
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

	private Event() {
		
	}
	
	public static Event parseEvent(ParseData data) {
		return new Event().parse(data);
	}
	
	@Override
	protected Map<String, String> store() {
		Map<String, String> result = new HashMap<>();
		ThreadId threadId = ThreadIdGenerator.threadGenerator.getId(this.threadId);
		result.put("threadId", fromObject(this.threadId));
		result.put("eventId", eventId + "");
		result.put("relevantLocation", fromObject(this.relevantLocation));
		return result;
		
	}

	@Override
	public Event parse(ParseData data) {
		this.threadId = data.getField("threadId").getLongValue();
		this.eventId = data.getField("eventId").getIntValue();
		
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(eventId, relevantLocation, threadId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Event other = (Event) obj;
		return eventId == other.eventId && Objects.equals(relevantLocation, other.relevantLocation)
				&& threadId == other.threadId;
	}
	
	
	
}
