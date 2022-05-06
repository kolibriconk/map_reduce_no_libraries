import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Record that represents a task where it gets the input file
 * path and reduces it to a single result being a key-value pair.
 * It implements the Callable interface returning a key-value pair
 * of the character and the percent of occurrence.
 *
 * @param inputFilePath
 * @param divisor
 * @param key
 */
public record ReduceTask(String inputFilePath
        , long divisor, char key) implements Callable<KeyValuePair<Character, Float>> {

    @Override
    public KeyValuePair<Character, Float> call() {
        float lines = 0;
        try {
            lines = Files.lines(Paths.get(inputFilePath)).count();
        } catch (Exception ignored) {
        }

        return new KeyValuePair<>(key, (lines / divisor) * 100);
    }
}

