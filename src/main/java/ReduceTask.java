import java.util.List;

public class ReduceTask implements Runnable {

    private List<KeyValuePair<Character, Integer>> keyValuePairs;
    private KeyValuePair<Character, Float> result;
    private final int divisor;

    public ReduceTask(List<KeyValuePair<Character, Integer>> keyValuePairs, int divisor) {
        this.keyValuePairs = keyValuePairs;
        this.divisor = divisor;
        this.result = new KeyValuePair<>(keyValuePairs.get(0).getKey(), 0f);
    }

    @Override
    public void run() {
        for (KeyValuePair<Character, Integer> keyValuePair : keyValuePairs) {
            result.setValue(result.getValue() + keyValuePair.getValue());
        }

        result.setValue((result.getValue() / divisor) * 100);
    }

    public KeyValuePair<Character, Float> getResult() {
        return result;
    }
}

