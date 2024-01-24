package microbat.instrumentation.model.id;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import microbat.instrumentation.model.storage.Storable;
import sav.common.core.Pair;

/**
 * Represents shared memory -> can be a field in an object,
 * an index in an array or a static field
 * 
 * @author Gabau
 *
 */
public class SharedMemoryLocation extends Storable {
	public Event lastWrite;
	/**
	 * The location this object is at.
	 */
	public MemoryLocation location;
	private List<Event> writeEventList = new LinkedList<>();
	
	
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
	 * The event write for this object.
	 * Uses a map to prevent need for synchronisation
	 */
	private final Map<Long, LinkedList<Pair<Event, Event>>> threadExListMap = new HashMap<>();
	
	/**
	 * The read count vector 
	 * @param event
	 */
	
	public synchronized void setLastWrite(Event event) {
		setLastWrite(event);
		lastWrite = event;
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
}
