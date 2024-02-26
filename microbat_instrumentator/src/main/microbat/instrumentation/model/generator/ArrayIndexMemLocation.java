package microbat.instrumentation.model.generator;

import java.util.Objects;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.model.id.MemoryLocation;
import microbat.instrumentation.model.id.ObjectId;

public class ArrayIndexMemLocation extends MemoryLocation {

	public ObjectId objectId;
	public int index;
	
	public ArrayIndexMemLocation(ObjectId objectId, int index) {
		this.objectId = objectId;
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public ArrayIndexMemLocation(ParseData parseData) {
		this.objectId = new ObjectId(false).parse(parseData.getField("objectId"));
		this.index = parseData.getField("index").getIntValue();
	}

	@Override
	public int hashCode() {
		return Objects.hash(index, objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArrayIndexMemLocation other = (ArrayIndexMemLocation) obj;
		return index == other.index && Objects.equals(objectId, other.objectId);
	}
	
	
	
}
