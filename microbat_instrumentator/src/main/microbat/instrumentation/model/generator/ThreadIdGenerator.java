package microbat.instrumentation.model.generator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.storage.Storable;

public class ThreadIdGenerator extends Storable implements IdGenerator<Thread, ThreadId> {
	private ConcurrentHashMap<Long, ThreadId> idMap = new ConcurrentHashMap<>();
	private ThreadId rootId = null;
	public static final ThreadIdGenerator threadGenerator = new ThreadIdGenerator();
	
	public ThreadId getRoot() {
		if (rootId == null) {
			createRootId();
		}
		return rootId;
	}
	
	public ThreadIdGenerator() {
		
	}
	
	public List<ThreadId> getThreadIds() {
		return idMap.values().stream().collect(Collectors.<ThreadId>toList());
	}
	
	private synchronized void createRootId() {
		if (this.rootId != null) return;
		this.rootId = new ThreadId(Thread.currentThread().getId());
		idMap.put(Thread.currentThread().getId(), rootId);
	}
	
	public void createId(Thread thread, int spawnOrder) {
		ThreadId id = createId(thread);
		id.setSpawnOrder(spawnOrder);
	}

	@Override
	public ThreadId createId(Thread thread) { 
		if (idMap.containsKey(thread.getId())) {
			return idMap.get(thread.getId());
		}
		// Only create root when creating child thread.
		ThreadId currentId = idMap.get(Thread.currentThread().getId());
		if (currentId == null && rootId == null) {
			createRootId();
			currentId = rootId;
		}
		if (currentId == null) {
			// when the current thread is outside of the application context
			// handle via rootId -> TODO(Gab): return null instead, no reason to keep track
			// of the shutdown thread.
			currentId = rootId;
		}
		
		ThreadId valueId = currentId.createChildWithThread(thread.getId());
		idMap.put(thread.getId(), valueId);
		return valueId;
	}

	@Override
	public ThreadId getId(Thread object) {
		return getId(object.getId());
	}

	public ThreadId getId(long threadId) {
		if (idMap.get(threadId) == null) {
			if (rootId == null) {
				createRootId();
				return rootId;
			}
			// Not supposed to happen
			// Happen's when Thread.start() isn't used to initialise the thread.
			return null;
		}
		return idMap.get(threadId);
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
