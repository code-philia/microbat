package microbat.instrumentation.instr.aggreplay.output;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import microbat.instrumentation.model.SharedVariableObjectId;
import microbat.instrumentation.model.generator.SharedVariableObjectGenerator;
import microbat.instrumentation.model.id.ObjectId;
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
public class SharedVariableOutput extends Storable implements Parser<SharedVariableOutput> {
	public Set<SharedVariableObjectId> sharedObjects;
	public SharedVariableOutput(SharedVariableObjectGenerator objectGen) {
		sharedObjects = objectGen.getSharedVariables().stream().collect(Collectors.<SharedVariableObjectId>toSet());
	}
	public SharedVariableOutput(ParseData data) {
		parse(data);
	}
	
	
	public Map<ObjectId, RecorderObjectId> getObjects() {
		Map<ObjectId, RecorderObjectId> result = new HashMap<>();
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
		return this;
		
	}

}
