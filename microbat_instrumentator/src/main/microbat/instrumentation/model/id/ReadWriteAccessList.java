package microbat.instrumentation.model.id;

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
	
	private ConcurrentHashMap<Long, LinkedList<Node>> exList = new ConcurrentHashMap<>();
	
	protected void assertExListThread(long threadId) {
		if (!exList.contains(threadId)) {
			synchronized (exList) {
				if (!exList.contains(threadId)) {
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
