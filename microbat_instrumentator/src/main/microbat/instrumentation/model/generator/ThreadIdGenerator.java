package microbat.instrumentation.model.generator;

import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.model.id.ListId;

public class ThreadIdGenerator implements IdGenerator<Thread, ListId> {
	private ConcurrentHashMap<Long, ListId> idMap = new ConcurrentHashMap<>();
	private ListId rootId = new ListId();

	@Override
	public ListId createId(Thread thread) {
		if (idMap.contains(thread.getId())) {
			return idMap.get(thread.getId());
		}
		ListId currentId = idMap.get(Thread.currentThread().getId());
		if (currentId == null) {
			currentId = rootId;
		}
		ListId valueId = currentId.createChild();
		idMap.put(thread.getId(), valueId);
		return valueId;
	}

	@Override
	public ListId getId(Thread object) {
		return idMap.get(object.getId());
	}

}
