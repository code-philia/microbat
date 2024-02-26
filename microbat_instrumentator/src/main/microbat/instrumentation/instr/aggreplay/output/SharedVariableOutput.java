package microbat.instrumentation.instr.aggreplay.output;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.model.RecorderObjectId;
import microbat.instrumentation.model.SharedMemGeneratorInitialiser;
import microbat.instrumentation.model.SharedVariableObjectId;
import microbat.instrumentation.model.generator.ArrayIndexMemLocation;
import microbat.instrumentation.model.generator.SharedVariableArrayRef;
import microbat.instrumentation.model.generator.SharedVariableObjectGenerator;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.model.id.StaticFieldLocation;
import microbat.instrumentation.model.storage.Storable;

/**
 * Class representing the Shared variable output.
 * Data needed for shared variable analysis
 * 
 * Memory locations which are shared
 * 
 * @author Gabau
 *
 */
public class SharedVariableOutput extends Storable implements Parser<SharedVariableOutput>, SharedMemGeneratorInitialiser {
	public Set<SharedVariableObjectId> sharedObjects;
	public Set<SharedVariableArrayRef> sharedArrays;
	public Set<StaticFieldLocation> sharedStaticFields;
	public SharedVariableOutput(SharedVariableObjectGenerator objectGen) {
		sharedObjects = objectGen.getSharedVariables()
				.stream().collect(Collectors.<SharedVariableObjectId>toSet());
		sharedArrays = objectGen.getSharedArrays();
		sharedStaticFields = objectGen.getSharedStaticFields();
	}
	public SharedVariableOutput(ParseData data) {
		parse(data);
	}
	
	
	public Map<ObjectId, RecorderObjectId> getObjects() {
		Map<ObjectId, RecorderObjectId> result = new HashMap<>();
		for (SharedVariableObjectId svoId: sharedObjects) {
			RecorderObjectId fromSharedVar = new RecorderObjectId(svoId.getObjectId());
			fromSharedVar.updateSharedFieldSet(svoId.getFieldAccessList());
			result.put(fromSharedVar.getObjectId(), fromSharedVar);
		}
		return result;
		
	}

	@Override
	public SharedVariableOutput parse(ParseData data) {
		List<SharedVariableObjectId> objectIds = data.getField("sharedObjects").toList(new Function<ParseData, SharedVariableObjectId>() {
			@Override
			public SharedVariableObjectId apply(ParseData data) {
				SharedVariableObjectId object = new SharedVariableObjectId();
				object.parse(data);
				return object;
			}
		});
		this.sharedObjects = objectIds.stream().collect(Collectors.<SharedVariableObjectId>toSet());
		this.sharedArrays = data.getField("sharedArrays").toList()
				.stream()
				.map(v -> new SharedVariableArrayRef(null).parse(v))
				.collect(Collectors.toSet());
		this.sharedStaticFields = data.getField("sharedStaticFields")
				.toList()
				.stream()
				.map(v -> new StaticFieldLocation(v))
				.collect(Collectors.toSet());
		return this;
	}
	@Override
	public int hashCode() {
		return Objects.hash(sharedObjects);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SharedVariableOutput other = (SharedVariableOutput) obj;
		return Objects.equals(sharedObjects, other.sharedObjects);
	}
	@Override
	public Set<SharedMemoryLocation> getArrayRefs() {
		return this.sharedArrays.stream()
				.<ArrayIndexMemLocation>flatMap(v -> v.getSharedMemLocations().stream())
				.map(v -> new SharedMemoryLocation(v))
				.collect(Collectors.toSet());
	}
	@Override
	public Set<SharedMemoryLocation> getStaticFields() {
		return this.sharedStaticFields.stream()
				.map(v -> new SharedMemoryLocation(v)).collect(Collectors.toSet());
	}

	
	
}
