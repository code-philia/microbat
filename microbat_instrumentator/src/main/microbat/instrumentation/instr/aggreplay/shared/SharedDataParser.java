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
	
	public static void main(String[] args) throws IOException {
		String r = "{\r\n"
		+ "ObjectType:microbat.instrumentation.model.generator.ThreadIdGenerator,\r\n"
		+ "    1:{\r\n"
		+ "        ObjectType:microbat.instrumentation.model.id.ThreadId,\r\n"
		+ "        rootListNode:0;,\r\n"
		+ "        internalHashCode:-594882159,\r\n"
		+ "    },\r\n"
		+ "23:{\r\n"
		+ "ObjectType:microbat.instrumentation.model.id.ThreadId,\r\n"
		+ "rootListNode:1;0;,\r\n"
		+ "internalHashCode:-535803366,\r\n"
		+ "},\r\n"
		+ "12:{\r\n"
		+ "ObjectType:microbat.instrumentation.model.id.ThreadId,\r\n"
		+ "rootListNode:0;0;,\r\n"
		+ "internalHashCode:-535803403,\r\n"
		+ "},\r\n"
		+ "14:{\r\n"
		+ "ObjectType:microbat.instrumentation.model.id.ThreadId,\r\n"
		+ "rootListNode:2;0;,\r\n"
		+ "internalHashCode:-535803329,\r\n"
		+ "},\r\n"
		+ "},{\r\n"
		+ "ObjectType:microbat.instrumentation.model.id.ObjectId,\r\n"
		+ "threadId:{\r\n"
		+ "ObjectType:microbat.instrumentation.model.id.ThreadId,\r\n"
		+ "rootListNode:0;,\r\n"
		+ "internalHashCode:-594882159,\r\n"
		+ "},\r\n"
		+ "fieldAccessMap:[value;],\r\n"
		+ "objectCounter:1,\r\n"
		+ "},";
		SharedDataParser parser = new SharedDataParser();
		BufferedReader br = new BufferedReader(new StringReader(r));
		parser.parse(br);
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
