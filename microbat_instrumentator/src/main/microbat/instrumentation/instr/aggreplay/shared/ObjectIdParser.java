package microbat.instrumentation.instr.aggreplay.shared;

import java.util.HashSet;

import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.RecorderObjectId;
import microbat.instrumentation.model.id.ThreadId;

public class ObjectIdParser implements Parser<ObjectId> {

	private ThreadId createThreadId(ParseData data) {
		int internalHashCode = Integer.parseInt(data.innerDataMap.get("internalHashCode").getValue());
		return ThreadId.createThread(internalHashCode, data.innerDataMap.get("rootListNode").getValue(),
				data.innerDataMap.get("threadId").getLongValue());
	}
	@Override
	public ObjectId parse(ParseData data) {
		ParseData threadId = data.innerDataMap.get("threadId");
		ThreadId actualThreadId = createThreadId(threadId);
		int objectCounter = Integer.parseInt(data.innerDataMap.get("objectCounter").getValue());
		ObjectId objectId = new ObjectId(actualThreadId, objectCounter);
		return objectId;
	}

}
