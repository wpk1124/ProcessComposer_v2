import java.util.ArrayList;
import java.util.List;

public class Trace {
    private List<String> workflowTrace;

    public Trace() {
        workflowTrace = new ArrayList<>();
    }

    public Trace(Trace t) {workflowTrace = new ArrayList<>(t.getWorkflowTrace());}

    public void addEvent(String event) {
        workflowTrace.add(event);
    }

    public List<String> getWorkflowTrace() {
        return workflowTrace;
    }

    public void setWorkflowTrace(List<String> workflowTrace) {
        this.workflowTrace = workflowTrace;
    }

    public String printWorkflowTrace() {
        String printout = "";
        for(String event : workflowTrace)
            printout += (event+"-");
        return printout;
    }

    public int countOccurrences(String task) {
        int count = 0;
        for(String s : workflowTrace) {
            if(s.equals(task))
                count++;
        }
        return count;
    }

}
