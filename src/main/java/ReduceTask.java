import java.util.List;

public class ReduceTask implements Runnable {

    private final KeyValuePair<Character, List<Integer>> keyValuePair;
    private final KeyValuePair<Character, Float> result;
    private final int divisor;

    public ReduceTask(KeyValuePair<Character, List<Integer>> keyValuePairs, int divisor) {
        this.keyValuePair = keyValuePairs;
        this.divisor = divisor;
        this.result = new KeyValuePair<>(keyValuePairs.getKey(), 0f);
    }

    @Override
    public void run() {
        for (Integer value : keyValuePair.getValue()) {
            result.setValue(result.getValue() + value);
        }
        result.setValue((result.getValue() / divisor) * 100);
    }

    public KeyValuePair<Character, Float> getResult() {
        return result;
    }
}

