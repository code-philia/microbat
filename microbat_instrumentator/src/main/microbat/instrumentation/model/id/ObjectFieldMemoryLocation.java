package microbat.instrumentation.model.id;

import java.util.Objects;

public class ObjectFieldMemoryLocation extends MemoryLocation {
	/**
	 * Public fields for storage.
	 */
	public final String fieldName;
	public final ObjectId objectId;	

	public ObjectFieldMemoryLocation(String fieldName, ObjectId objectId) {
		super();
		this.fieldName = fieldName;
		this.objectId = objectId;
	}

	public final String getField() {
		return this.fieldName;
	}
	
	@Override
	public ObjectId getObjectId() {
		return this.objectId;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(fieldName, objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectFieldMemoryLocation other = (ObjectFieldMemoryLocation) obj;
		return Objects.equals(fieldName, other.fieldName) && Objects.equals(objectId, other.objectId);
	}
	
	
	
}
