package microbat.ondemandtrace.model.traceskeleton;

import java.util.ArrayList;
import java.util.List;

import microbat.ondemandtrace.model.tracestates.TraceState;

/**
 * Each node represents a code block.
 * 
 * @author HongshuW
 */
public class TraceSkeletonNode {

	private String codeBlockKey;
	private TraceState traceState;
	private TraceSkeletonNode parent;
	private List<TraceSkeletonNode> children;

	public TraceSkeletonNode() {
		this.codeBlockKey = null;
		this.traceState = TraceState.UNRECORDED;
		this.parent = null;
		this.children = new ArrayList<>();
	}

	public TraceSkeletonNode(String codeBlockKey) {
		this();
		this.codeBlockKey = codeBlockKey;
	}

	public boolean isRoot() {
		return this.parent == null && this.codeBlockKey == null;
	}

	public void addChild(TraceSkeletonNode child) {
		this.children.add(child);
		child.setParent(this);
	}

	public void setParent(TraceSkeletonNode parent) {
		this.parent = parent;
	}

	public TraceState getTraceState() {
		return this.traceState;
	}

	public void setTraceState(TraceState traceState) {
		this.traceState = traceState;
	}

}
