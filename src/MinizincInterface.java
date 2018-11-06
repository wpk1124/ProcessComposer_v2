
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MinizincInterface {
    public enum SearchMode {
        LogGeneration, StructureIdentification
    }

    public static String prepareDataFile(String inputData, String fileName) {
        String dataFileName = "";
        try {
            dataFileName = fileName;
            PrintWriter writer = new PrintWriter(dataFileName+".dzn", "UTF-8");
            writer.println(inputData);
            writer.close();
        }
        catch (FileNotFoundException fino){
            System.out.println("File not found.");
        }
        catch (UnsupportedEncodingException uoo) {
            System.out.println("Unsupported encoding.");
        }
        return dataFileName;
    }

    public static String runSearch(SearchMode mode, String dataFileName, int numberOfSolutions, int numberOfThreads, boolean outputToFile) {
        StringBuilder output = new StringBuilder();
        try {
            String threadCmd = (numberOfThreads > 1) ? " -p "+numberOfThreads : "";
            String searchModelCmd = (mode == SearchMode.StructureIdentification) ? " structure_id.mzn" : " process.mzn";
            String solutionsCmd = "";
            if (numberOfSolutions == 0)
                solutionsCmd = " -a";
            else if(numberOfSolutions > 1)
                solutionsCmd = " -n "+numberOfSolutions;
            String outputFileName = (outputToFile) ? dataFileName+"_mzn_output" : "";
            String outputCmd = (outputToFile) ? " -o "+outputFileName+".txt" : "";
            String cmd = "minizinc -f fzn-gecode" + threadCmd + searchModelCmd + " -d " + dataFileName+".dzn" + solutionsCmd + outputCmd;
            //System.out.println(cmd);
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec(cmd);
            pr.waitFor();
            BufferedReader bufError = new BufferedReader(new InputStreamReader((pr.getErrorStream())));
            String errorLine = bufError.readLine();
            if(errorLine != null) {
                while (errorLine != null) {
                    output.append(errorLine);
                    output.append("\n");
                    errorLine = bufError.readLine();
                }
                throw new UnsupportedOperationException(output.toString());
            }
            if(outputToFile)
                output.append(outputFileName);
            else {
                BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                String line = buf.readLine();
                while (line != null) {
                    output.append(line);
                    output.append("\n");
                    line = buf.readLine();
                }
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
        catch (UnsupportedOperationException uoe) {
            System.out.println("Error when generating a Workflow Trace:");
            System.out.println(uoe.getMessage());
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

    /*DEPRECATED */
    public static String runSearch(String inputData) {
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
}
