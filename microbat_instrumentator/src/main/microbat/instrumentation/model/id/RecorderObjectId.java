package microbat.instrumentation.model.id;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.instr.aggreplay.ThreadIdInstrumenter;

/**
 * Class used for keeping track of the shared memory locations during record and replay.
 * Serves a different purpose compared to ObjectId
 * @author Gabau
 *
 */
public class RecorderObjectId {

	//  the fields of this object that are shared.
	private final ObjectId objectId;
	// the memory locations of the shared fields.
	private ConcurrentHashMap<String, SharedMemoryLocation> fieldMemoryLocations = new ConcurrentHashMap<>();
	
	public RecorderObjectId(ObjectId objectId) {
		this.objectId = objectId;
	}
	
	
	public void updateSharedFieldSet(Collection<String> fieldSet) {
		for (String fieldName : fieldSet) {
			ObjectFieldMemoryLocation location = new ObjectFieldMemoryLocation(fieldName,
					this.objectId);
			fieldMemoryLocations.put(fieldName, new SharedMemoryLocation(location));
		}
	}
	
	public Set<String> getField() {
		return fieldMemoryLocations.keySet();
	}
	
	public ObjectId getObjectId() {
		return this.objectId;
	}
	
	/**
	 * Gets the field from the object.
	 * Is null if the field is not shared exist.
	 * 
	 * @param field
	 * @return
	 */
	public SharedMemoryLocation getField(String field) {
		return fieldMemoryLocations.get(field);
	}


	@Override
	public int hashCode() {
		return Objects.hash(objectId);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RecorderObjectId other = (RecorderObjectId) obj;
		return Objects.equals(objectId, other.objectId);
	}


	
	
}
