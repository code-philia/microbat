package microbat.instrumentation.model.id;

import java.util.Objects;

public class FieldId {
	private ObjectId objectId;
	private String name;
	
	public FieldId(ObjectId objectId, String name) {
		this.objectId = objectId;
		this.name = name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FieldId other = (FieldId) obj;
		return Objects.equals(name, other.name) && Objects.equals(objectId, other.objectId);
	}

	
}
