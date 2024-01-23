package microbat.instrumentation.model.generator;

import microbat.instrumentation.model.id.ReferenceObjectId;

public class ObjectIdParser implements IdGenerator<String, ReferenceObjectId>{

	@Override
	public ReferenceObjectId createId(String object) {
		
		return null;
	}

	@Override
	public ReferenceObjectId getId(String object) {
		return createId(object);
	}

}
