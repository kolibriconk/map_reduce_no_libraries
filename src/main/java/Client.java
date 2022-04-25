import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {

    static final int MAX_CORES = Runtime.getRuntime().availableProcessors() - 1;
   //static final int BUFFER_SIZE = Integer.MAX_VALUE;// 8 * 1024;

    public static void main(String[] args) {
        if (args.length != 0) {
            List<String> files = Arrays.asList(args);
            //Uncomment this section to execute the comparison
            //benchmark(files);

            //Uncomment this section to execute the sequential mode
            //sequential(files, true);

            //Uncomment this section to execute the parallel mode
            parallel(files, MAX_CORES, true);
        } else {
            System.out.println("No files to process");
        }
    }

    private static void benchmark(List<String> files) {
        long startTime = System.currentTimeMillis();
        sequential(files, false);
        long finishTime = System.currentTimeMillis();
        System.out.println("Sequential mode takes : " + (finishTime - startTime) + " ms");

        for (int i = 1; i < MAX_CORES; i++) {
            startTime = System.currentTimeMillis();
            parallel(files, i, false);
            finishTime = System.currentTimeMillis();
            System.out.printf("Parallel mode with %d threads takes : %d ms\n", i, (finishTime - startTime));
        }
    }

    public static void sequential(List<String> files, boolean usePrint) {
        for (String file : files) {
            BufferedReader br = null;
            try {
                int totalWords = 0;
                //Input of the program
                if (usePrint) System.out.printf("%s:\n", file);
                br = new BufferedReader(new FileReader(file));
                String line;
                List<KeyValuePair<Character, Integer>> result = new ArrayList<>();
                while ((line = br.readLine()) != null) {
                    //Executing the map phase
                    MapTask mapTask = new MapTask(line);
                    mapTask.run();
                    result.addAll(mapTask.getResult());
                    totalWords += mapTask.getCount();
                    mapTask = null;
                    System.gc();
                }

                List<List<KeyValuePair<Character, Integer>>> shuffledList = shuffle(result);
                reduceSequential(shuffledList, totalWords, usePrint);

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

    public static void parallel(List<String> files, int threadNumber, boolean usePrint) {
        System.out.printf("%s:\n", new SimpleDateFormat("HH:mm:ss").format(new Date()));
        for (String file : files) {
            BufferedReader br = null;
            try {
                List<MapTask> threads = new ArrayList<>();
                ExecutorService es = Executors.newFixedThreadPool(threadNumber);
                int totalLetters = 0;
                //Input of the program
                if (usePrint) System.out.printf("%s:\n", file);
                br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    //Executing the map phase
                    MapTask mapTask = new MapTask(line);
                    threads.add(mapTask);
                    es.execute(mapTask);
                }
                es.shutdown();

                if (es.awaitTermination(5, TimeUnit.MINUTES)) {
                    List<KeyValuePair<Character, Integer>> result = new ArrayList<>();
                    for (MapTask mapTask : threads) {
                        result.addAll(mapTask.getResult());
                        totalLetters += mapTask.getCount();
                        mapTask = null;
                        System.gc();
                    }
                    List<List<KeyValuePair<Character, Integer>>> shuffledList = shuffle(result);
                    reduce(shuffledList, totalLetters, usePrint);
                }
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

    public static void reduce(List<List<KeyValuePair<Character, Integer>>> separatedKeyValuePairs, int totalLetters, boolean usePrint) throws InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        List<ReduceTask> threads = new ArrayList<>();
        for (List<KeyValuePair<Character, Integer>> list : separatedKeyValuePairs) {
            ReduceTask reduceTask = new ReduceTask(list, totalLetters);
            threads.add(reduceTask);
            es.execute(reduceTask);
        }
        es.shutdown();
        if (es.awaitTermination(5, TimeUnit.MINUTES)) {
            for (ReduceTask reduceTask1 : threads) {
                KeyValuePair<Character, Float> kvp = reduceTask1.getResult();
                if (usePrint) System.out.printf("%s : %.2f%s", kvp.getKey(), kvp.getValue(), "%\n");
            }
        }
    }

    public static void reduceSequential(List<List<KeyValuePair<Character, Integer>>> separatedKeyValuePairs, int totalLetters, boolean usePrint) throws InterruptedException {
        for (List<KeyValuePair<Character, Integer>> list : separatedKeyValuePairs) {
            ReduceTask reduceTask = new ReduceTask(list, totalLetters);
            reduceTask.run();
            KeyValuePair<Character, Float> kvp = reduceTask.getResult();
            if (usePrint) System.out.printf("%s : %.2f%s", kvp.getKey(), kvp.getValue(), "%\n");
        }
    }
}
