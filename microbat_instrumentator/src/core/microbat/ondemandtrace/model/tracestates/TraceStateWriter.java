package microbat.ondemandtrace.model.tracestates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is responsible for storing trace states.
 * 
 * @author HongshuW
 */
public class TraceStateWriter {

	private String path;

	public TraceStateWriter(String path) {
		this.path = path;
	}

	public void writeTraceStates(Map<String, TraceState> traceStates) {
		StringBuilder content = new StringBuilder();

		for (Entry<String, TraceState> entry : traceStates.entrySet()) {
			String key = entry.getKey();
			TraceState value = entry.getValue();
			content.append(key + "," + value.toString() + "\n");
		}

		try {
			Files.write(Paths.get(path), content.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
