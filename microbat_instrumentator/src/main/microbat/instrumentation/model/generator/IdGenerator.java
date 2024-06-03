package microbat.instrumentation.model.generator;

/**
 * 
 * @author Gabau
 *
 * @param <T> The type of the object to create id for.
 * @param <ID> The type of id created.
 */
public interface IdGenerator<T, ID> {
	public ID createId(T object);
	public ID getId(T object);
}
