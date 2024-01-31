package microbat.instrumentation.model.id;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.instr.aggreplay.shared.parser.MemoryLocationParser;
import microbat.instrumentation.instr.aggreplay.shared.parser.RCVectorParser;
import microbat.instrumentation.model.storage.Storable;
import sav.common.core.Pair;


public class ReadWriteAccessList extends Storable implements Parser<ReadWriteAccessList> {
	private HashMap<Long, LinkedList<Node>> exList = new HashMap<>();
	
	public Node top(long threadId) {
		return exList.get(threadId).getFirst();
	}
	
	public Map<Long, LinkedList<Node>> getList() {
		return exList;
	}
	
	public static class Node extends Storable implements Parser<Node> {
		private Map<Long, Integer> rcMap;
		private MemoryLocation memoryLocation;
		private Event event;
		public Node(Map<Long, Integer> rcMap, MemoryLocation memoryLocation, Event event) {
			this.rcMap = rcMap;
			this.memoryLocation = memoryLocation;
			this.event = event;
		}
		
		public Node() {
			
		}
		
		public Map<Long, Integer> getRcMap() {
			return rcMap;
		}
		
		public MemoryLocation getLocation() {
			return memoryLocation;
		}
		
		@Override
		protected Map<String, String> store() {
			Map<String, String> fields = new HashMap<>();
			fields.put("rcMap", fromObject(rcMap));
			fields.put("memoryLocation", fromObject(memoryLocation));
			fields.put("event", fromObject(event));
			return fields;
		}
		@Override
		public Node parse(ParseData data) {
			this.event = Event.parseEvent(data.getField("event"));
			this.memoryLocation = new MemoryLocationParser().parse(data.getField("memoryLocation"));
			data.getField("event");
			this.rcMap = new RCVectorParser().parse(data.getField("rcMap"));
			// TODO Auto-generated method stub
			return this;
		}
	}
	
	
	
	@Override
	protected Map<String, String> store() {
		Map<String, String> result = new HashMap<>();
		result.put("exList", fromMap(exList));
		return result;
	}

	
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

	@Override
	public ReadWriteAccessList parse(ParseData data) {
		ParseData exListData = data.getField("exList");
		List<Pair<ParseData, ParseData>> mapData = exListData.toPairList();
		for (Pair<ParseData, ParseData> innerData : mapData) {
			Long value = innerData.first().getLongValue();
			List<ParseData> nodeDatas = innerData.second().toList();
			LinkedList<Node> nodes = new LinkedList<>();
			for (ParseData nodeData: nodeDatas) {
				Node node = new Node();
				node.parse(nodeData);
				nodes.add(node);
			}
		}
		return this;
	}


	@Override
	public int hashCode() {
		return Objects.hash(exList);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReadWriteAccessList other = (ReadWriteAccessList) obj;
		return Objects.equals(exList, other.exList);
	}
	
	
}
