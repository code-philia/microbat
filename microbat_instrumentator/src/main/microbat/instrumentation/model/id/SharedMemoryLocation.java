package microbat.instrumentation.model.id;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
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
	public Event lastWrite;
	/**
	 * The location this object is at.
	 */
	public MemoryLocation location;
	

	/**
	 * The event write for this object.
	 * Uses a map to prevent need for synchronisation
	 */
	public final Map<Long, LinkedList<Pair<Event, Event>>> threadExListMap = new HashMap<>();
	
	private final LinkedList<Pair<Event, Event>> wrList = new LinkedList<>();
	
	// write event list used during recording
	private List<Event> writeEventList = new LinkedList<>();
	// TODO: get this stack from the input + put in a different class
	// the bottom two are data used for replay
	// Need to separate these two cause can get quite confusing -> ideally in a separate class
	// W_var(e)
	private Stack<Event> writeEventStack;
	private List<Event> repWriteEvent;
	private final Stack<Pair<Event, Event>> repWrStack = new Stack<>();
	
	public boolean checkLastWrite(Event e) {
		return getRecordedLastWrite().equals(lastWrite);
	}
	
	// TODO (Gab): make the recorded lastwrite not a map
	private Event getRecordedLastWrite() {
		return repWrStack.peek().first();
	}
	
	private void popRecordedLastWrite() {
		repWrStack.pop();
	}
	
	

	/**
	 * Checks if the given event is the same as the event in the previous run
	 * on the top of the stack
	 * @param e
	 * @return
	 */
	public boolean isSameAsPrevRunWrite(Event e) {
		return writeEventStack.peek().equals(e);
	}
	
	public void addWriteEvent(Event e) {
		repWriteEvent.add(e);
	}
	
	public void popEvent() {
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
	}
	
	
	/**
	 * The read count vector 
	 * @param event
	 */
	
	public synchronized void setLastWrite(Event event) {
		setLastWrite(event);
		lastWrite = event;
	}
	
	/**
	 * Checks if the current shared mem location write
	 * is the same as the previous
	 * @return
	 */
	public synchronized boolean isSameAsLastWrite() {
		return this.lastWrite.equals(writeEventStack.peek());
	}
	
	public synchronized boolean isSameEvent(Event event) {
		return this.writeEventStack.peek().equals(event);
	}
	
	public synchronized Event getLastWrite() {
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
		threadExListMap.get(threadId).add(Pair.of(lw, readEvent));
	}

	@Override
	public int hashCode() {
		return Objects.hash(location, threadExListMap, writeEventList);
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
		this.lastWrite = null;
		this.location = new MemoryLocationParser().parse(data.getField("location"));
		this.writeEventList = data.toList(new Function<ParseData, Event>() {
			@Override
			public Event apply(ParseData parseData) {
				return Event.parseEvent(parseData);
			}
		});
		// TODO(Gab): parse the ex list
		return this;
	}
	
	
}
