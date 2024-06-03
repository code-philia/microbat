package microbat.instrumentation.model.id;

public class ArrayIndexLocation extends MemoryLocation {
	private Object arrayRef;
	private final int index;
	public ArrayIndexLocation(Object arrayRef, int index) {
		this.arrayRef = arrayRef;
		this.index = index;
	}
	
}
