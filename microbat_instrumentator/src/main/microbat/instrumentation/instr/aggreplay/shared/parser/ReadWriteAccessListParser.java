package microbat.instrumentation.instr.aggreplay.shared.parser;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.model.id.ReadWriteAccessList;

public class ReadWriteAccessListParser implements Parser<ReadWriteAccessList> {

	@Override
	public ReadWriteAccessList parse(ParseData data) {
		ReadWriteAccessList accessList = new ReadWriteAccessList();
		return null;
	}

}
