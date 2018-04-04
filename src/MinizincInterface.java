
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MinizincInterface {
    public enum SearchMode {
        LogGeneration, StructureIdentification
    }

    public static String runSearch(SearchMode mode, String inputData, int NumberOfSolutions, int numberOfThreads) {
        StringBuilder output = new StringBuilder();
        try {
            String dataFileName = "data.dzn";
            PrintWriter writer = new PrintWriter(dataFileName, "UTF-8");
            writer.println(inputData);
            writer.close();
            String cmd = "minizinc -f fzn-gecode structure_id.mzn -d "+dataFileName;
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec(cmd);
            pr.waitFor();
            BufferedReader buf = new BufferedReader( new InputStreamReader( pr.getInputStream() ) ) ;
            String line = buf.readLine();
            while ( line  != null) {
                output.append(line+"\n");
                line = buf.readLine();
            }
        }
        catch (FileNotFoundException fino){
            System.out.println("File not found.");
        }
        catch (UnsupportedEncodingException uoo) {
            System.out.println("Unsupported encoding.");
        }
        catch (IOException ioe) {
            System.out.println("IO Exception");
        }
        catch (InterruptedException iue) {
            System.out.println("Interrupted Exception");
        }
        return output.toString();
    }

    public static int[] parseIntArray(String inputString) {
        String[] stringArray = inputString.split(", ");
        int[] result = new int[stringArray.length];
        for(int i = 0; i < stringArray.length; i++) {
            result[i] = Integer.parseInt(stringArray[i]);
        }
        return result;
    }

    public static int[][] parseSquareIntArray(String inputString) {
        String[] stringArray = inputString.split("\\]\\[");
        int[][] result = new int[stringArray.length][stringArray.length];
        for(int i = 0; i < stringArray.length; i++) {
            String stringToParse = stringArray[i].replace("[","").replace("]", "");
            int[] row = parseIntArray(stringToParse);
            for(int j = 0; j < row.length; j++) {
                result[i][j] = row[j];
            }
        }
        return result;
    }
}
