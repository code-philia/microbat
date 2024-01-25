package microbat.instrumentation.model.id;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.model.storage.Storable;
import microbat.instrumentation.model.storage.Storage;

/**
 * Uniquely identifies an ojbect
 * @author Gabau
 *
 */
public class ObjectId extends Storable {
	public ThreadId threadId;
	public long objectCounter;
	private LinkedList<Event> lockAcquisitionEvents = new LinkedList<>();
	
	public void lockAcquire() {
		lockAcquisitionEvents.add(new Event(null));
	}
	
	private static ThreadLocal<Long> objectCounterThraedLocal = ThreadLocal.withInitial(new Supplier<Long>() {
		@Override
		public Long get() {
			return 0L;
		}
	});
	private Map<String, Set<Long>> fieldAccessMap = new HashMap<>();
	
	public ObjectId() {
		this(true);
	}
	
	public ObjectId(ThreadId threadId, long objectCounter) {
		this.threadId = threadId;
		this.objectCounter = objectCounter;
	}
	
	/**
	 * 
	 * @param incrementLocalCounter false iff this is a reference object
	 */
	public ObjectId(boolean incrementLocalCounter) {
		this.threadId = ThreadIdGenerator.threadGenerator.getId(Thread.currentThread());
		if (incrementLocalCounter) {
			this.objectCounter = objectCounterThraedLocal.get();
			objectCounterThraedLocal.set(objectCounter + 1);
		}
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
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return getFromStore();
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
	
	public Map<String, String> store() {
		HashMap<String, String> fieldMap = new HashMap<String, String>();
		Field[] fields = getClass().getFields();
		for (Field f : fields) {
			if (f.getName().equals("fieldAccessMap")) {
				continue;
			}
			try {
				Object value = f.get(this);
				fieldMap.put(f.getName(), fromObject(value));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Collection<String> concAccessedFieldSet = getMultiThreadFields();
		StringBuilder fieldStringBuilder = new StringBuilder();
		for (String fieldname : concAccessedFieldSet) {
			fieldStringBuilder.append(fieldname);
			fieldStringBuilder.append(Storage.STORE_DELIM_STRING);
		}
		fieldMap.put("fieldAccessMap", fieldStringBuilder.toString());
		
		return fieldMap;
		
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
		return objectCounter == other.objectCounter && threadId.equals(other.threadId);
	}
	
	
}
