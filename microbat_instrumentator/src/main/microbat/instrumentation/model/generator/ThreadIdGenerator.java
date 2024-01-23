package microbat.instrumentation.model.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.storage.Storable;

public class ThreadIdGenerator extends Storable implements IdGenerator<Thread, ThreadId> {
	private ConcurrentHashMap<Long, ThreadId> idMap = new ConcurrentHashMap<>();
	private ThreadId rootId = new ThreadId(Thread.currentThread().getId());
	public ThreadIdGenerator() {
		idMap.put(Thread.currentThread().getId(), rootId);
	}
	
	@Override
	public ThreadId createId(Thread thread) { 
		if (idMap.containsKey(thread.getId())) {
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
	protected Map<String, String> store() {
		Map<String, String> result = new HashMap<>();
		for (Map.Entry<Long, ThreadId> entry : this.idMap.entrySet()) {
			result.put(entry.getKey().toString(), entry.getValue().getFromStore());
		}
		return result;
	}


}
