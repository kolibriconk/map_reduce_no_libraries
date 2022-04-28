import java.io.Serializable;

public class KeyValuePair<T,R> implements Serializable {
    private final T key;
    private R value;

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

    public void setValue(R value) {
        this.value = value;
    }

    public boolean equals(Object letter) {
        return this.key == letter;
    }

}
