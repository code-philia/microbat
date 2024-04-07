package microbat.instrumentation.model.id;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.instr.aggreplay.shared.parser.MemoryLocationParser;
import microbat.instrumentation.model.storage.Storable;
import sav.common.core.Pair;

/**
 * Represents shared memory -> can be a field in an object,
 * an index in an array or a static field
 * 
 * @author Gabau
 *
 */
public class SharedMemoryLocation extends Storable implements Parser<SharedMemoryLocation> {
	private Event lastWrite = Event.getFirstEvent(this);
	/**
	 * The location this object is at.
	 */
	private MemoryLocation location;
	
	

	/**
	 * The event write for this object.
	 * Uses a map to prevent need for synchronisation
	 */
	private final Map<Long, LinkedList<Pair<Event, Event>>> threadExListMap = new HashMap<>();
	private LinkedList<Pair<Event, Event>> wrList = new LinkedList<>();
	
	// write event list used during recording
	private List<Event> writeEventList = new LinkedList<>();
	// TODO: get this stack from the input + put in a different class
	// the bottom two are data used for replay
	// Need to separate these two cause can get quite confusing -> ideally in a separate class
	// W_var(e)
	// stack of write events, top is the earliest write
	private Stack<Event> writeEventStack;
	private List<Event> repWriteEvent = new LinkedList<>();
	// stack of lw -> read event
	private Stack<Pair<Event, Event>> repWrStack = new Stack<>();
	// mapping from write event to set of reads for that write
	private Map<Event, Set<Event>> repWrMapSetMap = new HashMap<>();
	
	
	protected Stack<Pair<Event, Event>> generateWriteEventStack(LinkedList<Pair<Event, Event>> writeEventList) {
		final Stack<Pair<Event, Event>> result = new Stack<>();
		writeEventList.descendingIterator().forEachRemaining(new Consumer<Pair<Event, Event>>() {
			@Override
			public void accept(Pair<Event, Event> value) {
				result.push(value);
			}
		});
		return result;
	}
	
	public boolean canWrite(Event e) {
		if (this.writeEventStack.empty()) return false;
		return this.writeEventStack.peek().equals(e) && 
				this.repWrMapSetMap.getOrDefault(lastWrite, Collections.emptySet()).isEmpty();
	}
	
	public boolean canRead(Event event) {
		return this.repWrMapSetMap.getOrDefault(lastWrite, Collections.emptySet()).contains(event);
	}
	
	public void read(Event event) {
		this.repWrMapSetMap.getOrDefault(lastWrite, Collections.emptySet()).remove(event);
	}
	
	@Override
	protected Map<String, String> store() {
		Map<String, String> result = new HashMap<String, String>();
		result.put("writeEventList", fromObject(writeEventList));
		result.put("location", fromObject(location));
		result.put("threadExListMap", fromObject(threadExListMap));
		result.put("wrList", fromObject(wrList));
		return result;
	}

	public boolean checkLastWrite(Event e) {
		return getRecordedLastWrite().equals(lastWrite);
	}
	
	// TODO (Gab): make the recorded lastwrite not a map
	private Event getRecordedLastWrite() {
		return repWrStack.peek().first();
	}
	
	public void popRecordedLastWR() {
		if (repWrStack.empty()) return;
		repWrStack.pop();
	}
	
	/**
	 * Checks if this shared memory location is a location
	 * in an object.
	 * @return
	 */
	public boolean isSharedObjectMem() {
		return this.location instanceof ObjectFieldMemoryLocation;
	}
	

	/**
	 * Checks if the given event is the same as the event in the previous run
	 * on the top of the stack
	 * @param e
	 * @return
	 */
	public boolean isSameAsPrevRunWrite(Event e) {
		if (writeEventStack.empty()) return false;
		return writeEventStack.peek().equals(e);
	}
	
	/**
	 * Separate function for replay.
	 * @param e
	 */
	public void addRepWriteEvent(Event e) {
		repWriteEvent.add(e);
		setLastWrite(e);
	}
	
	public void popEvent() {
		if (writeEventStack.empty()) return;
		writeEventStack.pop();
	}
	
	public SharedMemoryLocation() {
		this.location = null;
		this.writeEventList = null;
	}
	
	public SharedMemoryLocation(MemoryLocation location) {
		this.location = location;
	}
	
	public MemoryLocation getLocation() {
		return this.location;
	}
	
	public void write(Event event) {
		writeEventList.add(event);
		setLastWrite(event);
	}
	
	
	/**
	 * Checks if the current shared mem location write
	 * is the same as the previous
	 * @return
	 */
	public boolean isSameAsLastWrite(Event readEvent) {
		if (this.repWrStack.empty()) return false;
		return this.lastWrite.equals(repWrStack.peek().first())
				&& repWrStack.peek().second().equals(readEvent);
	}
	
	public boolean isSameEvent(Event event) {
		if (this.writeEventStack.peek() == null) return false;
		return this.writeEventStack.peek().equals(event);
	}
	
	public void setLastWrite(Event event) {
		this.lastWrite = event;
	}
	
	public Event getLastWrite() {
		return this.lastWrite;
	}
	
	private void assertThreadExists(long threadId) {
		if (!threadExListMap.containsKey(threadId)) {
			synchronized (threadExListMap) {
				if (!threadExListMap.containsKey(threadId)) {
					threadExListMap.put(threadId, new LinkedList<Pair<Event, Event>>());
				}
			}
		}
	}
	
	public void appendExList(Event lw, Event readEvent) {
		long threadId = Thread.currentThread().getId();
		assertThreadExists(threadId);
		// in theory only need to maintain  per thread tho 
		// - > then can do the popping per threaad
		// but need to decide which write occurs first
//		threadExListMap.get(threadId).add(Pair.of(lw, readEvent));
		synchronized (wrList) {
			wrList.add(Pair.of(lw, readEvent));
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(location, threadExListMap, writeEventList);
	}
	
	/**
	 * Generates the mapping from write event to read events
	 */
	public void generateWRMap() {
		
		for (Event writeEvent : writeEventList) {
			if (!this.repWrMapSetMap.containsKey(writeEvent)) {
				this.repWrMapSetMap.put(writeEvent, new HashSet<Event>());
			}
		}
		for (Pair<Event, Event> events: wrList) {
			if (!this.repWrMapSetMap.containsKey(events.first())) {
				this.repWrMapSetMap.put(events.first(), new HashSet<Event>());
			}
			this.repWrMapSetMap.get(events.first()).add(events.second());
		}
		
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SharedMemoryLocation other = (SharedMemoryLocation) obj;
		return Objects.equals(location, other.location) && Objects.equals(threadExListMap, other.threadExListMap)
				&& Objects.equals(writeEventList, other.writeEventList);
	}

	@Override
	public SharedMemoryLocation parse(ParseData data) {
		this.location = new MemoryLocationParser().parse(data.getField("location"));
		this.writeEventList = data.getField("writeEventList").toList(new Function<ParseData, Event>() {
			@Override
			public Event apply(ParseData parseData) {
				return Event.parseEvent(parseData);
			}
		});
		this.writeEventStack = new Stack<>();
		new LinkedList<>(writeEventList).descendingIterator().forEachRemaining(new Consumer<Event>() {
			@Override
			public void accept(Event event) {
				writeEventStack.add(event);
			}
		});
		
		
		List<Pair<Event, Event>> wrListInner = data.getField("wrList").<Pair<Event, Event>>toList(new Function<ParseData, 
				Pair<Event, Event>>() {
			@Override
			public Pair<Event, Event> apply(ParseData parseData) {
				return parseData.toPair(new Function<ParseData, Event>() {
					@Override
					public Event apply(ParseData data) {
						return Event.parseEvent(data);
					}
				},  
						new Function<ParseData, Event>() {
							@Override
							public Event apply(ParseData data) {
								return Event.parseEvent(data);
							}
						});
			}
		});
		
		this.wrList = new LinkedList<>(wrListInner);
		this.repWrStack = this.generateWriteEventStack(this.wrList);
		return this;
	}
	
	
}
