import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

public class ReduceTask implements Callable<KeyValuePair<Character, Float>> {
    private final String inputFilePath;
    private final long divisor;
    private final char key;

    public ReduceTask(String inputFilePath, long divisor, char key) {
        this.inputFilePath = inputFilePath;
        this.divisor = divisor;
        this.key = key;
    }

    @Override
    public KeyValuePair<Character, Float> call() throws Exception {
        KeyValuePair<Character, Float> result = null;
        //File inFile = new File(inputFilePath);
        float lines = Files.lines(Paths.get(inputFilePath)).count();
        result = new KeyValuePair<>(key, lines);
//        try (FileInputStream fis = new FileInputStream(inFile)) {
//            BufferedInputStream bis = new BufferedInputStream(fis);
//            //ObjectInputStream ois = new ObjectInputStream(bis);
//            while (bis.available() > 0) {
//                int object = bis.read();// ois.readObject();
//                //if (object instanceof KeyValuePair<?, ?>) {
//                //KeyValuePair<Character, List<Integer>> parsedObject = (KeyValuePair<Character, List<Integer>>) object;
//                //for (Integer value : parsedObject.getValue()) {
//                if (result == null) {
//                    result = new KeyValuePair<>(key, (float) object);
//                } else {
//                    result.setValue(result.getValue() + (float) object);
//                }
//                //}
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        if (result != null) {
            result.setValue((result.getValue() / divisor) * 100);
        }
        return result;
    }
}

