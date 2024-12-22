package microbat.ondemandtrace.model.tracestates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for reading and parsing stored trace states.
 * 
 * @author HongshuW
 */
public class TraceStateReader {

	private String path;

	public TraceStateReader(String path) {
		this.path = path;
	}

	public Map<String, TraceState> parseTraceStates() {
		Map<String, TraceState> traceStates = new HashMap<>();

		File file = new File(path);
		try {
			if (file.exists()) {
				List<String> lines = Files.readAllLines(Paths.get(path));
				for (String entry : lines) {
					if (entry != null) {
						String[] keyValPair = entry.split(",");
						String key = keyValPair[0];
						String val = keyValPair[1];
						traceStates.put(key, TraceState.valueOf(val));
					}
				}
			} else {
				file.createNewFile();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return traceStates;
	}

}
