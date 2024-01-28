package microbat.instrumentation.instr.aggreplay.shared;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.ReadCountVector;
import microbat.instrumentation.model.id.ReadWriteAccessList;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.storage.Storable;

/**
 * Class that represents the recording output for aggreplay recording.
 * Stages involved -> Shared var detection -> recording -> replay
 * 
 * @author Gabau
 *
 */
public class RecordingOutput extends Storable implements Parser<RecordingOutput> {
	public ReadCountVector readCountVector;
	public ReadWriteAccessList rwAccessList;
	// used to get the object acquisition
	public List<ObjectId> objectsHashSet;
	public List<ThreadId> threadIds;
	public List<SharedMemoryLocation> sharedMemoryLocations;
	public RecordingOutput(ReadCountVector readCountVector, ReadWriteAccessList rwAccessList,
			List<ThreadId> threadIds,
			List<ObjectId> objectsHashSet, List<SharedMemoryLocation> sharedMemoryLocations) {
		super();
		this.readCountVector = readCountVector;
		this.rwAccessList = rwAccessList;
		this.objectsHashSet = objectsHashSet;
		this.sharedMemoryLocations = sharedMemoryLocations;
		this.threadIds = threadIds;
	}
	
	public RecordingOutput() {
		
	}
	
	public static List<ThreadId> parseThreadIds(ParseData parseData) {
		List<ParseData> values = parseData.toList();
		return values.stream().map(new Function<ParseData, ThreadId>() {
			@Override
			public ThreadId apply(ParseData pData) {
				return new SharedDataParser().createThreadId(pData);
			}
		}).collect(Collectors.<ThreadId>toList());
	}
	
	// TODO: 
	public RecordingOutput parse(ParseData parseData) {
		rwAccessList = new ReadWriteAccessList().parse(parseData.getField("rwAccessList"));
		readCountVector = new ReadCountVector().parse(parseData.getField("readCountVector"));
		threadIds = parseThreadIds(parseData.getField("threadIds"));
		objectsHashSet = parseData.toList(new Function<ParseData, ObjectId>() {
			@Override
			public ObjectId apply(ParseData parseData) {
				return new ObjectIdParser().parse(parseData);
			}
		});
		this.sharedMemoryLocations = 
				parseData.toList(new Function<ParseData, SharedMemoryLocation>() {

					@Override
					public SharedMemoryLocation apply(ParseData t) {
						return new SharedMemoryLocation().parse(t);
					}
					
				});
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectsHashSet, readCountVector, rwAccessList, sharedMemoryLocations, threadIds);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RecordingOutput other = (RecordingOutput) obj;
		return Objects.equals(objectsHashSet, other.objectsHashSet)
				&& Objects.equals(readCountVector, other.readCountVector)
				&& Objects.equals(rwAccessList, other.rwAccessList)
				&& Objects.equals(sharedMemoryLocations, other.sharedMemoryLocations)
				&& Objects.equals(threadIds, other.threadIds);
	}
	
}
