package microbat.instrumentation.instr.aggreplay.shared;

import java.util.HashSet;

import microbat.instrumentation.model.RecorderObjectId;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.ThreadId;

public class ObjectIdParser implements Parser<ObjectId> {

	@Override
	public ObjectId parse(ParseData data) {
		ParseData threadId = data.innerDataMap.get("threadId");
		ThreadId actualThreadId = ThreadId.createThreadId(threadId);
		int objectCounter = Integer.parseInt(data.innerDataMap.get("objectCounter").getValue());
		ObjectId objectId = new ObjectId(actualThreadId, objectCounter);
		return objectId;
	}

}
