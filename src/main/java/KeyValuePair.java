import java.io.Serializable;

/**
 * A class that represents a key-value pair of dynamic objects.
 *
 * @param <T> The type of the key.
 * @param <R> The type of the value.
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

    public boolean equals(Object letter) {
        return this.key == letter;
    }

}
