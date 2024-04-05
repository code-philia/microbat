package microbat.instrumentation.model.id;

import java.util.Objects;

import microbat.instrumentation.model.storage.Storable;

public class ThreadId extends Storable {
	

	public ListNode rootListNode = null;
	public long threadId;
	// keeps track of the order of the trace node which spawned this node.
	public int spawnOrder = -1;
	private long idCounter = 0;
	
	public void setSpawnOrder(Integer spawnOrder) {
		this.spawnOrder = spawnOrder;
	}
	
	public ThreadId getParent() {
		if (rootListNode == null) {
			return null;
		}
		return new ThreadId(rootListNode.parent);
	}
	
	public Integer getSpawnOrder() {
		return this.spawnOrder;
	}

	public int internalHashCode = 100002301;
	
	public String printRootListNode() {
		return rootListNode.getFromStore();
	}
	
	private static class ListNode extends Storable {
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
		@Override
		public String getFromStore() {
			StringBuilder result = new StringBuilder();
			ListNode temp = this;
			while (temp != null) {
				result.append(temp.value);
				result.append(";");
				temp = temp.parent;
			}
			return result.toString();
		}
		
		
	}
	
	public ThreadId(long threadId) {
		rootListNode = new ListNode(0, null);
		precomputeHashCode();
		this.threadId = threadId;
	}
	
	private ThreadId(ListNode rootListNode) {
		this.rootListNode = rootListNode;
		precomputeHashCode();
	}
	
	private ThreadId(int internalHashCode, String threadId, long threadValue) {
		this.internalHashCode = internalHashCode;
		this.rootListNode = fromString(threadId);
		this.threadId = threadValue;
	}
	
	public static ListNode fromString(String hashedId) {
 		String[] splitValues = hashedId.split(";");
 		ListNode result = new ListNode(Integer.parseInt(splitValues[splitValues.length - 1]), null);
 		for (int i = splitValues.length - 2; i >= 0; i--) {
			int value = Integer.parseInt(splitValues[i]);
			result = new ListNode(value, result);
		}
		return result;
	}
	
	/**
	 * Creates a thread with the provided thread
	 * @param internalHashCode The generated hash code of the thread.
	 * @param threadId The replicated id of the thread.
	 * @param threadValue The id {@code long} of the thread. TODO(Gab): Not ideal to store in this class
	 * @return
	 */
	public static ThreadId createThread(int internalHashCode, String threadId, long threadValue) {
		return new ThreadId(internalHashCode, threadId, threadValue);
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
	
	/**
	 * Not synchronised, because this should be called in the parent thread.
	 * @param threadId
	 * @return
	 */
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

	public long getId() {
		return this.threadId;
	}

}
