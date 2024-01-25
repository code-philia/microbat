package microbat.instrumentation.model.generator;

import java.util.Map;

import org.apache.commons.lang.NotImplementedException;

import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.RecorderObjectId;
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
	
	public SharedMemoryGenerator(ObjectIdGenerator objIdGenerator) {
		this.objectIdGenerator = objIdGenerator;
	}
	
	public void setObjectIdRecorderMap(Map<ObjectId, RecorderObjectId> map) {
		this.objectIdRecorderMap = map;
	}
	
	public void init() {
		
	}
	
	public boolean isSharedObject(Object object, String field) {
		return ofField(object, field) != null;
	}

	public SharedMemoryLocation ofField(Object object, String fieldName) {
		ObjectId objectId = objectIdGenerator.getId(object);
		RecorderObjectId value = objectIdRecorderMap.get(objectId);
		if (value == null) return null;
		return value.getField(fieldName);
	}
	
	public SharedMemoryLocation ofStaticField(String className, String fieldName) {
		throw new NotImplementedException();
	}
	
	public SharedMemoryLocation ofArray(Object array, int index) {
		throw new NotImplementedException();
	}
}
