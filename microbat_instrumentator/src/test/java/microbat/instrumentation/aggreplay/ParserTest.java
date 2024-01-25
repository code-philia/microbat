package microbat.instrumentation.aggreplay;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.SharedDataParser;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.RecorderObjectId;

public class ParserTest {

	
	@Test
	public void basicTest() {
		String testString = "{\r\n"
				+ "ObjectType:microbat.instrumentation.model.generator.ThreadIdGenerator,\r\n"
				+ "1:{\r\n"
				+ "ObjectType:microbat.instrumentation.model.id.ThreadId,\r\n"
				+ "rootListNode:0;,\r\n"
				+ "internalHashCode:-594882159,\r\n"
				+ "},\r\n"
				+ "12:{\r\n"
				+ "ObjectType:microbat.instrumentation.model.id.ThreadId,\r\n"
				+ "rootListNode:0;0;,\r\n"
				+ "internalHashCode:-535803403,\r\n"
				+ "},\r\n"
				+ "13:{\r\n"
				+ "ObjectType:microbat.instrumentation.model.id.ThreadId,\r\n"
				+ "rootListNode:2;0;,\r\n"
				+ "internalHashCode:-535803329,\r\n"
				+ "},\r\n"
				+ "14:{\r\n"
				+ "ObjectType:microbat.instrumentation.model.id.ThreadId,\r\n"
				+ "rootListNode:1;0;,\r\n"
				+ "internalHashCode:-535803366,\r\n"
				+ "},\r\n"
				+ "},{\r\n"
				+ "ObjectType:microbat.instrumentation.model.id.ObjectId,\r\n"
				+ "threadId:{\r\n"
				+ "ObjectType:microbat.instrumentation.model.id.ThreadId,\r\n"
				+ "rootListNode:0;,\r\n"
				+ "internalHashCode:-594882159,\r\n"
				+ "},\r\n"
				+ "fieldAccessMap:value:,\r\n"
				+ "objectCounter:1,\r\n"
				+ "},!";
		SharedDataParser parser = new SharedDataParser();
		StringReader reader = new StringReader(testString);
		try {
			List<ParseData> data = parser.parse(reader);
			Map<ObjectId, RecorderObjectId> objectIds = parser.generateObjectIds(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
