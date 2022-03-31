public class KeyValuePair {
    private final char key;
    private int value;

    public KeyValuePair(char value) {
        this.key = value;
        this.value = 1;
    }

    public char getKey() {
        return key;
    }

    public int getValue() {
        return value;
    }

    public int getAsciiKey(){
        return key;
    }

    public void addValue() {
        this.value++;
    }
}
