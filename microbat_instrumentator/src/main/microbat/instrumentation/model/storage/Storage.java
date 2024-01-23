package microbat.instrumentation.model.storage;

import java.util.HashSet;

import microbat.instrumentation.model.id.MemoryLocation;

public interface Storage {
	public static String itemDelimString = ",";
	String STORE_DELIM_STRING = ":";
	public static final String START_OBJECT_STRING = "{";
	public static final String CLOSE_OBJECT_STRING = "}";
	public static final String OBJECT_SEPARATOR = ",";
	public void store(HashSet<Storable> objects);
}
