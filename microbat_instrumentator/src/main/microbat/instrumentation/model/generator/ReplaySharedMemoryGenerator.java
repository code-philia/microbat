package microbat.instrumentation.model.generator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import microbat.instrumentation.model.RecorderObjectId;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.SharedMemoryLocation;

/**
 * Shared mem generator used specifically for replay
 * @author Gabau
 *
 */
public class ReplaySharedMemoryGenerator extends SharedMemoryGenerator {
	
	/**
	 * The shared memory locations referring to the object field locations
	 */

	public ReplaySharedMemoryGenerator(ObjectIdGenerator objIdGenerator) {
		super(objIdGenerator);
	}

	/**
	 * TODO: Initalises the generator with the input shared objects.
	 * @param sharedObjects
	 */
	public void init(HashSet<ObjectId> sharedObjects) {
		
	}
	
	@Override
	public void setObjectIdRecorderMap(Map<ObjectId, RecorderObjectId> map) {
		// TODO Auto-generated method stub
		super.setObjectIdRecorderMap(map);
	}

	@Override
	public boolean isSharedObject(Object object, String field) {
		// TODO Auto-generated method stub
		return super.isSharedObject(object, field);
	}

	@Override
	public boolean isSharedObject(ObjectId object) {
		// TODO Auto-generated method stub
		return super.isSharedObject(object);
	}

	@Override
	public SharedMemoryLocation ofField(Object object, String fieldName) {
		// TODO Auto-generated method stub
		return super.ofField(object, fieldName);
	}

	@Override
	public SharedMemoryLocation ofStaticField(String className, String fieldName) {
		// TODO Auto-generated method stub
		return super.ofStaticField(className, fieldName);
	}

	@Override
	public SharedMemoryLocation ofArray(Object array, int index) {
		// TODO Auto-generated method stub
		return super.ofArray(array, index);
	}

	
}
