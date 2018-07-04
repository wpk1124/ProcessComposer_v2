import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by piotr on 07.11.2017.
 */

public class Console {
    public static void main(final String[] args) {
        System.out.println("Insert name of the log file (without extension):");
        Scanner reader = new Scanner(System.in);
        String fileName = reader.nextLine();
        System.out.println("Reading TXT file...");
        Log log = SequenceReader.readTxtFile(fileName+".txt");
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
                int selection = Integer.parseInt(reader.nextLine());
                switch (selection) {
                    case 1:
                        System.out.println("Writing XES file");
                        XESWriter.writeXESFile(log, fileName + "-log.xes", false);
                        System.out.println("File saved as " + fileName + "-log.xes");
                        break;
                    case 2:
                    case 3:
                        System.out.println("Composing BPMN process");
                        ProcessGraph BPgraph = ProcessComposer.ComposeBP(log);
                        if(selection == 3) {
                            BPgraph.printGraph(fileName+"-bpgraph.png");
                            System.out.println("Process graph saved as " + fileName + "-bpgraph.png");
                        }
                        BPMNWriter.writeBPMNXML(BPgraph, fileName+"-model.bpmn", false);
                        System.out.println("BPMN model saved as " + fileName + "-model.bpmn");
                        break;
                    case 0:
                        System.out.println("See you again!");
                        looping = false;
                        break;
                    default:
                        System.out.println("Incorrect choice");
                        break;
                }
            }
            catch (NumberFormatException nfe) {
                System.out.println("Incorrect choice");
            }

        }
    }
}
