package microbat.instrumentation.model;

import java.util.Map;

import microbat.instrumentation.model.id.ObjectId;

/**
 * Class represents object used to initialise
 * the shared memory generator
 * @author Gabau
 *
 */
public interface SharedMemGeneratorInitialiser {

	/**
	 * Get the map from object id to recorder object ids
	 * @return
	 */
	public Map<ObjectId, RecorderObjectId> getObjects();
}
