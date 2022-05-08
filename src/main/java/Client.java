import javax.swing.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author Jose Antonio Ramos Andrades - 1565479
 * @author Victor Sancho Aguilera - 1529721
 */

@SuppressWarnings("unchecked")
public class Client {

    static final int MAX_CORES = Runtime.getRuntime().availableProcessors();
    public static final long MEMORY_SIZE = Runtime.getRuntime().maxMemory();
    public static final long MAX_MEMORY_THRESHOLD = MEMORY_SIZE - MEMORY_SIZE / 4;

    private static final String READ_PATH = "/data/";

    /**
     * Main method.
     *
     * @param args It is expected to be: the program mode and the fileNames to the files to be processed.
     */
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
                int splittingFactor = 3000;
                int index = argList.indexOf("-sf");
                if (index != -1) {
                    try {
                        splittingFactor = Integer.parseInt(argList.get(index + 1));
                    } catch (NumberFormatException ignored) {
                    }
                }
                parallel(argList.subList(index + 2, argList.size()), MAX_CORES, splittingFactor, true, true);
            }

            if (argList.contains("-s") || argList.contains("--sequential")) {
                int splittingFactor = 4000;
                int index = argList.indexOf("-sf");
                if (index != -1) {
                    try {
                        splittingFactor = Integer.parseInt(argList.get(index + 1));
                    } catch (NumberFormatException ignored) {
                    }
                }
                sequential(argList.subList(1, argList.size()), splittingFactor, true);
            }

            if (argList.contains("-b") || argList.contains("--benchmark")) {
                benchmark(argList.subList(1, argList.size()));
            }

            if (argList.contains("-his") || argList.contains("--histogram")) {
                int index = argList.indexOf("-sf");
                int splittingFactor = 3000;

                if (index != -1) {
                    try {
                        splittingFactor = Integer.parseInt(argList.get(index + 1));
                    } catch (NumberFormatException ignored) {
                    }
                }
                int indexOfHis = argList.indexOf("-his");
                boolean totalHistogram = Boolean.parseBoolean(argList.get(indexOfHis + 1));
                histogram(argList.subList(index + 2, argList.size()), totalHistogram, splittingFactor);
            }

            if (argList.contains("-h") || argList.contains("--help")) {
                System.out.println("Usage: java Client [OPTION] [FILE1] [FILE2] ... [FILEn]");
                System.exit(0);
            }

            if (argList.contains("-c") || argList.contains("--clean")) {
                cleanTemps();
            }
        } else {
            System.out.println("Usage: java Client [OPTION] [FILE1] [FILE2] ... [FILEn]");
        }

        cleanTemps();
    }

    /**
     * Benchmarking the program by running it sequentially and
     * parallel with different number of threads and splittingFactor.
     *
     * @param files the files to be processed.
     */
    private static void benchmark(List<String> files) {
        long startTime;
        long finishTime;
        //Try to run the program with different splittingFactor using sequential approach.
        for (int i = 1000; i < 20000; i += 1000) {
            startTime = System.currentTimeMillis();
            sequential(files, i, false);
            finishTime = System.currentTimeMillis();
            System.out.printf("Sequential mode and %d line splitting takes : %d ms\n", i, (finishTime - startTime));
        }

        //Try to run the program with different splittingFactor and threads using parallel approach.
        for (int i = 1; i <= MAX_CORES; i++) {
            for (int j = 1000; j < 20000; j += 1000) {
                startTime = System.currentTimeMillis();
                parallel(files, i, j, false, true);
                finishTime = System.currentTimeMillis();
                System.out.printf("Parallel mode with %d threads and %d lines splitting takes : %d ms\n", i, j, (finishTime - startTime));
            }
        }
    }

    /**
     * Method for printing histogram of the words in the file.
     *
     * @param files the files to be processed.
     */
    private static void histogram(List<String> files, boolean totalHistogram, int splittingFactor) {
        List<KeyValuePair<Character, Float>> result = new ArrayList<>();

        if (totalHistogram) {
            result = parallel(files, MAX_CORES, splittingFactor, true, false);
            createBarChart(result);
        } else {
            for (String file : files) {
                List<String> adapter = new ArrayList<>();
                adapter.add(file);
                result = parallel(adapter, MAX_CORES,
                        splittingFactor, true, true);
                createBarChart(result);
            }
        }

        cleanTemps();
    }

    /**
     * Splits and map the input file into multiple tasks to be executed sequentially using a single thread (main thread).
     *
     * @param files               the files to be mapped.
     * @param lineSplittingFactor the number of lines to be split into a single task.
     * @param usePrint            whether to print the output to the console or not.
     */
    private static void sequential(List<String> files, int lineSplittingFactor, boolean usePrint) {
        //For each file, split it into multiple tasks and execute them sequentially.
        for (String file : files) {
            file = READ_PATH + file;
            cleanTemps();
            BufferedReader br = null;
            try {
                int totalWords = 0;
                //Input of the program
                if (usePrint) System.out.printf("%s:\n", file.replace(READ_PATH, ""));
                long lines = Files.lines(Paths.get(file)).count();
                long currentLine = 0;
                int fileCounter = 0;
                br = new BufferedReader(new FileReader(file)); //Using buffered reader to read the file line by line.
                String line;
                StringBuilder sb = new StringBuilder(); //String builder is used to improve the performance.
                List<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>> futures = new ArrayList<>();

                while ((line = br.readLine()) != null) {
                    sb.append(line)
                            .append(" "); //Append the line to the string builder.
                    currentLine++;
                    if (currentLine % lineSplittingFactor == 0 && currentLine != 0 || lines == currentLine) {
                        //When the line reaches the splitting factor or is the last line create a new task to Map.
                        MapTask mapTask = new MapTask(sb.toString());
                        sb.setLength(0);
                        futures.add(mapTask.call());
                    }
                    if (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_MEMORY_THRESHOLD || (lines == currentLine)) {
                        //When the memory reaches the threshold or is the last line, write the results to temporary files.
                        if (futures.size() > 0) {
                            File outFile = new File("temp/temp_" + fileCounter + ".txt");
                            FileOutputStream fos = new FileOutputStream(outFile, true);
                            BufferedOutputStream bos = new BufferedOutputStream(fos);
                            ObjectOutputStream oos = new ObjectOutputStream(bos);

                            for (KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> future : futures) {
                                totalWords += future.getKey();
                                oos.writeObject(future.getValue());
                                future = null; //Deallocate to free the memory.
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

    /**
     * Splits and map the input file into multiple tasks to be executed in parallel using a pool of threads.
     *
     * @param files               the files to be mapped.
     * @param threadNumber        the number of threads to be used.
     * @param lineSplittingFactor the number of lines to be split into a single task.
     * @param usePrint            whether to print the output to the console or not.
     */
    private static List<KeyValuePair<Character, Float>> parallel(List<String> files, int threadNumber, int lineSplittingFactor, boolean usePrint, boolean separatePerFile) throws RuntimeException {
        int totalWords = 0;
        int fileCounter = 0;
        List<KeyValuePair<Character, Float>> result = new ArrayList<>();
        for (String file : files) {
            file = READ_PATH + file;
            cleanTemps();
            BufferedReader br = null;
            try {
                //Create a thread pool with the number of threads specified.
                ExecutorService es = Executors.newFixedThreadPool(threadNumber);
                if (separatePerFile) {
                    totalWords = 0;
                    fileCounter = 0;
                }
                //Input of the program
                if (usePrint && separatePerFile) System.out.printf("%s:\n", file.replace(READ_PATH, ""));
                long lines = Files.lines(Paths.get(file)).count();
                long currentLine = 0;
                br = new BufferedReader(new FileReader(file)); //Using buffered reader to read the file line by line.
                String line;
                StringBuilder sb = new StringBuilder(); //String builder is used to improve the performance.
                List<Future<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>>> futures = new ArrayList<>();

                while ((line = br.readLine()) != null) {
                    sb.append(line)
                            .append(" "); //Append the line to the string builder.
                    currentLine++;
                    if (currentLine % lineSplittingFactor == 0 && currentLine != 0 || lines == currentLine) {
                        //When the line reaches the splitting factor or is the last line create a new task to Map using thread.
                        MapTask mapTask = new MapTask(sb.toString());
                        sb.setLength(0);
                        futures.add(es.submit(mapTask));
                    }
                    if (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() > MAX_MEMORY_THRESHOLD || (lines == currentLine)) {
                        //When the memory reaches the threshold or is the last line, write the results to temporary files.
                        es.shutdown(); //Shutdown the pool of threads so there are no more tasks to be queued.
                        if (es.awaitTermination(5, TimeUnit.MINUTES)) { //Wait for the tasks to finish.
                            if (futures.size() > 0) {
                                //Create a new file to store the partial results.
                                File outFile = new File("temp/temp_" + fileCounter + ".txt");
                                FileOutputStream fos = new FileOutputStream(outFile, true);
                                BufferedOutputStream bos = new BufferedOutputStream(fos);
                                ObjectOutputStream oos = new ObjectOutputStream(bos);

                                for (Future<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>> future : futures) {
                                    KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>> futureResult = future.get();
                                    totalWords += futureResult.getKey();
                                    oos.writeObject(futureResult.getValue());
                                    futureResult = null; //Deallocate the memory.
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
                                //If there are more lines to be processed, create a new pool of threads.
                                es = Executors.newFixedThreadPool(threadNumber);
                            }
                        }
                    }
                }
                es.shutdown(); //Shutdown the pool of threads so tasks can finish.
                if (es.awaitTermination(5, TimeUnit.MINUTES)) { //Wait for the tasks to finish.
                    if (futures.size() > 0) {
                        for (Future<KeyValuePair<Integer, List<KeyValuePair<Character, List<Integer>>>>> future : futures) {
                            totalWords += future.get().getKey(); //Sum the total number of words on the input file.
                        }
                        futures.clear(); //Clear the list of futures to deallocate the memory.
                    }
                }

                if (separatePerFile) {
                    List<Character> alreadyAdded = shuffle(fileCounter);
                    result = reduce(totalWords, alreadyAdded, threadNumber, usePrint);
                }

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

        if (!separatePerFile) {
            if (usePrint) System.out.println("Result of all files: ");
            List<Character> alreadyAdded = null;
            try {
                alreadyAdded = shuffle(fileCounter);
                result = reduce(totalWords, alreadyAdded, threadNumber, usePrint);
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Cannot read the output file");
            } catch (ExecutionException | InterruptedException e) {
                System.out.println("Cannot execute the reduce function");
            }
        }

        return result;
    }

    static volatile List<BufferedWriter> bufferedWriters = new ArrayList<>();
    static volatile List<FileWriter> fileWriters = new ArrayList<>();

    /**
     * Order the different results on temporary files and write them to different files depending on the character.
     *
     * @param fileCounter the number of temporary files created on the previous step.
     * @return a list of characters that have already been added to a file and consequently a temp file has been created.
     * @throws IOException            if an I/O error occurs.
     * @throws ClassNotFoundException if the class of a serialized object cannot be found.
     */
    private static List<Character> shuffle(int fileCounter) throws IOException, ClassNotFoundException {
        List<Character> alreadyAdded = new ArrayList<>();
        bufferedWriters.clear();

        for (int i = 0; i < fileCounter; i++) {
            //For each temporary file, create a new reader.
            File inFile = new File("temp/temp_" + i + ".txt");
            if (inFile.exists()) {
                FileInputStream fos = new FileInputStream(inFile);
                BufferedInputStream bos = new BufferedInputStream(fos);
                ObjectInputStream oos = new ObjectInputStream(bos);
                while (bos.available() > 0) {
                    Object o = oos.readObject(); //Read the object from the line.
                    if (o instanceof List<?>) {
                        //Cast the object to a list of KeyValuePairs.
                        List<KeyValuePair<Character, List<Integer>>> parsedObject
                                = (List<KeyValuePair<Character, List<Integer>>>) o;
                        for (KeyValuePair<Character, List<Integer>> pair : parsedObject) {
                            //Call the method to check if the character has already been added to a file.
                            checkAndAdd(alreadyAdded, pair);
                        }
                    }
                }
                //Close the reading streams.
                oos.close();
                bos.close();
                fos.close();
            }
        }

        //Close the writing streams.
        for (BufferedWriter bufferedOutputStream : bufferedWriters) {
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        }
        //Close the writing streams.
        for (FileWriter fileWriter : fileWriters) {
            fileWriter.close();
        }
        return alreadyAdded;
    }

    /**
     * Checks if the character is already added to the list.
     * If it is not then it adds.
     * Independent if the character is previously added or not the pair is written to a temporary file.
     *
     * @param alreadyAdded list of already added characters and, consequently, created the temporary file.
     * @param pair         the pair to be added.
     * @throws IOException if the temporary file cannot be created or written.
     */
    private static void checkAndAdd(List<Character> alreadyAdded, KeyValuePair<Character, List<Integer>> pair) throws IOException {
        if (!alreadyAdded.contains(pair.getKey())) {
            //If the character is not already added, add it to the list.
            //And create a new temporary file to store this type of character.
            alreadyAdded.add(pair.getKey());
            int index = alreadyAdded.indexOf(pair.getKey());
            File outFile = new File("temp/temp_shuffled_" + index + ".txt");
            FileWriter fw = new FileWriter(outFile, true);
            fileWriters.add(fw);
            BufferedWriter bos = new BufferedWriter(fw);
            bufferedWriters.add(bos);
        }
        int index = alreadyAdded.indexOf(pair.getKey());
        BufferedWriter bos = bufferedWriters.get(index);
        //Write the pair to the temporary file.
        bos.write(pair.getValue().get(0));
        bos.write('\n');
    }

    /**
     * Reduce the stored results from temps files and prints
     * it to System Console using multithreading.
     *
     * @param totalWords   total number of words in the input file.
     * @param alreadyAdded list of already added characters.
     * @param threadNumber number of threads to use.
     * @param usePrint     if true, prints the result to System Console.
     * @return the list of results.
     * @throws IOException            if there is an error while reading from the file.
     * @throws ClassNotFoundException if there is an error while deserializing the object.
     * @throws InterruptedException   if there is an error while waiting for the thread to finish.
     * @throws ExecutionException     if there is an error while waiting for the thread to finish.
     */
    private static List<KeyValuePair<Character, Float>> reduce(int totalWords, List<Character> alreadyAdded, int threadNumber, boolean usePrint) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
        List<Future<KeyValuePair<Character, Float>>> futures = new ArrayList<>();
        List<KeyValuePair<Character, Float>> result = new ArrayList<>();
        //Create a thread pool with the number of threads specified.
        ExecutorService es = Executors.newFixedThreadPool(threadNumber);
        for (int i = 0; i < alreadyAdded.size(); i++) {
            //Create a new thread for each character temp file.
            String fileName = "temp/temp_shuffled_" + i + ".txt";
            ReduceTask reduceTask = new ReduceTask(fileName, totalWords, alreadyAdded.get(i));
            futures.add(es.submit(reduceTask));
        }
        es.shutdown();

        if (es.awaitTermination(10, TimeUnit.MINUTES)) {
            for (Future<KeyValuePair<Character, Float>> future : futures) {
                KeyValuePair<Character, Float> kvp = future.get();
                //Print the result to System Console if requested.
                result.add(kvp);
                if (usePrint) System.out.printf("%s : %.2f%s", kvp.getKey(), kvp.getValue(), "%\n");
            }
            System.out.println();
        }

        return result;
    }

    /**
     * Reduce the stored results from temps files and prints
     * it to System Console using sequential approach (no threads).
     *
     * @param totalWords   total number of words in the input file.
     * @param alreadyAdded list of already added characters.
     * @param usePrint     if true, prints the result to System Console.
     * @throws IOException            if there is an error while reading from the file.
     * @throws ClassNotFoundException if there is an error while deserializing the object.
     * @throws InterruptedException   if there is an error while waiting for the thread to finish.
     * @throws ExecutionException     if there is an error while waiting for the thread to finish.
     */
    private static void reduceSequential(int totalWords, List<Character> alreadyAdded, boolean usePrint) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
        for (int i = 0; i < alreadyAdded.size(); i++) {
            //For each character temp file, reduce the results.
            String fileName = "temp/temp_shuffled_" + i + ".txt";
            ReduceTask reduceTask = new ReduceTask(fileName, totalWords, alreadyAdded.get(i));
            KeyValuePair<Character, Float> kvp = reduceTask.call();
            if (usePrint) System.out.printf("%s : %.2f%s", kvp.getKey(), kvp.getValue(), "%\n");
        }
    }

    /**
     * Create the histogram chart of the parallel mode with the dataset given.
     *
     * @param result_reduce result of the reduce phase in sequential mode
     */
    private static void createBarChart(List<KeyValuePair<Character, Float>> result_reduce) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                BarChart example = new BarChart("Histogram", result_reduce);
                example.setSize(800, 400);
                example.setLocationRelativeTo(null);
                example.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                example.setVisible(true);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            System.out.println("Error creating the histogram chart");
        }
    }

    /**
     * Clean temp folder files
     */
    private static void cleanTemps() {
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
