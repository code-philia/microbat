package microbat.instrumentation.model;

import microbat.instrumentation.model.id.ObjectId;

// used to represent an object in the replay
// only created for shared objects.
public class ReplayObjectId {
	
	private ObjectId objectId;
	/**
	 * The referenced object that was recorded
	 */
	private RecorderObjectId recorderObject;
	
	
}
