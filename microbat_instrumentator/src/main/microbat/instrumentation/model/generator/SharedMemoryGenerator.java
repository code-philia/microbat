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
import microbat.instrumentation.model.SharedMemGeneratorInitialiser;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.model.id.StaticFieldLocation;

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
	private Map<Integer, ObjectId> arrayObjectIdMap = new HashMap<>();
	
	private Map<ObjectId, RecorderObjectId> objectIdRecorderMap = new HashMap<>();
	private Map<ObjectId, Map<Integer, SharedMemoryLocation>> arrayMemLocationsMap = new HashMap<>();
	private Map<StaticFieldLocation, SharedMemoryLocation> staticMemLocationsMap = new HashMap<>();
	
	
	public Set<ObjectId> getSharedObjects() {
		return objectIdRecorderMap.keySet();
	}
	
	
	public List<SharedMemoryLocation> getAllLocations() {
		List<SharedMemoryLocation> result = new LinkedList<>();
		for (RecorderObjectId recObjectId : objectIdRecorderMap.values()) {
			result.addAll(recObjectId.getFieldLocations());
		}
		
		result.addAll(staticMemLocationsMap.values());
		for (Map<Integer, SharedMemoryLocation> location : arrayMemLocationsMap.values()) {
			result.addAll(location.values());
		}
		return result;
	}
	
	public SharedMemoryGenerator(ObjectIdGenerator objIdGenerator) {
		this.objectIdGenerator = objIdGenerator;
	}
	
	public void init(SharedMemGeneratorInitialiser sharedVar) {
		setObjectIdRecorderMap(sharedVar.getObjects());
		setStaticFields(sharedVar.getStaticFields());
		setArrayIndexes(sharedVar);
	}
	
	private void setArrayIndexes(SharedMemGeneratorInitialiser sharedMGI) {
		Set<SharedMemoryLocation> res = sharedMGI.getArrayRefs();
		arrayMemLocationsMap = new HashMap<>();
		res.forEach(field -> {
			Map<Integer, SharedMemoryLocation> shmMap = null;
			ArrayIndexMemLocation iml = (ArrayIndexMemLocation) field.getLocation();
			if (!arrayMemLocationsMap.containsKey(iml.getObjectId())) {
				arrayMemLocationsMap.put(iml.getObjectId(), new HashMap<Integer, SharedMemoryLocation>());
			}
			shmMap = arrayMemLocationsMap.get(iml.getObjectId());
			shmMap.put(iml.getIndex(), field);
		});
	}
	
	
	private void setStaticFields(Set<SharedMemoryLocation> sfl) {
		sfl.forEach(field -> this.staticMemLocationsMap.put((StaticFieldLocation) field.getLocation(), field));
	}
	
	private void setObjectIdRecorderMap(Map<ObjectId, RecorderObjectId> map) {
		this.objectIdRecorderMap = map;
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
		if (object == null) return false;
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
		return staticMemLocationsMap.get(new StaticFieldLocation(className, fieldName));
	}
	
	public boolean isSharedStaticField(String className, String fieldName) {
		return staticMemLocationsMap.containsKey(new StaticFieldLocation(className, fieldName));
	}
	
	public boolean isSharedArray(Object array, int access) {
		return ofArray(array, access) != null;
	}
	
	public void newArray(Object obj) {
		this.arrayObjectIdMap.put(System.identityHashCode(obj), new ObjectId());
	}
	
	public SharedMemoryLocation ofArray(Object array, int index) {
		if (!arrayObjectIdMap.containsKey(System.identityHashCode(array))) {
			return null;
		}
		ObjectId oId = arrayObjectIdMap.get(System.identityHashCode(array));
		if (!arrayMemLocationsMap.containsKey(oId)) {
			return null;
		}
		return arrayMemLocationsMap.get(oId).get(index);
	}
}
