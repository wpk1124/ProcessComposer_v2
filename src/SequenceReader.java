import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class SequenceReader {
    public static Log readTxtFile(String fileName) {
        try {
            FileInputStream fstream = new FileInputStream(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            Log workflowLog = new Log();
            //throw exception in case when there is an empty log
            while ((strLine = br.readLine()) != null) {
                if(strLine.startsWith("=====UNSATISFIABLE====="))
                    throw new UnsupportedOperationException("Error: The provided process specification is inconsistent.");
                Trace trace = new Trace();
                if(strLine.startsWith("\"")) {
                    for(String s : strLine.split(",")) {
                        if(s.matches("\".+\""))
                            trace.addEvent(s.replace("\"",""));
                        else if(s.startsWith("FSID:"))
                            trace.setEndEventID(s.replace("FSID:",""));
                    }
                    workflowLog.addTrace(trace);
                }
            }
            br.close();
            return workflowLog;
        }
        catch(IOException ioo) {
            System.out.println("Log file not found.");
            return null;
        }
        catch (UnsupportedOperationException uoe) {
            System.out.println(uoe.getMessage());
        }
        return null;
    }
}
