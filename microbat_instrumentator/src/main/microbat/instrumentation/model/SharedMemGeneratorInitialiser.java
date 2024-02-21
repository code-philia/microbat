package microbat.instrumentation.model;

import java.util.Map;
import java.util.Set;

import microbat.instrumentation.model.generator.SharedVariableArrayRef;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.StaticFieldLocation;

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
	
	public Set<SharedVariableArrayRef> getArrayRefs();
	
	public Set<StaticFieldLocation> getStaticFields();
}
