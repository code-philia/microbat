package microbat.instrumentation.model.generator;

import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.model.id.ObjectId;

public class ObjectIdGenerator implements IdGenerator<Object, ObjectId> {
	private ConcurrentHashMap<Integer, ObjectId> objectIdMap = new ConcurrentHashMap<>();
	
	public ObjectIdGenerator() {
	}
	@Override
	public ObjectId createId(Object object) {
		int hashCode = System.identityHashCode(object);
		if (objectIdMap.contains(hashCode)) {
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

}
