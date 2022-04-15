import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {
    public static void main(String[] args) {
        for (String arg : args) {
            BufferedReader br = null;
            try {
                List<MapTask> threads = new ArrayList<>();
                ExecutorService es = Executors.newCachedThreadPool();
                int totalLetters = 0;
                //Input of the program
                System.out.println("Reading file " + arg);
                br = new BufferedReader(new FileReader(arg));
                String line;
                while ((line = br.readLine()) != null) {
                    //Executing the map phase
                    MapTask mapTask = new MapTask(line);
                    threads.add(mapTask);
                    es.execute(mapTask);
                }
                es.shutdown();
                if (es.awaitTermination(1, TimeUnit.MINUTES)) {
                    List<KeyValuePair<Character, Integer>> result = new ArrayList<>();
                    for (MapTask mapTask : threads) {
                        result.addAll(mapTask.getResult());
                        totalLetters += mapTask.getCount();
                    }
                    List<List<KeyValuePair<Character, Integer>>> shuffledList = shuffle(result);
                    reduce(shuffledList, totalLetters);
                }

                System.out.println("Finished reading file " + arg);
            } catch (IOException e) {
                System.out.println("File not found, skipping");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        System.out.println("Cannot close the buffered reader");
                    }
                }
            }
        }
    }

    public static List<List<KeyValuePair<Character, Integer>>> shuffle(List<KeyValuePair<Character, Integer>> result) {
        List<List<KeyValuePair<Character, Integer>>> separatedKeyValuePairs = new ArrayList<>();
        List<Character> alreadyAdded = new ArrayList<>();
        for (KeyValuePair<Character, Integer> kvp : result) {
            if (alreadyAdded.contains(kvp.getKey())) {
                separatedKeyValuePairs.get(alreadyAdded.indexOf(kvp.getKey())).add(kvp);
            } else {
                alreadyAdded.add(kvp.getKey());
                List<KeyValuePair<Character, Integer>> temp = new ArrayList<>();
                temp.add(kvp);
                separatedKeyValuePairs.add(temp);
            }
        }

        return separatedKeyValuePairs;
    }

    public static void reduce(List<List<KeyValuePair<Character, Integer>>> separatedKeyValuePairs, int totalLetters) throws InterruptedException {
        System.out.println("Reducing input");
        ExecutorService es = Executors.newCachedThreadPool();
        List<ReduceTask> threads = new ArrayList<>();
        for (List<KeyValuePair<Character, Integer>> list : separatedKeyValuePairs) {
            ReduceTask reduceTask = new ReduceTask(list, totalLetters);
            threads.add(reduceTask);
            es.execute(reduceTask);
        }
        es.shutdown();
        if (es.awaitTermination(1, TimeUnit.MINUTES)) {
            for (ReduceTask reduceTask1 : threads) {
                KeyValuePair<Character, Float> kvp = reduceTask1.getResult();
                System.out.println(kvp.getKey() + " : " + kvp.getValue() + " %");
            }
        }
    }
}
