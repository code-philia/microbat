package microbat.instrumentation.model.id;

import java.util.Objects;

import microbat.instrumentation.model.storage.Storable;

public class ThreadId implements Storable {
	
	private static class ListNode {
		long value;
		ListNode parent;
		public ListNode(long value, ListNode parent) {
			this.value = value;
			this.parent = parent;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ListNode other = (ListNode) obj;
			return value == other.value && Objects.equals(parent, other.parent);
		}
		
		
	}
	private ListNode rootListNode = null;
	private long idCounter = 0;
	private long threadId;

	public int internalHashCode = 100002301;
	
	public ThreadId(long threadId) {
		rootListNode = new ListNode(0, null);
		precomputeHashCode();
		this.threadId = threadId;
	}
	
	private ThreadId(ListNode rootListNode) {
		this.rootListNode = rootListNode;
		precomputeHashCode();
	}
	
	private void precomputeHashCode() {
		ListNode temp = rootListNode;
		while (temp != null) {
			int k = Long.hashCode(temp.value);
			k = k & (k >>> 32);
			internalHashCode = 37 * internalHashCode + k;
			temp = temp.parent;
		}
	}
	
	public ThreadId createChildWithThread(long threadId) {
		long value = idCounter++;
		ThreadId childId = createChild(value);
		childId.threadId = threadId;
		return childId;
	}
	
	
	
	public ThreadId createChild(long value) {
		ListNode rootListNode = new ListNode(value, this.rootListNode);
		return new ThreadId(rootListNode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(internalHashCode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ThreadId other = (ThreadId) obj;
		return internalHashCode == other.internalHashCode
				&& other.rootListNode.equals(this.rootListNode);
	}

	@Override
	public String store() {
		StringBuilder resultBuilder = new StringBuilder();
		ListNode currListNode = rootListNode;
		resultBuilder.append("ThreadId=");
		resultBuilder.append(this.threadId);
		resultBuilder.append(Storable.STORE_DELIM_STRING);
		while (currListNode != null) {
			resultBuilder.append(currListNode.value);
			if(currListNode.parent == null) break; 
			resultBuilder.append(Storable.STORE_DELIM_STRING);
			currListNode = currListNode.parent;
		}
		return resultBuilder.toString();
	}
}
