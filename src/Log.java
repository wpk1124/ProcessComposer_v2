import java.util.*;

public class Log {
    private List<Trace> workflowLog;

    public Log() {
        workflowLog = new ArrayList<>();
    }

    public Log(List<Trace> existingLog) {workflowLog = new ArrayList<>(existingLog);}

    public void addTrace(Trace trace) {
        workflowLog.add(new Trace(trace));
    }

    public void removeTrace(Trace trace) {workflowLog.remove(trace);}

    public List<Trace> getWorkflowLog() {
        return workflowLog;
    }

    public void setWorkflowLog(List<Trace> workflowLog) {
        this.workflowLog = workflowLog;
    }

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

}
