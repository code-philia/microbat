package microbat.instrumentation.model.generator;

import microbat.instrumentation.model.id.RecorderObjectId;

public class ObjectIdParser implements IdGenerator<String, RecorderObjectId>{

	@Override
	public RecorderObjectId createId(String object) {
		return null;
	}

	@Override
	public RecorderObjectId getId(String object) {
		return createId(object);
	}

}
