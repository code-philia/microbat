package microbat.instrumentation.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.storage.Storable;
import microbat.instrumentation.model.storage.Storage;

public class SharedVariableObjectId extends Storable implements Parser<SharedVariableObjectId> {
	private ObjectId objectId;
	private Map<String, Set<Long>> fieldAccessMap;
	private List<String> fieldAccessList;
	
	public SharedVariableObjectId(ObjectId objectId) {
		this.objectId = objectId;
	}
	
	public SharedVariableObjectId() {
		
	}
	
	@Override
	public SharedVariableObjectId parse(ParseData data) {
		this.objectId = new ObjectId(false);
		this.objectId = objectId.parse(data.getField("objectId"));
		this.fieldAccessList = data.getField("fieldAccessList").toList(new Function<ParseData, String>() {
			@Override
			public String apply(ParseData data) {
				return data.getValue();
			}
		});
		return this;
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
	
	public List<String> getMultiThreadFields() {
		LinkedList<String> fieldsAccessedLinkedList = new LinkedList<>();
		for (String fieldString : fieldAccessMap.keySet()) {
			
			if (fieldAccessMap.get(fieldString).size() > 1) {
				fieldsAccessedLinkedList.add(fieldString);
			}
		}
		return fieldsAccessedLinkedList;
	}


	@Override
	protected Map<String, String> store() {
		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put("fieldAccessMap", Storable.fromList(getMultiThreadFields()));
		fieldMap.put("objectId", fromObject(objectId));
		return fieldMap;
	}
	
	
	
}
