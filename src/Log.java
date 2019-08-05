import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.algorithms.shortestpath.ShortestPath;

import java.util.*;

public class Log {
    private List<Trace> workflowLog;

    public List<Trace> getWorkflowLog() {
        return workflowLog;
    }

    public void setWorkflowLog(List<Trace> workflowLog) {
        this.workflowLog = workflowLog;
    }

    public Log() {
        workflowLog = new ArrayList<>();
    }

    public Log(List<Trace> existingLog) {workflowLog = new ArrayList<>(existingLog);}

    public Log(String fileName) {
        SequenceReader.readTxtFile(fileName+".txt");
    }

    public void addTrace(Trace trace) {
        workflowLog.add(new Trace(trace));
    }

    public void removeTrace(Trace trace) {workflowLog.remove(trace);}



    public int getSize() {return workflowLog.size();}

    public String printWorkflowLog() {
        int i = 1;
        String printout = "Workflow log\n";
        for(Trace t : workflowLog) {
            printout += ("Trace " + i + ": " + t.printWorkflowTrace() + ".\n");
            i++;
        }
        return printout;
    }

    public boolean parallelExecution(String taskA, String taskB) {
        int oldIndexDiff = 0;
        for(Trace t1 : workflowLog) {
            List<String> logTasks = t1.getWorkflowTrace();
            int startIndex = logTasks.indexOf(taskA);
            int endIndex = logTasks.indexOf(taskB);
            if((startIndex >= 0) && (endIndex >= 0)) {
                if(oldIndexDiff*(startIndex-endIndex) < 0) {
                    return true;
                }
                oldIndexDiff = startIndex-endIndex;
            }
        }
        return false;
    }

    public boolean parallelExecution(ProcessGraph bpGraph, String taskA, String taskB) {
        //if both task can be found in the same cycle on bpGraph then they are not parallel - method to test!
        boolean executedInParallel = false;
        int oldIndexDiff = 0;
        for(Trace t1 : workflowLog) {
            List<String> logTasks = t1.getWorkflowTrace();
            int startIndex = logTasks.indexOf(taskA);
            int endIndex = logTasks.indexOf(taskB);
            if((startIndex >= 0) && (endIndex >= 0)) {
                if(oldIndexDiff*(startIndex-endIndex) < 0) {
                    executedInParallel = true;
                    break;
                }
                oldIndexDiff = startIndex-endIndex;
            }
        }
        if(executedInParallel) {
            ProcessGraph tempGraph = new ProcessGraph(bpGraph);
            DijkstraShortestPath path = new DijkstraShortestPath(tempGraph);
            List edges = path.getPath(tempGraph.getFlowObject(taskA), tempGraph.getFlowObject(taskB));
            if(edges.size() > 0) {
                for (Object e : edges) {
                    SequenceFlow edgeToRemove = (SequenceFlow) e;
                    tempGraph.removeEdge(edgeToRemove);
                }
                path = new DijkstraShortestPath(tempGraph);
                if (path.getPath(tempGraph.getFlowObject(taskB), tempGraph.getFlowObject(taskA)).size() > 0)
                    executedInParallel = false;
            }
        }
        return executedInParallel;
    }

}
