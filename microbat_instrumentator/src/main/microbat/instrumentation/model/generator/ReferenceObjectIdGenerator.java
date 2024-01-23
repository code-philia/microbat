package microbat.instrumentation.model.generator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.ReferenceObjectId;

public class ReferenceObjectIdGenerator implements IdGenerator<Object, ReferenceObjectId> {

	private Map<Integer, ReferenceObjectId> objectMap = new HashMap<>();
	// TODO:
	private Map<ObjectId, Set<String>> fieldSetMap = new HashMap<>();

	
	@Override
	public ReferenceObjectId createId(Object object) {
		ReferenceObjectId id = new ReferenceObjectId();
		id.updateSharedFieldSet(fieldSetMap.getOrDefault(id.getObjectId(), Collections.<String>emptySet()));
		objectMap.put(System.identityHashCode(object), id);
		return id;
	}

	@Override
	public ReferenceObjectId getId(Object object) {
		return objectMap.get(System.identityHashCode(object));
	}

}
