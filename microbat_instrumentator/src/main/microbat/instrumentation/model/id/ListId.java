package microbat.instrumentation.model.id;

import java.util.Objects;

public class ListId {
	
	private static class ListNode {
		long value;
		ListNode parent;
		public ListNode(long value, ListNode parent) {
			this.value = value;
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

	public int internalHashCode = 100002301;
	
	public ListId() {
		rootListNode = new ListNode(0, null);
		precomputeHashCode();
	}
	
	private ListId(ListNode rootListNode) {
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
	
	public ListId createChild() {
		long value = idCounter++;
		return createChild(value);
	}
	
	public ListId createChild(long value) {
		ListNode rootListNode = new ListNode(value, this.rootListNode);
		return new ListId(rootListNode);
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
		ListId other = (ListId) obj;
		return internalHashCode == other.internalHashCode
				&& other.rootListNode.equals(this.rootListNode);
	}
}
