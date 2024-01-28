package microbat.instrumentation.aggreplay.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.RecordingOutput;
import microbat.instrumentation.instr.aggreplay.shared.SharedDataParser;
import microbat.instrumentation.instr.aggreplay.shared.parser.MemoryLocationParser;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.MemoryLocation;
import microbat.instrumentation.model.id.ObjectFieldMemoryLocation;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.ReadCountVector;
import microbat.instrumentation.model.id.ReadWriteAccessList;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.storage.Storable;

public class RecordingOutputTest {
	
	@Test
	public void testParsingObjectFieldLocation() throws IOException {

		ObjectFieldMemoryLocation ofml = new ObjectFieldMemoryLocation("apple", new ObjectId());
		MemoryLocationParser parser = new MemoryLocationParser();
		ParseData parseData = fromStorableData(ofml);
		MemoryLocation result =	parser.parse(parseData);
		assertEquals(ofml, result);
	
	}

	private ParseData fromStorableData(Storable data) throws IOException {
		SharedDataParser parser = new SharedDataParser();
		String result = data.getFromStore();
		return parser.parse(new StringReader(result)).get(0);
	}
	
	@Test
	public void testParsingReadCountVector() throws IOException {
		ReadCountVector rcv = new ReadCountVector();
		ObjectFieldMemoryLocation ofml = new ObjectFieldMemoryLocation("apple", new ObjectId());
		rcv.increment(0, ofml);
		String data = rcv.getFromStore();
		SharedDataParser parser = new SharedDataParser();
		List<ParseData> parseData = parser.parse(new StringReader(data));
		ReadCountVector result = new ReadCountVector();
		assertEquals(result.parse(parseData.get(0)), rcv);
	}
	
	@Test
	public void testThreadParsing() throws IOException {
		ThreadId r = new ThreadId(100);
		ThreadId g = r.createChild(0);
		ThreadId b = g.createChild(10);
		LinkedList<ThreadId> toTest = new LinkedList<>();
		toTest.add(r);
		toTest.add(g);
		toTest.add(b);
		
		
		RecordingOutput output = new RecordingOutput(new ReadCountVector(), 
				new ReadWriteAccessList(), 
				toTest, 
				Collections.<ObjectId>emptyList(), 
				Collections.<SharedMemoryLocation>emptyList());
		testRecordingOutputParsing(output);
		
	}
	
	private void testRecordingOutputParsing(RecordingOutput data) throws IOException {
		String dump = data.getFromStore();
		StringReader values = new StringReader(dump);
		List<ParseData> result = new SharedDataParser().parse(values);
		assertEquals(data, new RecordingOutput().parse(result.get(0)));
	}
	
	@Test
	public void testRecordReadWriteOutput() {
		ReadWriteAccessList rwal = new ReadWriteAccessList();
		ReadCountVector rcv = new ReadCountVector();
		ObjectFieldMemoryLocation memLocation = new ObjectFieldMemoryLocation("a", new ObjectId());
		SharedMemoryLocation smlLocation = new SharedMemoryLocation(memLocation);
		rwal.add(memLocation, new Event(smlLocation), rcv);
		
	}
	
	@Test
	public void testBasicRecordingOutput() {
		
		
	}
	
}
