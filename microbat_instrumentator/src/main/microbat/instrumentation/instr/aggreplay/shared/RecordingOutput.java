package microbat.instrumentation.instr.aggreplay.shared;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import microbat.instrumentation.instr.aggreplay.output.SharedVariableOutput;
import microbat.instrumentation.model.RecorderObjectId;
import microbat.instrumentation.model.SharedMemGeneratorInitialiser;
import microbat.instrumentation.model.generator.ArrayIndexMemLocation;
import microbat.instrumentation.model.generator.ObjectIdGenerator;
import microbat.instrumentation.model.generator.SharedVariableArrayRef;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.MemoryLocation;
import microbat.instrumentation.model.id.ObjectFieldMemoryLocation;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.ReadCountVector;
import microbat.instrumentation.model.id.ReadWriteAccessList;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.model.id.StaticFieldLocation;
import microbat.instrumentation.model.id.ThreadId;
import microbat.instrumentation.model.storage.Storable;

/**
 * Class that represents the recording output for aggreplay recording.
 * Stages involved -> Shared var detection -> recording -> replay
 * 
 * @author Gabau
 *
 */
public class RecordingOutput extends Storable implements Parser<RecordingOutput>, SharedMemGeneratorInitialiser {
	public ReadWriteAccessList rwAccessList; // WR_var(e)
	// used to get the object acquisition
	public List<ThreadId> threadIds;
	public List<SharedMemoryLocation> sharedMemoryLocations; // W_var(e)
	public Map<ObjectId, List<Event>> lockAcquisitionMap;
	public RecordingOutput(ReadWriteAccessList rwAccessList,
			List<ThreadId> threadIds,
			List<SharedMemoryLocation> sharedMemoryLocations,
			Map<ObjectId, List<Event>> lockAcquisitionMap) {
		super();
		this.rwAccessList = rwAccessList;
		// TODO(Gab): filter out the objects that aren't used.
		this.sharedMemoryLocations = sharedMemoryLocations;
		this.threadIds = threadIds;
		this.lockAcquisitionMap = lockAcquisitionMap;
	}
	
	public RecordingOutput() {
		
	}
	public Map<ObjectId, List<Event>> getLockAcquisitionMap() {
		return lockAcquisitionMap;
	}
	
	// used for shared mem locations
	@Override
	public Map<ObjectId, RecorderObjectId> getObjects() {
		Map<ObjectId, RecorderObjectId> result = new HashMap<>();
		for (SharedMemoryLocation shMemoryLocation : this.sharedMemoryLocations) {
			if (shMemoryLocation.isSharedObjectMem()) {
				ObjectId objectId = shMemoryLocation.getLocation().getObjectId();
				if (!result.containsKey(objectId)) {
					result.put(objectId, new RecorderObjectId(objectId));
				}
				RecorderObjectId toObtainRObjectId = result.get(objectId);
				ObjectFieldMemoryLocation ofml = (ObjectFieldMemoryLocation) shMemoryLocation.getLocation();
				toObtainRObjectId.setField(ofml.getField(), shMemoryLocation);
			}
		}
		
		return result;
	}
	
	
	public static List<ThreadId> parseThreadIds(ParseData parseData) {
		List<ParseData> values = parseData.toList();
		return values.stream().map(new Function<ParseData, ThreadId>() {
			@Override
			public ThreadId apply(ParseData pData) {
				return SharedDataParser.createThreadId(pData);
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
	
		for (SharedMemoryLocation sml : this.sharedMemoryLocations) {
			sml.generateWRMap();
		}
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

	@Override
	public Set<SharedMemoryLocation> getArrayRefs() {
		return this.sharedMemoryLocations
				.stream()
				.filter((v) -> v.getLocation() instanceof ArrayIndexMemLocation).collect(Collectors.<SharedMemoryLocation>toSet());
	}

	@Override
	public Set<SharedMemoryLocation> getStaticFields() {
		Set<SharedMemoryLocation> locations = new HashSet<>();
		for (SharedMemoryLocation sml: this.sharedMemoryLocations) {
			if (sml.getLocation() instanceof StaticFieldLocation) {
				locations.add(sml);
			}
		}
		return locations;
	}
	
}
