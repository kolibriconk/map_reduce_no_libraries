import java.util.List;
import java.util.concurrent.Callable;

public class AddReduceTask implements Callable<KeyValuePair<Character, Float>> {

    private final KeyValuePair<Character, List<Float>> input;

    public AddReduceTask(KeyValuePair<Character, List<Float>> keyValuePairs) {
        this.input = keyValuePairs;
    }

    @Override
    public KeyValuePair<Character, Float> call() throws Exception {
        KeyValuePair<Character, Float> result = new KeyValuePair<>(input.getKey(), 0f);
        for (Float value : input.getValue()) {
            result.setValue(result.getValue() + value);
        }

        return result;
    }
}

