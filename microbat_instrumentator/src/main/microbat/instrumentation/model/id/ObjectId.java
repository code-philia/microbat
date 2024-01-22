package microbat.instrumentation.model.id;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import microbat.instrumentation.model.storage.Storable;

public class ObjectId implements Storable {
	private long threadId;
	private long objectCounter;
	private static ThreadLocal<Long> objectCounterThraedLocal = ThreadLocal.withInitial(new Supplier<Long>() {
		@Override
		public Long get() {
			return 0L;
		}
	});
	private Map<String, Set<Long>> fieldAccessMap = new HashMap<>();
	
	public ObjectId() {
		this.threadId = Thread.currentThread().getId();
		this.objectCounter = objectCounterThraedLocal.get();
		objectCounterThraedLocal.set(objectCounter + 1);
	}
	
	private void assertHashSet(String field) {
		if (!fieldAccessMap.containsKey(field)) {
			synchronized (fieldAccessMap) {
				if (!fieldAccessMap.containsKey(field)) {
					fieldAccessMap.put(field, new HashSet<Long>());
				}
			}
		}
	}
	
	public void addAccess(long threadId, String field) {
		assertHashSet(field);
		Set<Long> hSet = fieldAccessMap.get(field);
		synchronized (hSet) {
			hSet.add(threadId);
		}
	}
	
	public Collection<String> getMultiThreadFields() {
		LinkedList<String> fieldsAccessedLinkedList = new LinkedList<>();
		for (String fieldString : fieldAccessMap.keySet()) {
			
			if (fieldAccessMap.get(fieldString).size() > 1) {
				fieldsAccessedLinkedList.add(fieldString);
			}
		}
		return fieldsAccessedLinkedList;
	}
	
	public String store() {
		StringBuilder resultBuilder = new StringBuilder();
		resultBuilder.append(threadId);
		resultBuilder.append(Storable.STORE_DELIM_STRING);
		resultBuilder.append(objectCounter);
		resultBuilder.append(Storable.STORE_DELIM_STRING);
		return resultBuilder.toString();
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
