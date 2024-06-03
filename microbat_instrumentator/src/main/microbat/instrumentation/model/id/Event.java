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
	// on write, the location written to
	// on read, the location read from.
	private SharedMemoryLocation relevantLocation;
	
	/**
	 * Needed to update thread Id which is obtained from
	 * serialized data.
	 * @param threadId
	 */
	public void updateThreadId(long threadId) {
		this.threadId = threadId;
	}
	
	public static Event getFirstEvent(SharedMemoryLocation sml) {
		Event result = new Event();
		result.eventId = -1;
		result.threadId = -1;
		result.relevantLocation = sml;
		return result;
	}
	
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
		result.put("threadId", fromObject(this.threadId));
		result.put("eventId", eventId + "");
		// removed -> won't be stored as can lead to recursive problems
		// solution -> the role of populating the relevant location falls
		// to the parent shared memory location object.
		// result.put("relevantLocation", fromObject(this.relevantLocation));
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
		return Objects.hash(eventId, threadId);
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
		return eventId == other.eventId
				&& threadId == other.threadId;
	}
	
	
	
}
