import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public record ReduceTask(String inputFilePath
        , long divisor, char key) implements Callable<KeyValuePair<Character, Float>> {

    @Override
    public KeyValuePair<Character, Float> call() throws Exception {
        float lines = Files.lines(Paths.get(inputFilePath)).count();

        return new KeyValuePair<>(key, (lines / divisor) * 100);
    }
}

