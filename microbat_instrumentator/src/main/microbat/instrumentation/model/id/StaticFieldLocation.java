package microbat.instrumentation.model.id;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;

/**
 * Represents a static field location.
 * @author Gabau
 *
 */
public class StaticFieldLocation extends MemoryLocation {
	private final String className;
	private final String fieldName;
	
	public StaticFieldLocation(ParseData data) {
		className = data.getField("className").getValue();
		fieldName = data.getField("fieldName").getValue();
	}
	
	public StaticFieldLocation(String className, String fieldName) {
		this.className = className;
		this.fieldName = fieldName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(className, fieldName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StaticFieldLocation other = (StaticFieldLocation) obj;
		return Objects.equals(className, other.className) && Objects.equals(fieldName, other.fieldName);
	}

	@Override
	protected Map<String, String> store() {
		Map<String, String> result = new HashMap<>();
		result.put("className", this.className);
		result.put("fieldName", this.fieldName);
		return result;
	}
	
	
	
}
