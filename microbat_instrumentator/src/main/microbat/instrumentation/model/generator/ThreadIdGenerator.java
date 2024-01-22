package microbat.instrumentation.model.generator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.storage.Storable;

public class ThreadIdGenerator implements IdGenerator<Thread, ThreadId>, Storable {
	private ConcurrentHashMap<Long, ThreadId> idMap = new ConcurrentHashMap<>();
	private ThreadId rootId = new ThreadId(Thread.currentThread().getId());
	private static final String MAIN_STORE_STRING = ":";
	public ThreadIdGenerator() {
		idMap.put(Thread.currentThread().getId(), rootId);
	}
	
	@Override
	public ThreadId createId(Thread thread) { 
		if (idMap.contains(thread.getId())) {
			return idMap.get(thread.getId());
		}
		ThreadId currentId = idMap.get(Thread.currentThread().getId());
		if (currentId == null) {
			currentId = rootId;
		}
		ThreadId valueId = currentId.createChildWithThread(thread.getId());
		idMap.put(thread.getId(), valueId);
		return valueId;
	}

	@Override
	public ThreadId getId(Thread object) {
		return idMap.get(object.getId());
	}

	@Override
	public String store() {
		StringBuilder sBuilder = new StringBuilder();
		for (Map.Entry<Long, ThreadId> entry : idMap.entrySet()) {
			sBuilder.append(entry.getValue().store());
			sBuilder.append(MAIN_STORE_STRING);
		}
		return sBuilder.toString();
	}

}
