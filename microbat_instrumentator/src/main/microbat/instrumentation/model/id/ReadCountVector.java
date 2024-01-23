package microbat.instrumentation.model.id;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ReadCountVector {
	
	private ConcurrentHashMap<Long, Map<MemoryLocation, Map<Long, Integer>>> 
		rcVectorClockConcurrentHashMap;
	public ReadCountVector() {
		
	}
	
	public Map<Long, Integer> get(MemoryLocation memoryLocation, long threadId) {
		assertThreadId(threadId);
		Map<Long, Integer> otherMap = rcVectorClockConcurrentHashMap.get(threadId).get(memoryLocation);
		return new HashMap<>(otherMap);
	}
	
	private void assertThreadId(long threadId) {
		if (!rcVectorClockConcurrentHashMap.contains(threadId)) {
			synchronized (rcVectorClockConcurrentHashMap) {
				// additional check in case another thread reaches this line
				if (!rcVectorClockConcurrentHashMap.contains(threadId)) {
					rcVectorClockConcurrentHashMap.put(threadId, 
							new ConcurrentHashMap<MemoryLocation, Map<Long, Integer>>());
				}
			}
		}
	}
	
	public void updateReadVectors(MemoryLocation variable, long eventThreadID) {
		assertThreadId(eventThreadID);
		Map<Long, Integer> eventThreadMap = rcVectorClockConcurrentHashMap.get(eventThreadID)
				.getOrDefault(variable, new HashMap<Long, Integer>());
		for (Map.Entry<Long, Map<MemoryLocation, Map<Long, Integer>>> entry 
				: rcVectorClockConcurrentHashMap.entrySet()) {
			int value = entry.getValue().getOrDefault(variable, new HashMap<Long, Integer>())
					.getOrDefault(entry.getKey(), 0);
			if (value == 0) {
				continue;
			}
			long tPrime = entry.getKey();
			eventThreadMap.put(tPrime, value);
		}
		rcVectorClockConcurrentHashMap.get(eventThreadID).put(variable, eventThreadMap);
	}
	
	public void increment(long t1, MemoryLocation variable) {
		assertThreadId(t1);
		Map<MemoryLocation, Map<Long, Integer>> threadRCVector =
				rcVectorClockConcurrentHashMap.get(t1);
		Map<Long, Integer> innerConcurrentHashMap = threadRCVector.get(variable);
		if (innerConcurrentHashMap == null) {
			innerConcurrentHashMap = new HashMap<>();
			threadRCVector.put(variable, innerConcurrentHashMap);
		}
		int count = innerConcurrentHashMap.getOrDefault(t1, 0);
		innerConcurrentHashMap.put(t1, count + 1);
	}
	
	public void update(Event event) {
		long threadId = Thread.currentThread().getId();
		assertThreadId(threadId);
		
	}
	
}
