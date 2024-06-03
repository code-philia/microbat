package microbat.instrumentation.model.id;

import microbat.instrumentation.model.storage.Storable;

/**
 * A unique identifier for an objects field,
 * array index or static field
 * @author Gabau
 *
 */
public abstract class MemoryLocation extends Storable {
	
	/**
	 * Returns the object that this memory location belongs to
	 * and null if this memory location does not belong to an object.
	 * @return
	 */
	public ObjectId getObjectId() {
		return null;
	}
	
}
