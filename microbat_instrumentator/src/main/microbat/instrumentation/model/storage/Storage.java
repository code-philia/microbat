package microbat.instrumentation.model.storage;

import java.util.HashSet;

import microbat.instrumentation.model.id.MemoryLocation;

public interface Storage {
	public static String itemDelimString = ",";
	public void store(HashSet<Storable> objects);
}
