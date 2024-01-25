package microbat.instrumentation.instr.aggreplay.shared;

public interface Parser<T> {
	public T parse(ParseData data);
}
