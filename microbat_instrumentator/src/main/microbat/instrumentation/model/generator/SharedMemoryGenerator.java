package microbat.instrumentation.model.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

import microbat.instrumentation.instr.aggreplay.output.SharedVariableOutput;
import microbat.instrumentation.model.RecorderObjectId;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.SharedMemoryLocation;

/**
 * Used to generate shared memory id's
 * Object fields, Object 
 * 
 * @author Gabau
 *
 */
public class SharedMemoryGenerator {

	/**
	 * TODO:
	 * ObjectId generator currently stores data on the
	 * object id's lock acquisition,
	 * while object id recorder map stores data on
	 * the field's shared memory locations. Would
	 * be better to have the two data in one object,
	 * but it isn't straightforward to implement.
	 * 
	 */
	private ObjectIdGenerator objectIdGenerator = new ObjectIdGenerator();
	private Map<ObjectId, RecorderObjectId> objectIdRecorderMap;
	
	public Set<ObjectId> getSharedObjects() {
		return objectIdRecorderMap.keySet();
	}
	
	public List<SharedMemoryLocation> getAllLocations() {
		List<SharedMemoryLocation> result = new LinkedList<>();
		for (RecorderObjectId recObjectId : objectIdRecorderMap.values()) {
			result.addAll(recObjectId.getFieldLocations());
		}
		return result;
	}
	
	public SharedMemoryGenerator(ObjectIdGenerator objIdGenerator) {
		this.objectIdGenerator = objIdGenerator;
	}
	
	public void updateSharedVariables(SharedVariableOutput sharedVar) {
		setObjectIdRecorderMap(sharedVar.getObjects());
	}
	
	private void setObjectIdRecorderMap(Map<ObjectId, RecorderObjectId> map) {
		this.objectIdRecorderMap = map;
	}
	
	public void init() {
		
	}
	
	public RecorderObjectId ofObject(Object object) {
		ObjectId objectId = objectIdGenerator.getId(object);
		RecorderObjectId result = null;
		synchronized (this.objectIdRecorderMap) {
			if (!this.objectIdRecorderMap.containsKey(objectId)) {
				result = new RecorderObjectId(objectId);
				this.objectIdRecorderMap.put(objectId, result);
			} else {
				result = this.objectIdRecorderMap.get(objectId);
			}
		}
		return result;
	}
	
	public boolean isSharedObject(Object object, String field) {
		return ofField(object, field) != null;
	}
	
	public boolean isSharedObject(ObjectId object) {
		return this.objectIdRecorderMap.containsKey(object);
	}

	public SharedMemoryLocation ofField(Object object, String fieldName) {
		ObjectId objectId = objectIdGenerator.getId(object);
		RecorderObjectId value = objectIdRecorderMap.get(objectId);
		if (value == null) return null;
		return value.getField(fieldName);
	}
	
	public Map<ObjectId, List<Event>> getLockAcquisitionMap() {
		Map<ObjectId, List<Event>> resultList = new HashMap<>();
		for (RecorderObjectId objectId : this.objectIdRecorderMap.values()) {
			resultList.put(objectId.getObjectId(), objectId.getLockAcquisition());
		}
		return resultList;
	}
	
	public SharedMemoryLocation ofStaticField(String className, String fieldName) {
		throw new NotImplementedException();
	}
	
	public SharedMemoryLocation ofArray(Object array, int index) {
		throw new NotImplementedException();
	}
}
