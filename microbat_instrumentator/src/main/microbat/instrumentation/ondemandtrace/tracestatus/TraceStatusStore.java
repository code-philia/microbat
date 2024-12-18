package microbat.instrumentation.ondemandtrace.tracestatus;

import java.util.HashMap;

/**
 * This class answers queries about the trace recording statuses for code
 * blocks, and updates the statuses upon requests.
 * 
 * Methods in this class will be called by instrumented code.
 * 
 * @author HongshuW
 */
public class TraceStatusStore {

	private static HashMap<String, TraceStatus> statuses = new HashMap<>();

	/* APIs */

	/**
	 * The code block has been recorded as execution trace and won't be recorded
	 * again.
	 * 
	 * @param codeBlockKey
	 * @return
	 */
	public static boolean _isRecorded(String codeBlockKey) {
		return statuses.get(codeBlockKey) == TraceStatus.RECORDED;
	}

	/**
	 * The code block hasn't been recorded and will be recorded in the next
	 * execution round.
	 * 
	 * @param codeBlockKey
	 * @return
	 */
	public static boolean _isToRecord(String codeBlockKey) {
		return statuses.get(codeBlockKey) == TraceStatus.TO_RECORD;
	}

	/**
	 * The code block hasn't been recorded and won't be recorded in the next
	 * execution round.
	 * 
	 * @param codeBlockKey
	 * @return
	 */
	public static boolean _isUnrecorded(String codeBlockKey) {
		return !statuses.containsKey(codeBlockKey);
	}

	public static void _updateStatusToRecord(String codeBlockKey) {
		if (_isUnrecorded(codeBlockKey)) {
			statuses.put(codeBlockKey, TraceStatus.TO_RECORD);
		}
	}

	public static void _updateStatusRecorded(String codeBlockKey) {
		if (_isUnrecorded(codeBlockKey) || _isToRecord(codeBlockKey)) {
			statuses.put(codeBlockKey, TraceStatus.RECORDED);
		}
	}

}
