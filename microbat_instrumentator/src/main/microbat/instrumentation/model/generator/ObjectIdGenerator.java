package microbat.instrumentation.model.generator;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.storage.Storable;

public class ObjectIdGenerator implements IdGenerator<Object, ObjectId> {
	private ConcurrentHashMap<Integer, ObjectId> objectIdMap = new ConcurrentHashMap<>();
	
	
	public ObjectIdGenerator() {
	}
	
	public Collection<ObjectId> getObjects() {
		return objectIdMap.values();
	}
	
	@Override
	public ObjectId createId(Object object) {
		int hashCode = System.identityHashCode(object);
		if (objectIdMap.containsKey(hashCode)) {
			return objectIdMap.get(object);
		}
		ObjectId objectId =  new ObjectId();
		objectIdMap.put(hashCode,objectId);
		return objectId;
	}

	@Override
	public ObjectId getId(Object object) {
		int hashCode = System.identityHashCode(object);
		return objectIdMap.get(hashCode);
	}
	
	public HashSet<Storable> generateToStoreHashSet() {
		HashSet<Storable> valueHashSet = new HashSet<>();
		for (ObjectId kId : objectIdMap.values()) {
			if (kId.getMultiThreadFields().size() == 0) {
				continue;
			}
			valueHashSet.add(kId);
		}
		return valueHashSet;
	}

}
