package microbat.model;

import microbat.model.trace.ConcurrentTrace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import sav.common.core.Pair;

public class ConcNode {
	private int node1;
	// after
	private int node2;
	// node1 is before
	private boolean isBefore1;
	// node2 isbefore
	private boolean isBefore2;
	
	private int changeType;
	
	private VarValue linkedVarValue = null;
	
	public VarValue getLinkedValue() {
		return linkedVarValue;
	}
	
	public int getChangeType() {
		return this.changeType;
	}
	public ConcNode(int node1, int node2, boolean isBefore1, boolean isBefore2, int changeType) {
		this.node1 = node1;
		this.node2 = node2;
		this.isBefore1 = isBefore1;
		this.isBefore2 = isBefore2;
		this.changeType = changeType;
	}
	public int getNode1() {
		return node1;
	}
	public int getNode2() {
		return node2;
	}
	public boolean isBefore1() {
		return isBefore1;
	}
	public boolean isBefore2() {
		return isBefore2;
	}
	
	public Pair<Integer, Boolean> getFirst() {
		return Pair.of(node1, isBefore1);
	}
	
	public Pair<Integer, Boolean> getSecond() {
		return Pair.of(node2, isBefore2);
	}

	
	public static ConcNode fromTraceNodes(TraceNode node1, TraceNode node2, boolean isBefore1, boolean isBefore2,
			int changeType) {
		return new ConcNode(node1.getBound().getOrder(), node2.getBound().getOrder(), isBefore1, isBefore2, changeType);
	}
}
