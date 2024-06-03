package microbat.instrumentation.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.storage.Storable;
import microbat.instrumentation.model.storage.Storage;

/**
 * Represents an object during the shared variable detection
 * stage
 * @author Gabau
 *
 */
public class SharedVariableObjectId extends Storable implements Parser<SharedVariableObjectId> {
	private ObjectId objectId;
	private Map<String, Set<Long>> fieldAccessMap = new HashMap<>();
	private List<String> fieldAccessList = new LinkedList<>();
	
	public SharedVariableObjectId(ObjectId objectId) {
		this.objectId = objectId;
	}
	
	public SharedVariableObjectId() {
		
	}
	
	public List<String> getFieldAccessList() {
		return this.fieldAccessList;
	}
	
	public ObjectId getObjectId() {
		return this.objectId;
	}
	
	/**
	 * The stored data only includes the field access list, no need for the map.
	 */
	@Override
	public SharedVariableObjectId parse(ParseData data) {
		this.objectId = new ObjectId(false);
		this.objectId = objectId.parse(data.getField("objectId"));
		this.fieldAccessList = data.getField("fieldAccessList").toList(new Function<ParseData, String>() {
			@Override
			public String apply(ParseData data) {
				return data.getValue();
			}
		});
		return this;
	}
	
	
	private void assertHashSet(String field) {
		if (!fieldAccessMap.containsKey(field)) {
			synchronized (fieldAccessMap) {
				if (!fieldAccessMap.containsKey(field)) {
					fieldAccessMap.put(field, new HashSet<Long>());
				}
			}
		}
	}
	
	public void addAccess(long threadId, String field) {
		assertHashSet(field);
		Set<Long> hSet = fieldAccessMap.get(field);
		// no need to address this field again
		if (hSet.size() == 2) return;
		synchronized (hSet) {
			hSet.add(threadId);
			// only do this on the second access
			if (hSet.size() == 2) {
				synchronized(fieldAccessList) {
					fieldAccessList.add(field);	
				}
			}
		}
	}
	
	public List<String> getMultiThreadFields() {
		return fieldAccessList;
	}


	@Override
	protected Map<String, String> store() {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("fieldAccessList", Storable.fromList(getMultiThreadFields()));
		fieldMap.put("objectId", fromObject(objectId));
		return fieldMap;
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
		SharedVariableObjectId other = (SharedVariableObjectId) obj;
		return Objects.equals(fieldAccessList, other.fieldAccessList) && Objects.equals(objectId, other.objectId);
	}


	
	
}
