package microbat.ondemandtrace.model.tracestates;

import java.util.HashMap;
import java.util.Map;

/**
 * This class answers queries about the trace recording statuses for code
 * blocks, and updates the statuses upon requests.
 * 
 * APIs in this class will be called by instrumented code.
 * 
 * @author HongshuW
 */
public class TraceStateQuerier {

	private static Map<String, TraceState> traceStates = new HashMap<>();
	private static String path;

	public static void init(String path) {
		TraceStateQuerier.path = path;

		TraceStateReader reader = new TraceStateReader(path);
		Map<String, TraceState> traceStates = reader.parseTraceStates();
		TraceStateQuerier.traceStates = traceStates;
	}

	public static boolean hasNoRecordedTraceStates() {
		return TraceStateQuerier.traceStates.isEmpty();
	}

	/* APIs */

	/**
	 * The code block has been recorded as execution trace and won't be recorded
	 * again.
	 * 
	 * @param codeBlockKey
	 * @return
	 */
	public static boolean _isRecorded(String codeBlockKey) {
		return traceStates.get(codeBlockKey) == TraceState.RECORDED;
	}

	/**
	 * The code block hasn't been recorded and will be recorded in the next
	 * execution round.
	 * 
	 * @param codeBlockKey
	 * @return
	 */
	public static boolean _isToRecord(String codeBlockKey) {
		return traceStates.get(codeBlockKey) == TraceState.TO_RECORD;
	}

	/**
	 * The code block hasn't been recorded and won't be recorded in the next
	 * execution round.
	 * 
	 * @param codeBlockKey
	 * @return
	 */
	public static boolean _isUnrecorded(String codeBlockKey) {
		return !traceStates.containsKey(codeBlockKey) || traceStates.get(codeBlockKey) == TraceState.UNRECORDED;
	}

	public static void _updateStatusToRecord(String codeBlockKey) {
		if (_isUnrecorded(codeBlockKey)) {
			traceStates.put(codeBlockKey, TraceState.TO_RECORD);
		}

		TraceStateWriter writer = new TraceStateWriter(path);
		writer.writeTraceStates(traceStates);
	}

	public static void _updateStatusRecorded(String codeBlockKey) {
		if (_isUnrecorded(codeBlockKey) || _isToRecord(codeBlockKey)) {
			traceStates.put(codeBlockKey, TraceState.RECORDED);
		}

		TraceStateWriter writer = new TraceStateWriter(path);
		writer.writeTraceStates(traceStates);
	}

}
