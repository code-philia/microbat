package microbat.instrumentation.model.id;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.instr.aggreplay.shared.parser.MemoryLocationParser;
import microbat.instrumentation.instr.aggreplay.shared.parser.RCVectorParser;
import microbat.instrumentation.model.ReadWriteAccessListReplay;
import microbat.instrumentation.model.storage.Storable;
import sav.common.core.Pair;


public class ReadCountVector extends Storable implements Parser<ReadCountVector> {
	
	private ConcurrentHashMap<Long, Map<MemoryLocation, Map<Long, Integer>>> 
		rcVectorClockConcurrentHashMap = new ConcurrentHashMap<>();
	
	private static Map<Long, Integer> parseInnerMap(ParseData data) {
		return new RCVectorParser().parse(data);
	}
	
	public Map<MemoryLocation, Map<Long, Integer>> getRCVector(long threadID) {
		return rcVectorClockConcurrentHashMap.getOrDefault(threadID, new HashMap<MemoryLocation, Map<Long, Integer>>());
	}
 	
	private static Map<MemoryLocation, Map<Long, Integer>> parseMap(ParseData data) {
		List<Pair<ParseData, ParseData>> objectMap = data.toPairList();
		Map<Long, Integer> rcMap;
		Map<MemoryLocation, Map<Long, Integer>> rcVector = new HashMap<>();
		MemoryLocationParser parser = new MemoryLocationParser();
		for (Pair<ParseData, ParseData> innerData : objectMap) {
			MemoryLocation mlocation = parser.parse(innerData.first());
			rcMap = parseInnerMap(innerData.second());
			rcVector.put(mlocation, rcMap);
		}
		return rcVector;
	}
	@Override
	public ReadCountVector parse(ParseData data) {
		ParseData mapData = data.getField("rcVectorClockConcurrentHashMap");
		List<Pair<ParseData, ParseData>> objectMap = mapData.toPairList();
		Long keyValueLong = 0L;
		Map<MemoryLocation, Map<Long, Integer>> rcVector = null;
		for (Pair<ParseData, ParseData> innerData : objectMap) {
			keyValueLong = Long.parseLong(innerData.first().getValue());
			rcVector = parseMap(innerData.second());
			rcVectorClockConcurrentHashMap.put(keyValueLong, rcVector);
		}
		return this;
	}

	@Override
	protected Map<String, String> store() {
		Map<String, String> result = new HashMap<>();
		String resultString = fromMap(rcVectorClockConcurrentHashMap);
		result.put("rcVectorClockConcurrentHashMap", resultString);
		return result;
	}

	public ReadCountVector() {
	
	}

	public Map<Long, Integer> get(MemoryLocation memoryLocation, long threadId) {
		assertThreadId(threadId);
		Map<Long, Integer> otherMap = rcVectorClockConcurrentHashMap.get(threadId).get(memoryLocation);
		if (otherMap == null) return new HashMap<>();
		return new HashMap<>(otherMap);
	}
	
	private void assertThreadId(long threadId) {
		if (!rcVectorClockConcurrentHashMap.containsKey(threadId)) {
			synchronized (rcVectorClockConcurrentHashMap) {
				// additional check in case another thread reaches this line
				if (!rcVectorClockConcurrentHashMap.containsKey(threadId)) {
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

	@Override
	public int hashCode() {
		return Objects.hash(rcVectorClockConcurrentHashMap);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReadCountVector other = (ReadCountVector) obj;
		return Objects.equals(rcVectorClockConcurrentHashMap, other.rcVectorClockConcurrentHashMap);
	}
	
	
	
}
