package microbat.instrumentation.instr.aggreplay.shared.parser;

import microbat.instrumentation.instr.aggreplay.shared.ObjectIdParser;
import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.instr.aggreplay.shared.SharedDataParser;
import microbat.instrumentation.model.generator.ArrayIndexMemLocation;
import microbat.instrumentation.model.id.MemoryLocation;
import microbat.instrumentation.model.id.ObjectFieldMemoryLocation;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.StaticFieldLocation;

public class MemoryLocationParser implements Parser<MemoryLocation> {
	private static class ObjectFieldMemLocationParser implements Parser<ObjectFieldMemoryLocation> {
		@Override
		public ObjectFieldMemoryLocation parse(ParseData data) {
			return new ObjectFieldMemoryLocation(data.getField("fieldName").getValue(),
					new ObjectIdParser().parse(data.getField("objectId")));
		}
		
	}
	
	@Override
	public MemoryLocation parse(ParseData data) {
		if (data.isClass(ObjectFieldMemoryLocation.class)) {
			ObjectFieldMemLocationParser ofmParser = new ObjectFieldMemLocationParser();
			return ofmParser.parse(data);
		}
		if (data.isClass(ArrayIndexMemLocation.class)) {
			return new ArrayIndexMemLocation(data);
		}
		
		if (data.isClass(StaticFieldLocation.class)) {
			return new StaticFieldLocation(data);
		}
		return null;
	}

}
