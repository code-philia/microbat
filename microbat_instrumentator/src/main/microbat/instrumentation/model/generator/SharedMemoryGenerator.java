package microbat.instrumentation.model.generator;

import org.apache.commons.lang.NotImplementedException;

import microbat.instrumentation.model.id.ReferenceObjectId;
import microbat.instrumentation.model.id.SharedMemoryLocation;

/**
 * Used to generate shared memory id's
 * Object fields, Object 
 * 
 * @author Gabau
 *
 */
public class SharedMemoryGenerator {
	
	private IdGenerator<Object, ReferenceObjectId> generator;
	
	public SharedMemoryGenerator(IdGenerator<Object, ReferenceObjectId> generator) {
		this.generator = generator;
	}
	
	public void setObjectIdGenerator(IdGenerator<Object, ReferenceObjectId> generator) {
		this.generator = generator;
	}

	public SharedMemoryLocation ofField(Object object, String fieldName) {
		return generator.getId(object).getField(fieldName);
	}
	
	public SharedMemoryLocation ofStaticField(String className, String fieldName) {
		throw new NotImplementedException();
	}
	
	public SharedMemoryLocation ofArray(Object array, int index) {
		throw new NotImplementedException();
	}
}
