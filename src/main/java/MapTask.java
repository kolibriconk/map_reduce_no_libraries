import java.util.ArrayList;
import java.util.List;

public class MapTask implements Runnable {
    private final String line;
    private List<KeyValuePair<Character, Integer>> result;
    private final int count;

    public MapTask(String line) {
        line = line.replaceAll("[^\\p{L}]", "");
        line = line.trim();
        this.line = line;
        this.result = new ArrayList<>();
        this.count = line.length();
    }

    @Override
    public void run() {
        for (char letter : this.line.toCharArray()) {
            result.add(new KeyValuePair<>(letter, 1));
        }
    }

    public List<KeyValuePair<Character, Integer>> getResult() {
        return result;
    }

    public int getCount() {
        return count;
    }
}
