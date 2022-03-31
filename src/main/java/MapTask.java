import java.util.ArrayList;
import java.util.List;

public class MapTask implements Runnable {
    private final String line;
    private List<KeyValuePair> result;

    public MapTask(String line) {
        line = line.replaceAll("[^\\p{L}]", "");
        line = line.trim();
        this.line = line;
        result = new ArrayList<>();
    }

    @Override
    public void run() {
        for (char letter : this.line.toCharArray()) {

        }

    }
}
