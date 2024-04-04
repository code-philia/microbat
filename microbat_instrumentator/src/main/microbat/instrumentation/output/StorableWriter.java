package microbat.instrumentation.output;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import microbat.instrumentation.Agent;
import microbat.instrumentation.model.storage.Storable;

public class StorableWriter {
	private OutputWriter outputWriter;
	
	public StorableWriter(OutputStream outputStream) {
		this.outputWriter = new OutputWriter(outputStream);
	}
	
	public StorableWriter(File file) throws FileNotFoundException {
		OutputStream oStream = new FileOutputStream(file);
		this.outputWriter = new OutputWriter(oStream);
	}
	
	public void writeStorable(Storable storable) throws IOException {
		String resultString = storable.getFromStore();
		outputWriter.writeString(resultString);
		outputWriter.writeString(Agent.getProgramMsg());
		outputWriter.flush();
		outputWriter.close();
	}
	
	
}
