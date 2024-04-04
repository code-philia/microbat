package microbat.instrumentation.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.SharedDataParser;

public class StorableReader {
	private OutputReader reader;
	private String programMsgString;
	
	public StorableReader(InputStream stream) {
		this.reader = new OutputReader(stream);
	}
	
	public StorableReader(File file) throws FileNotFoundException {
		InputStream iStream = new FileInputStream(file);
		this.reader = new OutputReader(iStream);
	}
	
	public List<ParseData> read() throws IOException {
		SharedDataParser parser = new SharedDataParser();
		String dataString = reader.readString();
		StringReader sReader = new StringReader(dataString);
		List<ParseData> resultDatas = parser.parse(sReader);
		this.programMsgString = reader.readString();
		reader.close();
		return resultDatas;
	}
	
	public String getProgramMsg() {
		return programMsgString;
	}
 	
}

