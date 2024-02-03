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

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.instr.aggreplay.shared.SharedDataParser;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.model.storage.Storable;
import microbat.instrumentation.model.storage.Storage;

/**
 * Uniquely identifies an ojbect
 * @author Gabau
 *
 */
public class ObjectId extends Storable implements Parser<ObjectId> {
	public ThreadId threadId;
	public long objectCounter;
	
	private static ThreadLocal<Long> objectCounterThraedLocal = ThreadLocal.withInitial(new Supplier<Long>() {
		@Override
		public Long get() {
			return 0L;
		}
	});
	
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
	

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return getFromStore();
	}



	public Map<String, String> store() {
		HashMap<String, String> fieldMap = new HashMap<String, String>();
		Field[] fields = getClass().getFields();
		for (Field f : fields) {
			try {
				Object value = f.get(this);
				fieldMap.put(f.getName(), fromObject(value));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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

	@Override
	public ObjectId parse(ParseData data) {
		this.threadId = SharedDataParser.createThreadId(data.getField("threadId"));
		this.objectCounter = data.getField("objectCounter").getLongValue();
		return this;
	}
	
	
}
