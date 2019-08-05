import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by piotr on 07.11.2017.
 */

public class Console {
    public static void main(final String[] args) {
        String dznFileName = "bpmn-overview-example-Zdzn";
        String logFileName = MinizincInterface.runSearch(MinizincInterface.SearchMode.LogGeneration, dznFileName, 0, 4, true);
        System.out.println(logFileName);
        System.out.println("Composing BPMN process");
        Log log = SequenceReader.readTxtFile(logFileName+".txt");
        ProcessGraph BPgraph = ProcessComposer.ComposeBP(log);
        BPgraph.printGraph(logFileName+"-bpgraph.png");
        System.out.println("Process graph saved as " + logFileName + "-bpgraph.png");
        BPMNWriter.writeBPMNXML(BPgraph, logFileName+"-model.bpmn", true);
        System.out.println("BPMN model saved as " + logFileName + "-model.bpmn");
        XESWriter.writeXESFile(log, logFileName + "-log.xes", false);

        /*  DDR as-is

        //System.out.println("Insert name of the log file (without extension):");
        ProcessSpecification spec = new ProcessSpecification("activities/order_dr/order_process_req.xml");
        spec.importActivityDescriptionsFromDirectory("activities/order_dr");
        spec.generateMinizincData();
        //end test spec
        Scanner reader = new Scanner(System.in);
        //System.out.println("Insert name of the MiniZinc Data file (without extension):");
        //String dznFileName = reader.nextLine();
        String dznFileName = "process_data";
        System.out.println("Reading DZN file...");
        String logFileName = MinizincInterface.runSearch(MinizincInterface.SearchMode.LogGeneration, dznFileName, 0, 4, true);
        //System.out.println(logFileName);
        Log log = SequenceReader.readTxtFile(logFileName+".txt");
        boolean looping = true;
        if(log != null)
            System.out.println("Log file loaded");
        else
            looping = false;

        while (looping) {
            System.out.println("\n************************************************");
            System.out.println("Choose action:");
            System.out.println("1 - write XES file");
            System.out.println("2 - compose BPMN process");
            System.out.println("3 - compose BPMN process in debug mode");
            System.out.println("0 - exit");
            try {
                int selection = 3;//Integer.parseInt(reader.nextLine());
                switch (selection) {
                    case 1:
                        System.out.println("Writing XES file");
                        XESWriter.writeXESFile(log, logFileName + "-log.xes", false);
                        System.out.println("File saved as " + logFileName + "-log.xes");
                        break;
                    case 2:
                    case 3:
                        System.out.println("Composing BPMN process");
                        ProcessGraph BPgraph = ProcessComposer.ComposeBP(log, spec);
                        if(selection == 3) {
                            BPgraph.printGraph(logFileName+"-bpgraph.png");
                            System.out.println("Process graph saved as " + logFileName + "-bpgraph.png");
                        }
                        BPMNWriter.writeBPMNXML(BPgraph, logFileName+"-model.bpmn", true);
                        BPMNWriter.writeBPMNXML(BPgraph,spec, logFileName+"-model.bpmn",  true);
                        System.out.println("BPMN model saved as " + logFileName + "-model.bpmn");
                        break;
                    case 0:
                        System.out.println("See you again!");
                        looping = false;
                        break;
                    default:
                        System.out.println("Incorrect choice");
                        break;
                }
                //debug
                looping = false;
                //end debug
            }
            catch (NumberFormatException nfe) {
                System.out.println("Incorrect choice");
                nfe.printStackTrace();
                break;
            }


        }

                    */
    }
}
