import java.util.List;

public class AddAndDivideReduceTask extends AddReduceTask {

    private final int divisor;

    public AddAndDivideReduceTask(KeyValuePair<Character, List<Float>> keyValuePairs, int divisor) {
        super(keyValuePairs);
        this.divisor = divisor;
    }

    @Override
    public KeyValuePair<Character, Float> call() throws Exception {
        KeyValuePair<Character, Float> result = super.call();
        result.setValue(result.getValue() / divisor);
        return result;
    }


}
