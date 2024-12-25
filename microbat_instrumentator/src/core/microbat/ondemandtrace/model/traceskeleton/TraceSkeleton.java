package microbat.ondemandtrace.model.traceskeleton;

/**
 * Trace skeleton model that records the trace expansion states.
 * 
 * @author HongshuW
 */
public class TraceSkeleton {

	private TraceSkeletonNode root;

	public TraceSkeleton() {
		this.root = new TraceSkeletonNode();
	}

}
