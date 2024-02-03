package microbat.instrumentation.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.instr.aggreplay.ThreadIdInstrumenter;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.ObjectFieldMemoryLocation;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.model.storage.Storable;

/**
 * Class used for keeping track of the shared memory locations during record and replay.
 * Serves a different purpose compared to ObjectId.
 * @author Gabau
 *
 */
public class RecorderObjectId extends Storable {

	//  the fields of this object that are shared.
	private final ObjectId objectId;
	// the memory locations of the shared fields.
	private ConcurrentHashMap<String, SharedMemoryLocation> fieldMemoryLocations = new ConcurrentHashMap<>();
	private LinkedList<Event> lockAcquisitionList = new LinkedList<>();
	public RecorderObjectId(ObjectId objectId) {
		this.objectId = objectId;
	}
	
	/**
	 * Get the set of shared memory locations that represents this object's fields.
	 * @return
	 */
	public Collection<SharedMemoryLocation> getFieldLocations() {
		return fieldMemoryLocations.values();
	}
	
	public List<Event> getLockAcquisition() {
		return this.lockAcquisitionList;
	}
	
	@Override
	protected Map<String, String> store() {
		Map<String, String> result = new HashMap<>();
		result.put("fieldMemoryLocations", fromObject(fieldMemoryLocations));
		result.put("lockAcquisitionList", fromObject(lockAcquisitionList));
		result.put("objectId", fromObject(objectId));
		return result;
	}

	public void acquireLock(Event event) {
		lockAcquisitionList.add(event);
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
