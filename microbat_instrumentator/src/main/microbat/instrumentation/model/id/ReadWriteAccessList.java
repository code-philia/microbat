package microbat.instrumentation.model.id;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ReadWriteAccessList {
	private static class Node {
		public Map<Long, Integer> rcMap;
		public MemoryLocation memoryLocation;
		public Event event;
		public Node(Map<Long, Integer> rcMap, MemoryLocation memoryLocation, Event event) {
			this.rcMap = rcMap;
			this.memoryLocation = memoryLocation;
			this.event = event;
		}
	}
	
	private HashMap<Long, LinkedList<Node>> exList = new HashMap<>();
	
	protected void assertExListThread(long threadId) {
		if (!exList.containsKey(threadId)) {
			synchronized (exList) {
				if (!exList.containsKey(threadId)) {
					exList.put(threadId, new LinkedList<Node>());
				}
			}
		}
	}
	
	public void add(MemoryLocation memoryLocation, Event event, ReadCountVector readVector) {
		long tid = event.getThreadId();
		assertExListThread(tid);
		Map<Long, Integer> rcMap = readVector.get(memoryLocation, tid);
		exList.get(tid).add(new Node(rcMap, memoryLocation, event));
	}
}
