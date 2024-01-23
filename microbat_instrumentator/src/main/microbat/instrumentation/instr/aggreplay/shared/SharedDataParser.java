package microbat.instrumentation.instr.aggreplay.shared;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.RecorderObjectId;
import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.storage.Storage;
import microbat.instrumentation.output.OutputReader;

/**
 * Performs parsing of data generated during instrumentation.
 * Generic object parser
 * @author Gabau
 *
 */
public class SharedDataParser {
	HashSet<ThreadId> sharedThreads;
	HashSet<ObjectId> sharedVariables;
	
	
	
	public static class ParseData {
		Map<String, ParseData> innerDataMap = new HashMap<>();
		String actualData = null;
		
		public String getObjectType() {
			return innerDataMap.get("ObjectType").actualData;
		}
		
		public boolean isClass(Class<?> clazz) {
			return getObjectType().equals(clazz.getName());
		}
	}
	
	public ThreadId createThreadId(ParseData data) {
		int internalHashCode = Integer.parseInt(data.innerDataMap.get("internalHashCode").actualData);
		return ThreadId.createThread(internalHashCode, data.innerDataMap.get("rootListNode").actualData);
	}
	
	private HashSet<String> parseSet(String setString) {
		HashSet<String> result = new HashSet<>();
		// TODO: add the values in the string to the set.
		String[] fieldNames = setString.split(Storage.STORE_DELIM_STRING);
		for (String fieldName : fieldNames) {
			result.add(fieldName);
		}
		return result;
	}
	
	public RecorderObjectId parseRecordObject(ParseData data) {
		ParseData threadId = data.innerDataMap.get("threadId");
		ThreadId actualThreadId = createThreadId(threadId);
		int objectCounter = Integer.parseInt(data.innerDataMap.get("objectCounter").actualData);
		ObjectId objectId = new ObjectId(actualThreadId, objectCounter);
		HashSet<String> fieldNames = parseSet(data.innerDataMap.get("fieldAccessMap").actualData);
		RecorderObjectId result = new RecorderObjectId(objectId);
		result.updateSharedFieldSet(fieldNames);
		return result;
	}
	
	public Map<ObjectId, RecorderObjectId> generateObjectIds(List<ParseData> data) {
		HashMap<ObjectId, RecorderObjectId> resultObjectIdsHashMap = new HashMap<>();
		for (ParseData value : data) {
			if (value.isClass(ObjectId.class)) {
				RecorderObjectId roiObjectId = parseRecordObject(value);
				resultObjectIdsHashMap.put(roiObjectId.getObjectId(), roiObjectId);
			}
		}
		return resultObjectIdsHashMap;
	}
	
	public List<ParseData> parse(Reader data) throws IOException {
		LinkedList<ParseData> result = new LinkedList<>();
		int k = data.read();
		while (k != -1) {
			while (k != Storage.START_OBJECT_STRING.charAt(0)) {
				if (k == -1) break;
				k = data.read();
			}
			if (k == -1) break;
			result.add(parseInner(data));	
			k = data.read();
		}
		return result;
	}
	
	private ParseData parseInner(Reader reader) throws IOException {
		ParseData parseData = new ParseData();
		if (reader.read() == Storage.START_OBJECT_STRING.charAt(0)) {
			return parseInner(reader);
		}
		StringBuilder sb = new StringBuilder();
		while (true) {
			char c = (char) reader.read();
			if (c == Storage.CLOSE_OBJECT_STRING.charAt(0)) {
				break;
			}
			if (c == Storage.OBJECT_SEPARATOR.charAt(0)
					|| c == '\n' || c == '\r') {
				continue;
			}
			if (c == Storage.STORE_DELIM_STRING.charAt(0)) {
				String field = sb.toString().trim();
				char k = (char) reader.read();
				ParseData value = new ParseData();
				if (k == Storage.START_OBJECT_STRING.charAt(0)) {
					// handle object
					value = parseInner(reader);
				} else {
					StringBuilder innerValue = new StringBuilder();
					while (k != Storage.OBJECT_SEPARATOR.charAt(0)) {
						innerValue.append(k);
						k = (char) reader.read();
					}
					value.actualData = innerValue.toString();
				}
				parseData.innerDataMap.put(field, value);
				sb.setLength(0);
				continue;
			}
			sb.append(c);
		}
		return parseData;
	}
}
