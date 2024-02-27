package microbat.instrumentation.model.generator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import microbat.instrumentation.model.SharedVariableObjectId;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.StaticFieldLocation;
import sav.common.core.Pair;

public class SharedVariableObjectGenerator implements IdGenerator<Object, SharedVariableObjectId> {
	// the object id generator does not need to store the map
	private ObjectIdGenerator objectIdGenerator = new ObjectIdGenerator(false);
	private ObjectIdGenerator arrayIdGenerator = new ObjectIdGenerator(false);
	private ConcurrentHashMap<Integer, SharedVariableObjectId> sharedVariableMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, SharedVariableArrayRef> arrayRefMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<StaticFieldLocation, HashSet<Long>> staticFieldMap = new ConcurrentHashMap<>();
	
	@Override
	public SharedVariableObjectId createId(Object object) {
		Integer currentId = System.identityHashCode(object);
		ObjectId objectId = objectIdGenerator.createId(object);
		SharedVariableObjectId result = new SharedVariableObjectId(objectId);
		sharedVariableMap.put(currentId, result);
		return result;
	}
	
	public Set<SharedVariableArrayRef> getSharedArrays() {
		return arrayRefMap.values().stream()
				.filter(new Predicate<SharedVariableArrayRef>() {
					@Override
					public boolean test(SharedVariableArrayRef v) {
						return v.isShared();
					}
				})
				.collect(Collectors.<SharedVariableArrayRef>toSet());
	}
	
	public Set<StaticFieldLocation> getSharedStaticFields() {
		return staticFieldMap.entrySet().stream()
				.filter(new Predicate<Entry<StaticFieldLocation, HashSet<Long>>>() {
					@Override
					public boolean test(Entry<StaticFieldLocation, HashSet<Long>> entry) {
						return entry.getValue().size() > 1;
					}
				})
				.map(new Function<Entry<StaticFieldLocation, HashSet<Long>>, StaticFieldLocation>() {
					@Override
					public StaticFieldLocation apply(Entry<StaticFieldLocation, HashSet<Long>> v) {
						return v.getKey();
					}
				})
				.collect(Collectors.<StaticFieldLocation>toSet());
	}

	public void createArrayId(Object arrayId) {
		ObjectId arrayObjectId = arrayIdGenerator.createId(arrayId);
		arrayRefMap.put(System.identityHashCode(arrayId), 
				new SharedVariableArrayRef(arrayObjectId));
	}
	
	public SharedVariableArrayRef getArrayId(Object arrayId) {
		return arrayRefMap.get(System.identityHashCode(arrayId));
	}
	
	public void addAccessStaticField(StaticFieldLocation sfl, long threadId) {
		if (!this.staticFieldMap.containsKey(sfl)) {
			synchronized (staticFieldMap) {
				if (!this.staticFieldMap.containsKey(sfl)) staticFieldMap.put(sfl, new HashSet<>());
			}
		}
		HashSet<Long> values = staticFieldMap.get(sfl);
		synchronized (values) {
			values.add(threadId);
		}
	}
	
	public Collection<SharedVariableObjectId> getSharedVariables() {
		return sharedVariableMap.values().stream().filter(new Predicate<SharedVariableObjectId>() {
			@Override
			public boolean test(SharedVariableObjectId value) {
				return value.getMultiThreadFields().size() > 0;
			}
		}).collect(Collectors.<SharedVariableObjectId>toList());
	}

	@Override
	public SharedVariableObjectId getId(Object object) {
		Integer currentId = System.identityHashCode(object);
		return sharedVariableMap.get(currentId);
	}
	
	
	
}
