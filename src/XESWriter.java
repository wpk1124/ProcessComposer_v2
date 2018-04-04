import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class XESWriter {
    public static void writeXESFile(Log workflowLog, String fileName) {
        String myFavouriteDate = "2017-05-29";
        try {
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<log xes.version=\"1.0\" xmlns=\"http://www.xes-standard.org\" xes.creator=\"PW-DDR\">");
            writer.println("<extension name=\"Concept\" prefix=\"concept\" uri=\"http://www.xes-standard.org/concept.xesext\"/>");
            writer.println("<extension name=\"Lifecycle\" prefix=\"lifecycle\" uri=\"http://www.xes-standard.org/lifecycle.xesext\"/>");
            writer.println("<extension name=\"Time\" prefix=\"time\" uri=\"http://www.xes-standard.org/time.xesext\"/>");
            writer.println("<global scope=\"trace\">");
            writer.println("<string key=\"concept:name\" value=\"name\"/>");
            writer.println("<string key=\"variant\" value=\"string\"/>");
            writer.println("<int key=\"variant-index\" value=\"0\"/>");
            writer.println("</global>");
            writer.println("<global scope=\"event\">");
            writer.println("<string key=\"concept:name\" value=\"name\"/>");
            writer.println("<string key=\"lifecycle:transition\" value=\"transition\"/>");
            writer.println("<date key=\"time:timestamp\" value=\""+myFavouriteDate+"T21:08:59.999+02:00\"/>");
            //writer.println("<string key=\"Column_2\" value=\"string\"/>");
            writer.println("</global>");
            //writer.println("<classifier name=\"Activity\" keys=\"Column_2\"/>");
            writer.println("<string key=\"lifecycle:model\" value=\"standard\"/>");
            writer.println("<string key=\"creator\" value=\"Fluxicon Disco\"/>");
            writer.println("<string key=\"library\" value=\"Process Composer\"/>");
            int i = 1, j;
            for(Trace t : workflowLog.getWorkflowLog()){
                writer.println("<trace>");
                writer.println("<string key=\"concept:name\" value=\""+i+"\"/>");
                writer.println("<string key=\"variant\" value=\"Variant "+i+"\"/>");
                writer.println("<int key=\"variant-index\" value=\""+i+"\"/>");
                writer.println("<string key=\"creator\" value=\"Process Composer\"/>");
                j = 10;
                for(String s : t.getWorkflowTrace()) {
                    writer.println("<event>");
                    writer.println("<string key=\"concept:name\" value=\""+s+"\"/>");
                    writer.println("<string key=\"lifecycle:transition\" value=\"complete\"/>");
                    writer.println("<date key=\"time:timestamp\" value=\""+myFavouriteDate+"T21:"+j+":00.000+02:00\"/>");
                    //writer.println("<string key=\"Column_2\" value="+s+"/>");
                    writer.println("</event>");
                    j++;
                }
                writer.println("<event>");
                writer.println("<string key=\"concept:name\" value=\"*END*\"/>");
                writer.println("<string key=\"lifecycle:transition\" value=\"complete\"/>");
                writer.println("<date key=\"time:timestamp\" value=\""+myFavouriteDate+"T21:"+j+":00.000+02:00\"/>");
                //writer.println("<string key=\"Column_2\" value="+s+"/>");
                writer.println("</event>");
                writer.println("</trace>");
                i++;
            }
            writer.println("</log>");
            writer.close();
        }
        catch (FileNotFoundException fino){
            System.out.println("File not found.");
        }
        catch (UnsupportedEncodingException uoo) {
            System.out.println("Unsupported encoding.");
        }
    }
}
