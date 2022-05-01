import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class Client {

    static final int MAX_CORES = Runtime.getRuntime().availableProcessors();
    public static final long MEMORY_SIZE = Runtime.getRuntime().maxMemory();
    public static final long MAX_MEMORY_THRESHOLD = MEMORY_SIZE - MEMORY_SIZE / 4;

    public static void main(String[] args) {
        try {
            Files.createDirectories(Paths.get("temp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

        try {
            File tempDir = new File("temp");
            File[] tempFiles = tempDir.listFiles();
            for (File tempFile : tempFiles) {
                tempFile.delete();
            }
            tempDir.delete();
        } catch (Exception e) {
            System.out.println("Cannot delete the temp directory");
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
                List<KeyValuePair<Character, List<Integer>>> result = new ArrayList<>();
                while ((line = br.readLine()) != null) {
                    //Executing the map phase
                    MapTask mapTask = new MapTask(line);
                    KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> resultCalled = mapTask.call();
                    result.addAll(resultCalled.getValue());
                    totalWords += resultCalled.getKey();
                    mapTask = null;
                }

                List<KeyValuePair<Character, List<Integer>>> shuffledList = shuffle(result, new ArrayList<>());
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
        for (String file : files) {
            BufferedReader br = null;
            try {
                ExecutorService es = Executors.newFixedThreadPool(threadNumber);
                int totalLetters = 0;
                //Input of the program
                if (usePrint) System.out.printf("%s:\n", file);
                long lines = Files.lines(Paths.get(file)).count();
                long currentLine = 0;
                int fileCounter = 0;
                br = new BufferedReader(new FileReader(file));
                String line;
                List<KeyValuePair<Character, List<Integer>>> result = new ArrayList<>((int) lines);

                StringBuilder sb = new StringBuilder();

                List<Future<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>>> futures = new ArrayList<>();

                while ((line = br.readLine()) != null) {
                    sb.append(line)
                            .append(" ");
                    if (currentLine % 100000 == 0 && currentLine != 0 || lines - currentLine <= 100000) {
                        //Executing the map phase
                        MapTask mapTask = new MapTask(sb.toString());
                        sb.setLength(0);
                        futures.add(es.submit(mapTask));
                        System.out.println("Queued " + currentLine + " from " + lines);
                    }
                    currentLine++;
                    if (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_MEMORY_THRESHOLD || (lines - currentLine <= 100000)) {
                        System.out.println("Memory limit reached, waiting for tasks to finish");
                        es.shutdown();
                        if (es.awaitTermination(5, TimeUnit.MINUTES)) {
                            for (Future<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>> future : futures) {
                                KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> futureResult = future.get();
                                totalLetters += futureResult.getKey();

                                for (KeyValuePair<Character, List<Integer>> pair : futureResult.getValue()) {
                                    File outFile = new File("temp/temp" + pair.getKey() + ".txt");
                                    if (outFile.createNewFile()) {
                                        FileOutputStream fos = new FileOutputStream(outFile, true);
                                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                                        oos.writeObject(pair);
                                    } else {
                                        FileOutputStream fos = new FileOutputStream(outFile, true);
                                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                                        ObjectOutputStream oos = new CustomObjectOutputStream(bos);
                                        oos.writeObject(pair);
                                    }
                                }
                            }
                            futures.clear();
                            if (!(lines - currentLine <= 100000)) {
                                es = Executors.newFixedThreadPool(threadNumber);
                            }
                        }
                    }
                }
                //Shuffle results from files and rearrange
                //shuffle(tempResult, shuffledList);
                //MAYBE WE HAVE TO SHUFFLE DIRECTLY FROM FILES

                //Reduce phase
                //reduce(shuffledList, totalLetters, usePrint);



//                es.shutdown();
//                System.out.println("Waiting for threads to finish");
//                if (es.awaitTermination(5, TimeUnit.MINUTES)) {
//                    System.out.println("All threads finished, merging results");
//                    for (Future<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>> future : futures) {
//                        result.addAll(future.get().getValue());
//                        totalLetters += future.get().getKey();
//                    }
//                    futures.clear();
//                    System.out.println("Beginning shuffle");
//                    List<KeyValuePair<Character, List<Integer>>> shuffledList = shuffle(result, new ArrayList<>());
//                    result.clear();
//
//                    for (int i = 0; i < fileCounter; i++) {
//                        File tempFile = new File("temp/temp" + i + ".txt");
//                        if (tempFile.exists()) {
//                            List<KeyValuePair<Character, List<Integer>>> tempResult = new ArrayList<>();
//                            //read objects from temp file
//                            try (FileInputStream fis = new FileInputStream(tempFile);
//                                 BufferedInputStream bis = new BufferedInputStream(fis);
//                                 ObjectInputStream ois = new ObjectInputStream(bis)) {
//                                while (bis.available() > 0) {
//                                    //KeyValuePair<Integer, List<KeyValuePair<Character, Integer>>> temp = (KeyValuePair<Integer, List<KeyValuePair<Character, Integer>>>) ois.readObject();
//                                    Object object = ois.readObject();
//                                    if (object instanceof KeyValuePair<?, ?>) {
//                                        KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> parsedObject = (KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>) object;
//                                        tempResult.addAll(parsedObject.getValue());
//                                        totalLetters += parsedObject.getKey();
//                                    }
//                                }
//                                shuffle(tempResult, shuffledList);
//                            } catch (ClassNotFoundException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                    }
//                    //System.out.println("Beginning reduce");
//                    reduce(shuffledList, totalLetters, usePrint);
//                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
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

    public static List<KeyValuePair<Character, List<Integer>>> shuffle(List<KeyValuePair<Character, List<Integer>>> partialMapResult, List<KeyValuePair<Character, List<Integer>>> partialResult) {
        List<Character> alreadyAdded = new ArrayList<>();
        for (KeyValuePair<Character, List<Integer>> kvp : partialMapResult) {
            if (alreadyAdded.contains(kvp.getKey())) {
                partialResult.get(alreadyAdded.indexOf(kvp.getKey())).getValue().add(1);
            } else {
                alreadyAdded.add(kvp.getKey());
                List<Integer> tempList = new ArrayList<>();
                tempList.add(1);
                KeyValuePair<Character, List<Integer>> temp = new KeyValuePair<>(kvp.getKey(), tempList);
                partialResult.add(temp);
            }
        }
        return partialResult;
    }

    public static void reduce(List<KeyValuePair<Character, List<Integer>>> separatedKeyValuePairs, int totalLetters, boolean usePrint) throws InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        List<ReduceTask> threads = new ArrayList<>();
        for (KeyValuePair<Character, List<Integer>> list : separatedKeyValuePairs) {
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

    public static void reduceSequential(List<KeyValuePair<Character, List<Integer>>> separatedKeyValuePairs, int totalLetters, boolean usePrint) throws InterruptedException {
        for (KeyValuePair<Character, List<Integer>> list : separatedKeyValuePairs) {
            ReduceTask reduceTask = new ReduceTask(list, totalLetters);
            reduceTask.run();
            KeyValuePair<Character, Float> kvp = reduceTask.getResult();
            if (usePrint) System.out.printf("%s : %.2f%s", kvp.getKey(), kvp.getValue(), "%\n");
        }
    }
}
