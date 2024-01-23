package microbat.instrumentation.model.storage;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public abstract class Storable {

	public static String fromObject(Object object) {
		if (object instanceof Storable) {
			return ((Storable) object).getFromStore();
		}
		return object.toString();
	}
	
	/**
	 * The guarantees of a storable.
	 * Object is enclosed in { and }
	 * 
	 * 
	 * @return
	 */
	protected Map<String, String> store() {
		HashMap<String, String> fieldMap = new HashMap<String, String>();
		Field[] fields = getClass().getFields();
		for (Field f : fields) {
			if (Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			try {
				Object value = f.get(this);
				if (value == null) {
					continue;
				}
				if (value instanceof Storable) {
					fieldMap.put(f.getName(), ((Storable) value).getFromStore());
				} else {
					fieldMap.put(f.getName(), value.toString());
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return fieldMap;
	}
	
	public String getObjectType() {
		return this.getClass().getName();
	}
	
	// guarantees an object type
	public String getFromStore() {
		StringBuilder builder = new StringBuilder();
		builder.append(Storage.START_OBJECT_STRING);
		builder.append('\n');
		builder.append("ObjectType:");
		builder.append(getObjectType());
		builder.append(Storage.OBJECT_SEPARATOR);
		builder.append('\n');
		for (Map.Entry<String, String> entry : store().entrySet()) {
			builder.append(entry.getKey());
			builder.append(Storage.STORE_DELIM_STRING);
			builder.append(entry.getValue());
			builder.append(Storage.OBJECT_SEPARATOR);
			builder.append('\n');
		}
		builder.append(Storage.CLOSE_OBJECT_STRING);
		return builder.toString();
	}
	
}
