package microbat.instrumentation.model.generator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.model.SharedVariableObjectId;
import microbat.instrumentation.model.id.ObjectId;

public class SharedVariableObjectGenerator implements IdGenerator<Object, SharedVariableObjectId> {
	// the object id generator does not need to store the map
	private ObjectIdGenerator objectIdGenerator = new ObjectIdGenerator(false);
	private ConcurrentHashMap<Integer, SharedVariableObjectId> sharedVariableMap = new ConcurrentHashMap<>();

	@Override
	public SharedVariableObjectId createId(Object object) {
		Integer currentId = System.identityHashCode(object);
		ObjectId objectId = objectIdGenerator.createId(object);
		SharedVariableObjectId result = new SharedVariableObjectId(objectId);
		sharedVariableMap.put(currentId, result);
		return result;
	}
	
	public Collection<SharedVariableObjectId> getSharedVariables() {
		return sharedVariableMap.values();
	}

	@Override
	public SharedVariableObjectId getId(Object object) {
		Integer currentId = System.identityHashCode(object);
		return sharedVariableMap.get(currentId);
	}
	
	
	
}
