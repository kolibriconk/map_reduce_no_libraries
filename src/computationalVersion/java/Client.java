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
            System.out.println("Cannot create the temp directory");
        }

        if (args.length != 0) {
            List<String> files = Arrays.asList(args);
            long startTime = System.currentTimeMillis();
            parallel(files, MAX_CORES, true);
            long finishTime = System.currentTimeMillis();
            System.out.println("Parallel mode takes : " + (finishTime - startTime) + " ms");
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
                List<KeyValuePair<Character, Float>> result = new ArrayList<>((int) lines);

                StringBuilder sb = new StringBuilder();

                List<Future<KeyValuePair<Integer, List<KeyValuePair<Character, Float>>>>> futures = new ArrayList<>();

                while ((line = br.readLine()) != null) {
                    sb.append(line)
                            .append(" ");
                    currentLine++;
                    if (currentLine % 1000 == 0 && currentLine != 0 || lines == currentLine) {
                        //Executing the map phase
                        MapTask mapTask = new MapTask(sb.toString());
                        sb.setLength(0);
                        futures.add(es.submit(mapTask));
                    }

                    if (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_MEMORY_THRESHOLD || lines == currentLine) {
                        es.shutdown();
                        if (es.awaitTermination(5, TimeUnit.MINUTES)) {
                            es = Executors.newFixedThreadPool(threadNumber);
                            List<Future<KeyValuePair<Character, Float>>> reducedFutures = new ArrayList<>();
                            for (Future<KeyValuePair<Integer, List<KeyValuePair<Character, Float>>>> future : futures) {
                                KeyValuePair<Integer, List<KeyValuePair<Character, Float>>> resultCalled = future.get();
                                totalLetters += resultCalled.getKey();
                                List<KeyValuePair<Character, List<Float>>> shuffledList = shuffle(resultCalled.getValue());
                                for (KeyValuePair<Character, List<Float>> shuffledListElement : shuffledList) {
                                    reducedFutures.add(es.submit(new AddReduceTask(shuffledListElement)));
                                }
                            }
                            es.shutdown();
                            if (es.awaitTermination(5, TimeUnit.MINUTES)) {
                                File outFile = new File("temp/temp" + fileCounter + ".txt");
                                try (FileOutputStream fos = new FileOutputStream(outFile, false)) {
                                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                                    for (Future<KeyValuePair<Character, Float>> reducedFuture : reducedFutures) {
                                        oos.writeObject(reducedFuture.get());
                                    }
                                    oos.flush();
                                    oos.close();
                                    bos.flush();
                                    bos.close();
                                } catch (IOException e) {
                                    System.out.println("Cannot write to file");
                                }
                                fileCounter++;
                            }

                            futures.clear();
                            es = Executors.newFixedThreadPool(threadNumber);
                        }
                    }
                }
                es.shutdown();

//                System.out.println("Waiting for threads to finish");
                if (es.awaitTermination(5, TimeUnit.MINUTES)) {
                    for (int i = 0; i < fileCounter; i++) {
                        File inFile = new File("temp/temp" + i + ".txt");
                        if (inFile.exists()) {

                            try (FileInputStream fis = new FileInputStream(inFile);
                                 BufferedInputStream bis = new BufferedInputStream(fis);
                                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                                while (bis.available() > 0) {
                                    Object object = ois.readObject();
                                    if (object instanceof KeyValuePair<?, ?>) {
                                        KeyValuePair<Character, Float> pair = (KeyValuePair<Character, Float>) object;
                                        result.add(pair);
                                    }
                                }
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    //System.out.println("All threads finished, merging results");
                    for (Future<KeyValuePair<Integer, List<KeyValuePair<Character, Float>>>> future : futures) {
                        result.addAll(future.get().getValue());
                        totalLetters += future.get().getKey();
                        future = null;
                    }
                    futures.clear();
                    List<KeyValuePair<Character, List<Float>>> shuffledList = shuffle(result);

                    reduce(shuffledList, totalLetters, usePrint);
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                System.out.println("Execution exception");
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

    public static List<KeyValuePair<Character, List<Float>>> shuffle(List<KeyValuePair<Character, Float>> result) {
        List<KeyValuePair<Character, List<Float>>> separatedKeyValuePairs = new ArrayList<>();
        List<Character> alreadyAdded = new ArrayList<>();
        for (KeyValuePair<Character, Float> kvp : result) {
            if (alreadyAdded.contains(kvp.getKey())) {
                separatedKeyValuePairs.get(alreadyAdded.indexOf(kvp.getKey())).getValue().add(kvp.getValue());
            } else {
                alreadyAdded.add(kvp.getKey());
                List<Float> tempList = new ArrayList<>();
                tempList.add(kvp.getValue());
                KeyValuePair<Character, List<Float>> temp = new KeyValuePair<>(kvp.getKey(), tempList);
                separatedKeyValuePairs.add(temp);
            }
        }

        return separatedKeyValuePairs;
    }

    public static void reduce(List<KeyValuePair<Character, List<Float>>> separatedKeyValuePairs, int totalLetters, boolean usePrint) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newCachedThreadPool();
        List<Future<KeyValuePair<Character, Float>>> futures = new ArrayList<>();
        for (KeyValuePair<Character, List<Float>> list : separatedKeyValuePairs) {
            AddAndDivideReduceTask reduceTask = new AddAndDivideReduceTask(list, totalLetters);
            futures.add(es.submit(reduceTask));
        }
        es.shutdown();
        if (es.awaitTermination(5, TimeUnit.MINUTES)) {
            for (Future<KeyValuePair<Character, Float>> future : futures) {
                KeyValuePair<Character, Float> kvp = future.get();
                if (usePrint) System.out.printf("%s : %.2f%s", kvp.getKey(), kvp.getValue() * 100, "%\n");
            }
        }
    }
}
