package microbat.instrumentation.model;

/**
 * 
 * @author Gabau
 *
 * @param <T> The object containing the change information
 */
public interface Observable<T> {
	public void onChange(T data);
}
