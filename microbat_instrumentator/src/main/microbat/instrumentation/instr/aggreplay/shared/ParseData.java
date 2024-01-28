package microbat.instrumentation.instr.aggreplay.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import sav.common.core.Pair;

public class ParseData {
	Map<String, ParseData> innerDataMap = new HashMap<>();
	List<ParseData> listData;
	String actualData = null;
	
	public String getObjectType() {
		return innerDataMap.get("ObjectType").getValue();
	}
	
	public ParseData getField(String field) {
		return innerDataMap.get(field);
	}
	public String getValue() {
		return actualData;
	}
	
	public Long getLongValue() {
		return Long.parseLong(actualData);
	}
	
	public <T> List<T> toList(Function<ParseData, T> function) {
		if (this.listData == null) {
			return Collections.emptyList();
		}
		return this.listData.stream().map(function).collect(Collectors.<T>toList());
	}
	
	public List<Pair<ParseData, ParseData>> toPairList() {
		if (this.listData == null) {
			return Collections.emptyList();
		}
		LinkedList<Pair<ParseData, ParseData>> result = new LinkedList<>();
		ParseData data1 = null;
		ParseData data2 = null;
		int ctr = 0;
		for (ParseData data : listData) {
			if (ctr%2 == 0) {
				data1 = data;
			} else {
				data2 = data;
				result.add(Pair.of(data1, data2));
			}
			ctr++;
		}
		
		return result;
	}
	
	public List<ParseData> toList() {
		return listData;
	}
	
	public int getIntValue() {
		return Integer.parseInt(actualData);
	}

	
	public boolean isClass(Class<?> clazz) {
		return getObjectType().equals(clazz.getName());
	}
}