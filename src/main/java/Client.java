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

        cleanTemps();

        if (args.length != 0) {
            List<String> argList = Arrays.asList(args);
            if (argList.contains("-p") || argList.contains("--parallel")) {
                parallel(argList.subList(1, argList.size()), MAX_CORES, 10000, true);
            }

            if (argList.contains("-s") || argList.contains("--sequential")) {
                sequential(argList.subList(1, argList.size()), 10000, true);
            }

            if (argList.contains("-b") || argList.contains("--benchmark")) {
                benchmark(argList.subList(1, argList.size()));
            }

            if (argList.contains("-h") || argList.contains("--help")) {
                System.out.println("Usage: java Client [OPTION] [FILE1] [FILE2] ...");
                System.exit(0);
            }

            if (argList.contains("-c") || argList.contains("--clean")) {
                cleanTemps();
            }
        } else {
            System.out.println("Usage: java Client [OPTION] [FILE1] [FILE2] ...");
        }
    }

    private static void benchmark(List<String> files) {
        long startTime;
        long finishTime;
        for (int i = 1000; i < 10000000; i = i * 2) {
            startTime = System.currentTimeMillis();
            sequential(files, i, false);
            finishTime = System.currentTimeMillis();
            System.out.printf("Sequential mode and %d line splitting takes : %d ms\n", i, (finishTime - startTime));
        }

        for (int i = 1; i < MAX_CORES; i++) {
            for (int j = 1000; j < 10000000; j = j * 2) {
                startTime = System.currentTimeMillis();
                parallel(files, i, j, false);
                finishTime = System.currentTimeMillis();
                System.out.printf("Parallel mode with %d threads and %d lines splitting takes : %d ms\n", i, j, (finishTime - startTime));
            }
        }
    }

    public static void sequential(List<String> files, int lineSplittingFactor, boolean usePrint) {
        for (String file : files) {
            cleanTemps();
            BufferedReader br = null;
            try {
                int totalWords = 0;
                //Input of the program
                if (usePrint) System.out.printf("%s:\n", file);
                long lines = Files.lines(Paths.get(file)).count();
                long currentLine = 0;
                int fileCounter = 0;
                br = new BufferedReader(new FileReader(file));
                String line;
                StringBuilder sb = new StringBuilder();
                List<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>> futures = new ArrayList<>();

                while ((line = br.readLine()) != null) {
                    sb.append(line)
                            .append(" ");
                    currentLine++;
                    if (currentLine % lineSplittingFactor == 0 && currentLine != 0 || lines == currentLine) {
                        //Executing the map phase
                        MapTask mapTask = new MapTask(sb.toString());
                        sb.setLength(0);
                        futures.add(mapTask.call());
                        //System.out.println("Queued " + currentLine + " from " + lines);
                    }
                    if (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_MEMORY_THRESHOLD || (lines == currentLine)) {
                        //System.out.println("Memory limit reached, waiting for tasks to finish");
                        if (futures.size() > 0) {
                            File outFile = new File("temp/temp_" + fileCounter + ".txt");
                            FileOutputStream fos = new FileOutputStream(outFile, true);
                            BufferedOutputStream bos = new BufferedOutputStream(fos);
                            ObjectOutputStream oos = new ObjectOutputStream(bos);

                            for (KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> future : futures) {
                                totalWords += future.getKey();
                                oos.writeObject(future.getValue());
                                future = null;
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
                    }
                }

                if (futures.size() > 0) {
                    for (KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> future : futures) {
                        totalWords += future.getKey();
                    }
                    futures.clear();
                }

                List<Character> alreadyAdded = shuffle(fileCounter);

                reduceSequential(totalWords, alreadyAdded, usePrint);
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

    public static void parallel(List<String> files, int threadNumber, int lineSplittingFactor, boolean usePrint) {
        for (String file : files) {
            cleanTemps();
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
                    if (currentLine % lineSplittingFactor == 0 && currentLine != 0 || lines == currentLine) {
                        //Executing the map phase
                        MapTask mapTask = new MapTask(sb.toString());
                        sb.setLength(0);
                        futures.add(es.submit(mapTask));
                        //System.out.println("Queued " + currentLine + " from " + lines);
                    }
                    if (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_MEMORY_THRESHOLD || (lines == currentLine)) {
                        //System.out.println("Memory limit reached, waiting for tasks to finish");
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
                es.shutdown();
                if (es.awaitTermination(5, TimeUnit.MINUTES)) {
                    if (futures.size() > 0) {
                        for (Future<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>> future : futures) {
                            totalWords += future.get().getKey();
                        }
                        futures.clear();
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

    static volatile List<BufferedWriter> bufferedOutputStreams = new ArrayList<>();
    static volatile List<FileWriter> fileWriters = new ArrayList<>();

    private static List<Character> shuffle(int fileCounter) throws IOException, ClassNotFoundException {
        List<Character> alreadyAdded = new ArrayList<>();
        bufferedOutputStreams.clear();

        for (int i = 0; i < fileCounter; i++) {
            File inFile = new File("temp/temp_" + i + ".txt");
            //System.out.println("Processing file " + inFile.getName());
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
                oos.close();
                bos.close();
                fos.close();
            }
        }

        //System.out.println("Shuffle finished initiating stream closing");

        for (BufferedWriter bufferedOutputStream : bufferedOutputStreams) {
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        }

        for (FileWriter fileWriter : fileWriters) {
            fileWriter.close();
        }

        //System.out.println("Closed all streams");

        return alreadyAdded;
    }

    private static void checkAndAdd(List<Character> alreadyAdded, KeyValuePair<Character, List<Integer>> pair) throws IOException {
        if (!alreadyAdded.contains(pair.getKey())) {
            alreadyAdded.add(pair.getKey());
            int index = alreadyAdded.indexOf(pair.getKey());
            File outFile = new File("temp/temp_shuffled_" + index + ".txt");
            FileWriter fw = new FileWriter(outFile, true);
            fileWriters.add(fw);
            BufferedWriter bos = new BufferedWriter(fw);
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
                if (usePrint) System.out.printf("%s : %.2f%s", kvp.getKey(), kvp.getValue(), "%\n");
            }
        }
    }

    public static void reduceSequential(int totalWords, List<Character> alreadyAdded, boolean usePrint) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
        for (int i = 0; i < alreadyAdded.size(); i++) {
            String fileName = "temp/temp_shuffled_" + i + ".txt";
            ReduceTask reduceTask = new ReduceTask(fileName, totalWords, alreadyAdded.get(i));
            KeyValuePair<Character, Float> kvp = reduceTask.call();
            if (usePrint) System.out.printf("%s : %.2f%s", kvp.getKey(), kvp.getValue(), "%\n");
        }
    }

    private static void cleanTemps() {
        boolean success = false;
        try {
            File tempDir = new File("temp");
            File[] tempFiles = tempDir.listFiles();
            if (tempFiles != null) {
                for (File tempFile : tempFiles) {
                    tempFile.delete();
                }
            }
        } catch (Exception e) {
            System.out.println("Cannot delete the temp directory");
        }
    }
}
