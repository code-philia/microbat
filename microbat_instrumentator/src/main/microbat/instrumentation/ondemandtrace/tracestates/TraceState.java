package microbat.instrumentation.ondemandtrace.tracestates;

/**
 * This enum class contains trace states for on-demand trace recording.
 * 
 * (1) RECORDED: execution trace has been recorded for a code block;
 * (2) TO_RECORD: trace for a code block should be recorded in the next
 * execution round;
 * (3) execution is unrecorded & won't be recorded in the next round: won't be
 * stored in the {@code TraceStateQuerier}, thus no corresponding
 * {@code TraceState}.
 * 
 * @author HongshuW
 */
public enum TraceState {
	RECORDED, TO_RECORD
}
