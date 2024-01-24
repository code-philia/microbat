package microbat.instrumentation.model.id;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.model.storage.Storable;


public class ReadWriteAccessList extends Storable {
	private static class Node extends Storable {
		private Map<Long, Integer> rcMap;
		private MemoryLocation memoryLocation;
		private Event event;
		public Node(Map<Long, Integer> rcMap, MemoryLocation memoryLocation, Event event) {
			this.rcMap = rcMap;
			this.memoryLocation = memoryLocation;
			this.event = event;
		}
		@Override
		protected Map<String, String> store() {
			Map<String, String> fields = new HashMap<>();
			fields.put("rcMap", fromObject(rcMap));
			fields.put("memoryLocation", fromObject(memoryLocation));
			fields.put("event", fromObject(event));
			return fields;
		}
	}
	
	
	
	@Override
	protected Map<String, String> store() {
		Map<String, String> result = new HashMap<>();
		result.put("exList", fromMap(exList));
		return result;
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
		exList.get(tid).add(new Node(new HashMap<>(rcMap), memoryLocation, event));
	}
}
