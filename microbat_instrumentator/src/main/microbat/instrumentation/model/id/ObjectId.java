package microbat.instrumentation.model.id;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ObjectId {
	private long threadId;
	private long objectCounter;
	private static ThreadLocal<Long> objectCounterThraedLocal = ThreadLocal.withInitial(new Supplier<Long>() {
		@Override
		public Long get() {
			return 0L;
		}
	});
	private ConcurrentHashMap<String, HashSet<Long>> fieldAccessMap = new ConcurrentHashMap<>();
	
	public ObjectId() {
		this.threadId = Thread.currentThread().getId();
		this.objectCounter = objectCounterThraedLocal.get();
		objectCounterThraedLocal.set(objectCounter + 1);
	}
	
	private void assertHashSet(String field) {
		if (!fieldAccessMap.contains(field)) {
			synchronized (fieldAccessMap) {
				if (!fieldAccessMap.contains(field)) {
					fieldAccessMap.put(field, new HashSet<Long>());
				}
			}
		}
	}
	
	public void addAccess(long threadId, String field) {
		assertHashSet(field);
		HashSet<Long> hSet = fieldAccessMap.get(field);
		synchronized (hSet) {
			hSet.add(threadId);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectCounter, threadId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectId other = (ObjectId) obj;
		return objectCounter == other.objectCounter && threadId == other.threadId;
	}
	
	
}
