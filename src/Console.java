import java.util.ArrayList;
import java.util.List;

/**
 * Created by piotr on 07.11.2017.
 */

public class Console {
    public static void main(final String[] args) {
        System.out.println("Reading TXT file");
        String fileName = "kkl2-project";

        Log log = SequenceReader.readTxtFile(fileName+"-output.txt");
        //System.out.println(log.printWorkflowLog());
/*
        List<Log> logList = ProcessComposer.groupTraces(log);
        List<ProcessGraph> graphs = new ArrayList<>();
        int i=0;

        for(Log l : logList) {
            graphs.add(ProcessComposer.buildProcessGraph(l));
            graphs.get(i).printGraph("component" + ++i + ".png");
            System.out.println("Trace group " + i);
            System.out.println(l.printWorkflowLog());
        }

        ProcessGraph merged = new ProcessGraph(false);
        i=0;
        for(ProcessGraph g : graphs) {
            merged = ProcessComposer.mergeGraphs(merged, g);
            merged.printGraph("merged" + ++i + ".png");
        }

        merged.printGraph("merged123.png");

        ProcessGraph BPgraph = ProcessComposer.createBPGraph(merged);
        BPgraph.printGraph("bpgraph.png");*/



//Z TEGO KORZYSTAC
        ProcessGraph BPgraph = ProcessComposer.ComposeBP(log);

        BPgraph.printGraph(fileName+"-bpgraph.png");

        BPMNWriter.writeBPMNXML(BPgraph, fileName+"-model.bpmn", false);

        System.out.println("Writing XES file");
        XESWriter.writeXESFile(log, fileName+"-log.xes", false);
    }
}
