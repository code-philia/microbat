package microbat.instrumentation.instr.aggreplay.shared;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import microbat.instrumentation.instr.aggreplay.output.SharedVariableOutput;
import microbat.instrumentation.model.RecorderObjectId;
import microbat.instrumentation.model.generator.ObjectIdGenerator;
import microbat.instrumentation.model.id.Event;
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
	public ReadWriteAccessList rwAccessList; // WR_var(e)
	// used to get the object acquisition
	public List<ThreadId> threadIds;
	public List<SharedMemoryLocation> sharedMemoryLocations; // W_var(e)
	public Map<ObjectId, List<Event>> lockAcquisitionMap;
	public SharedVariableOutput sharedVariableOutput;
	public RecordingOutput(ReadWriteAccessList rwAccessList,
			List<ThreadId> threadIds,
			List<SharedMemoryLocation> sharedMemoryLocations,
			Map<ObjectId, List<Event>> lockAcquisitionMap,
			SharedVariableOutput sharedVariableOutput) {
		super();
		this.rwAccessList = rwAccessList;
		// TODO(Gab): filter out the objects that aren't used.
		this.sharedMemoryLocations = sharedMemoryLocations;
		this.threadIds = threadIds;
		this.lockAcquisitionMap = lockAcquisitionMap;
		this.sharedVariableOutput = sharedVariableOutput;
	}
	
	public RecordingOutput() {
		
	}
	
	public SharedVariableOutput getSharedVariables() {	
		return sharedVariableOutput;
	}
	public Map<ObjectId, List<Event>> getLockAcquisitionMap() {
		return lockAcquisitionMap;
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
		threadIds = parseThreadIds(parseData.getField("threadIds"));
		this.lockAcquisitionMap = parseData.getField("lockAcquisitionMap")
				.toMap(new Function<ParseData, ObjectId>() {
			@Override
			public ObjectId apply(ParseData data) {
				ObjectId objectId = new ObjectId(false);
				objectId.parse(data);
				return objectId;
			}
		}, new Function<ParseData, List<Event>>() {
			@Override
			public List<Event> apply(ParseData data) {
				return data.toList(new Function<ParseData, Event>() {
					@Override
					public Event apply(ParseData parseData) {
						Event result = Event.parseEvent(parseData);
						return result;
					}
				});
			}
		});
		
		this.sharedMemoryLocations = 
				parseData.getField("sharedMemoryLocations").toList(new Function<ParseData, SharedMemoryLocation>() {

					@Override
					public SharedMemoryLocation apply(ParseData t) {
						return new SharedMemoryLocation().parse(t);
					}
					
				});
		this.sharedVariableOutput = 
				new SharedVariableOutput(parseData.getField("sharedVariableOutput"));
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.lockAcquisitionMap, rwAccessList, sharedMemoryLocations, threadIds);
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
		return Objects.equals(this.lockAcquisitionMap, other.lockAcquisitionMap)
				&& Objects.equals(rwAccessList, other.rwAccessList)
				&& Objects.equals(sharedMemoryLocations, other.sharedMemoryLocations)
				&& Objects.equals(threadIds, other.threadIds);
	}
	
}
