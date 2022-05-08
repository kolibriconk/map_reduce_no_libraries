import java.io.Serializable;

/**
 * @param <T> The type of the key.
 * @param <R> The type of the value.
 * @author Jose Antonio Ramos Andrades - 1565479
 * @author Victor Sancho Aguilera - 1529721
 * A class that represents a key-value pair of dynamic objects.
 */
public class KeyValuePair<T, R> implements Serializable {
    private final T key;
    private final R value;

    public KeyValuePair(T value, R count) {
        this.key = value;
        this.value = count;
    }

    public T getKey() {
        return key;
    }

    public R getValue() {
        return value;
    }
}
