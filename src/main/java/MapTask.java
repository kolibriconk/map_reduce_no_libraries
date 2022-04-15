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
            result.add(new KeyValuePair(letter));
            System.out.printf("For the thread num: %s Adding new letter: %s\n"
                    , Thread.currentThread().getName() ,letter);


            /*boolean found = false;
            for (KeyValuePair pair : result) {
                if (pair.equals(letter)) {
                    pair.increment();
                    System.out.printf("For the thread num: %s Incrementing letter: %s total: %d\n"
                            ,Thread.currentThread().getName(), letter, pair.getValue());
                }
            }
            if (!found){
                result.add(new KeyValuePair(letter));
                System.out.printf("For the thread num: %s Adding new letter: %s\n"
                        , Thread.currentThread().getName() ,letter);
            }*/
        }
    }
}
