package microbat.instrumentation.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

import microbat.instrumentation.model.id.MemoryLocation;
import microbat.instrumentation.model.id.ReadCountVector;
import microbat.instrumentation.model.id.ReadWriteAccessList;
import microbat.instrumentation.model.id.ReadWriteAccessList.Node;
import microbat.instrumentation.model.id.SharedMemoryLocation;

/**
 * Wrapper class used for generating the RC vector
 * for each thread accounting for the reduced recording
 * due to optimisations
 * 
 * Represents EX_t in aggreplay.
 * @author Gabau
 *
 */
public class ReadWriteAccessListReplay {
	
	// map from recorded threadId to RC vector for thread id
	// with the current state
	private Map<Long, Map<MemoryLocation, Map<Long, Integer>>> 
		generatedMap = new HashMap<>();
	private Map<Long, Stack<Node>> changeStack = new HashMap<>();
	public ReadWriteAccessListReplay(ReadWriteAccessList rwal) {
		Map<Long, LinkedList<Node>> values = rwal.getList();
		for (Map.Entry<Long, LinkedList<Node>> value: values.entrySet()) {	
			changeStack.put(value.getKey(), fromList(value.getValue()));
		}
	}
	/**
	 * Returns true if the RC vector for each memory location is greater or equal
	 * to the reference
	 * @param threadMap The current rc vector map
	 * @param threadId The thread id in which this access list was built on
	 * @param threadIdMap The thread id map from current thread id to previous
	 * @return
	 */
	public boolean checkRead(ReadCountVector currentVector,
			long threadId,
			Map<Long, Long> threadIdMap) {
		
		Map<MemoryLocation, Map<Long, Integer>> threadMap = currentVector.getRCVector(threadId);
		for (Map.Entry<MemoryLocation, Map<Long, Integer>> entry : threadMap.entrySet()) {
			Map<Long, Integer> reference = entry.getValue();
			Map<Long, Integer> current = generatedMap.get(threadId)
					.getOrDefault(entry.getValue(), new HashMap<Long, Integer>());
			if (!compareMaps(reference, current, threadIdMap)) {
				return false;
			}
		}
		pop(threadId);
		return true;
	}
	
	/**
	 * Checks that none of the current RC vectors is less than the reference RC vector
	 * 
	 * @param reference The referfence RC vector
	 * @param current The current RC vector
	 * @param threadIdMap The mapping from current thread id's to previous id's
	 * @return
	 */
	private boolean compareMaps(Map<Long, Integer> reference, 
			Map<Long, Integer> current, 
			Map<Long, Long> threadIdMap) {
		for (Map.Entry<Long, Integer> entry : current.entrySet()) {
			long previousThreadId = threadIdMap.get(entry.getKey());
			if (entry.getValue() < reference.get(previousThreadId)) {
				return false;
			}
		}
		return  true;
	}
	
	private Stack<Node> fromList(LinkedList<Node> values) {
		final Stack<Node> result = new Stack<>();
		values.descendingIterator().forEachRemaining(new Consumer<Node>() {
			@Override
			public void accept(Node val) {
				result.push(val);
			}
		});
		return result;
	}
	
	private void assertThreadId(long threadId) {
		if (!generatedMap.containsKey(threadId)) {
			synchronized (generatedMap) {
				if (!generatedMap.containsKey(threadId)) generatedMap.put(threadId, 
						new HashMap<MemoryLocation, Map<Long, Integer>>());
			}
		}
	}

	/**
	 * Obtain the top 
	 * 
	 * @param threadId The recorded thread id.
	 * @return
	 */
	public int top(long threadId, long index, SharedMemoryLocation shm) {
		assertThreadId(threadId);
		return generatedMap.get(threadId).get(shm.getLocation()).get(index);
	}
	
	public void pop(long threadId) {
		assertThreadId(threadId);
		Node change = changeStack.get(threadId).peek();
		changeStack.get(threadId).pop();
		generatedMap.get(threadId).put(change.getLocation(), change.getRcMap());
	}
}
