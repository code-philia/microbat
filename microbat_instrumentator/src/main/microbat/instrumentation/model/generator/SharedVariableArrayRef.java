package microbat.instrumentation.model.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.storage.Storable;
import sav.common.core.Pair;

public class SharedVariableArrayRef extends Storable implements Parser<SharedVariableArrayRef> {
	public ObjectId objectId;
	public HashMap<Integer, HashSet<Long>> indexAccessMap = new HashMap<>();
	private HashSet<Integer> sharedIndexSet = new HashSet<>();
	protected Set<Integer> sharedIndexes() { 
		return indexAccessMap.entrySet().stream().filter(new Predicate<Entry<Integer, HashSet<Long>>>() {
			@Override
			public boolean test(Entry<Integer, HashSet<Long>> v) {
				return v.getValue().size() > 1;
			}
		}).map(v -> v.getKey()).collect(Collectors.<Integer>toSet());
	}
	
	public ObjectId getObjectId() {
		return this.objectId;
	}
	
	public Set<ArrayIndexMemLocation> getSharedMemLocations() {
		return sharedIndexes()
				.stream().map(v -> new ArrayIndexMemLocation(objectId, v)).collect(Collectors.toSet());
	}
	
	public HashSet<Integer> getSharedIndexes() {
		return sharedIndexSet;
	}
	
	@Override
	protected Map<String, String> store() {
		Map<String, String> result = new HashMap<>();
		result.put("objectId", fromObject(objectId));
		result.put("indexAccessSet", fromObject(sharedIndexes()));
		return result;
	}

	public SharedVariableArrayRef(ObjectId objectId) {
		this.objectId = objectId;
	}
	
	public boolean isShared() {
		for (Map.Entry<Integer, HashSet<Long>> entry : indexAccessMap.entrySet()) {
			if (entry.getValue().size() > 1) {
				return true;
			}
		}
		return false;
	}
	
	public synchronized void addAccess(int index, long threadId) {
		HashSet<Long> indexAccess = this.indexAccessMap.getOrDefault(index, new HashSet<Long>());
		indexAccess.add(threadId);
		this.indexAccessMap.put(index, indexAccess);
	}

	@Override
	public SharedVariableArrayRef parse(ParseData data) {
		this.objectId = new ObjectId(false);
		objectId.parse(data.getField("objectId"));
		data.getField("indexAccessSet").toList().forEach(new Consumer<ParseData>() {
			@Override
			public void accept(ParseData v) {
				sharedIndexSet.add(v.getIntValue());
			}
		});
		return this;
	}
}