import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Client {
    public static void main(String[] args) {
        for (String arg : args) {
            BufferedReader br = null;
            try {
                //Input of the program
                System.out.println("Reading file " + arg);
                br = new BufferedReader(new FileReader(arg));
                String line;
                while ((line = br.readLine()) != null) {
                    //Executing the map phase
                    MapTask mapTask = new MapTask(line);
                    Thread thread = new Thread(mapTask);
                    thread.start();
                }

                System.out.println("Finished reading file " + arg);
            } catch (IOException e) {
                System.out.println("File not found, skipping");
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        System.out.println("Cannot close the buffered reader");
                    }
                }
            }
            //shuffle



            //reduce
        }

    }
}
