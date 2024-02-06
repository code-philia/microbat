package microbat.instrumentation.model;

public interface Observer<Data> {
	public void onChange(Data data);
}
