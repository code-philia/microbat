package microbat.instrumentation.model.generator;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.storage.Storable;

public class ObjectIdGenerator implements IdGenerator<Object, ObjectId> {
	private ConcurrentHashMap<Integer, ObjectId> objectIdMap = new ConcurrentHashMap<>();
	// indicates whether this generator needs to store the map
	private final boolean storeMap;
	
	public ObjectIdGenerator() {
		this.storeMap = true;
	}
	
	/**
	 * Indicates whether this object id generator should use the object map
	 * @param storeMap
	 */
	public ObjectIdGenerator(boolean storeMap) {
		this.storeMap = storeMap;
	}
	
	public Collection<ObjectId> getObjects() {
		return objectIdMap.values();
	}
	
	@Override
	public ObjectId createId(Object object) {
		int hashCode = System.identityHashCode(object);
		if (storeMap && objectIdMap.containsKey(hashCode)) {
			return objectIdMap.get(object);
		}
		
		ObjectId objectId =  new ObjectId();
		if (storeMap) objectIdMap.put(hashCode,objectId);
		return objectId;
	}

	/**
	 * Only works if 
	 */
	@Override
	public ObjectId getId(Object object) {
		int hashCode = System.identityHashCode(object);
		return objectIdMap.get(hashCode);
	}

}
