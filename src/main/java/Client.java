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
            long startTime = System.currentTimeMillis();
            parallel(files, MAX_CORES, true);
            long finishTime = System.currentTimeMillis();
            System.out.println("Total time taken : " + (finishTime - startTime) + " ms");
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
//            BufferedReader br = null;
//            try {
//                int totalWords = 0;
//                //Input of the program
//                if (usePrint) System.out.printf("%s:\n", file);
//                br = new BufferedReader(new FileReader(file));
//                String line;
//                List<KeyValuePair<Character, List<Integer>>> result = new ArrayList<>();
//                while ((line = br.readLine()) != null) {
//                    //Executing the map phase
//                    MapTask mapTask = new MapTask(line);
//                    KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> resultCalled = mapTask.call();
//                    result.addAll(resultCalled.getValue());
//                    totalWords += resultCalled.getKey();
//                    mapTask = null;
//                }
//
//                List<KeyValuePair<Character, List<Integer>>> shuffledList = shuffle(result, new ArrayList<>());
//                reduceSequential(shuffledList, totalWords, usePrint);
//
//            } catch (IOException e) {
//                System.out.println("File not found, skipping");
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } finally {
//                if (br != null) {
//                    try {
//                        br.close();
//                    } catch (IOException e) {
//                        System.out.println("Cannot close the buffered reader");
//                    }
//                }
//            }
        }
    }

    public static void parallel(List<String> files, int threadNumber, boolean usePrint) {
        for (String file : files) {
            BufferedReader br = null;
            try {
                ExecutorService es = Executors.newFixedThreadPool(threadNumber);
                int totalWords = 0;
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
                    currentLine++;
                    if (currentLine % 50000 == 0 && currentLine != 0 || lines == currentLine) {
                        //Executing the map phase
                        MapTask mapTask = new MapTask(sb.toString());
                        sb.setLength(0);
                        futures.add(es.submit(mapTask));
                        System.out.println("Queued " + currentLine + " from " + lines);
                    }
                    if (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_MEMORY_THRESHOLD || (lines == currentLine)) {
                        System.out.println("Memory limit reached, waiting for tasks to finish");
                        es.shutdown();
                        if (es.awaitTermination(5, TimeUnit.MINUTES)) {
                            if (futures.size() > 0) {
                                File outFile = new File("temp/temp_" + fileCounter + ".txt");
                                FileOutputStream fos = new FileOutputStream(outFile, true);
                                BufferedOutputStream bos = new BufferedOutputStream(fos);
                                ObjectOutputStream oos = new ObjectOutputStream(bos);

                                for (Future<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>> future : futures) {
                                    KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> futureResult = future.get();
                                    totalWords += futureResult.getKey();
                                    oos.writeObject(futureResult.getValue());
                                    futureResult = null;
                                }
                                oos.flush();
                                oos.close();
                                bos.flush();
                                bos.close();
                                fos.flush();
                                fos.close();
                                fileCounter++;
                                futures.clear();
                            }
                            if (lines != currentLine) {
                                es = Executors.newFixedThreadPool(threadNumber);
                            }
                        }
                    }
                }

                List<Character> alreadyAdded = shuffle(fileCounter);

                reduce(totalWords, alreadyAdded, threadNumber, usePrint);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException | ClassNotFoundException e) {
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

    static List<BufferedWriter> bufferedOutputStreams = new ArrayList<>();

    private static List<Character> shuffle(int fileCounter) throws IOException, ClassNotFoundException {
        List<Character> alreadyAdded = new ArrayList<>();
        for (int i = 0; i < fileCounter; i++) {
            File inFile = new File("temp/temp_" + i + ".txt");
            System.out.println("Processing file " + inFile.getName());
            if (inFile.exists()) {
                FileInputStream fos = new FileInputStream(inFile);
                BufferedInputStream bos = new BufferedInputStream(fos);
                ObjectInputStream oos = new ObjectInputStream(bos);
                while (bos.available() > 0) {
                    Object o = oos.readObject();
                    if (o instanceof List<?>) {
                        List<KeyValuePair<Character, List<Integer>>> parsedObject = (List<KeyValuePair<Character, List<Integer>>>) o;
                        for (KeyValuePair<Character, List<Integer>> pair : parsedObject) {
                            checkAndAdd(alreadyAdded, pair);
                        }
                    }
                }
            }
        }

        System.out.println("Shuffle finished initiating stream closed");

        for (BufferedWriter bufferedOutputStream : bufferedOutputStreams) {
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        }

        System.out.println("Closed all streams");

        return alreadyAdded;
    }

    private static void checkAndAdd(List<Character> alreadyAdded, KeyValuePair<Character, List<Integer>> pair) throws IOException {
        if (!alreadyAdded.contains(pair.getKey())) {
            alreadyAdded.add(pair.getKey());
            int index = alreadyAdded.indexOf(pair.getKey());
            File outFile = new File("temp/temp_shuffled_" + index + ".txt");
            BufferedWriter bos = new BufferedWriter(new FileWriter(outFile, true));
            bufferedOutputStreams.add(bos);
        }
        int index = alreadyAdded.indexOf(pair.getKey());
        BufferedWriter bos = bufferedOutputStreams.get(index);
        bos.write(pair.getValue().get(0));
        bos.write('\n');
    }

    public static void reduce(int totalWords, List<Character> alreadyAdded, int threadNumber, boolean usePrint) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
        List<Future<KeyValuePair<Character, Float>>> futures = new ArrayList<>();
        ExecutorService es = Executors.newFixedThreadPool(threadNumber);
        for (int i = 0; i < alreadyAdded.size(); i++) {
            String fileName = "temp/temp_shuffled_" + i + ".txt";
            ReduceTask reduceTask = new ReduceTask(fileName, totalWords, alreadyAdded.get(i));
            futures.add(es.submit(reduceTask));
        }
        es.shutdown();

        if (es.awaitTermination(15, TimeUnit.MINUTES) && usePrint) {
            for (Future<KeyValuePair<Character, Float>> future : futures) {
                KeyValuePair<Character, Float> kvp = future.get();
                System.out.printf("%s : %.2f%s", kvp.getKey(), kvp.getValue(), "%\n");
            }
        }
    }

//    public static void reduceSequential(List<KeyValuePair<Character, List<Integer>>> separatedKeyValuePairs, int totalLetters, boolean usePrint) throws InterruptedException {
//        for (KeyValuePair<Character, List<Integer>> list : separatedKeyValuePairs) {
//            ReduceTask reduceTask = new ReduceTask(list, totalLetters);
//            reduceTask.run();
//            KeyValuePair<Character, Float> kvp = reduceTask.getResult();
//            if (usePrint) System.out.printf("%s : %.2f%s", kvp.getKey(), kvp.getValue(), "%\n");
//        }
//    }
}
