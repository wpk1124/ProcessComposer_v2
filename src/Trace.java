import java.util.ArrayList;
import java.util.List;

public class Trace {
    private List<String> workflowTrace;
    private int endEventID;

    public int getEndEventID() {return endEventID;}

    public void setEndEventID(String eventIDString) {
        try {
            endEventID = Integer.parseInt(eventIDString);
        }
        catch (NumberFormatException nfe) {
            endEventID = 1;
        }
    }

    public List<String> getWorkflowTrace() {
        return workflowTrace;
    }
    public void setWorkflowTrace(List<String> workflowTrace) {
        this.workflowTrace = workflowTrace;
    }

    public Trace() {
        workflowTrace = new ArrayList<>();
    }

    public Trace(Trace t) {
        workflowTrace = new ArrayList<>(t.getWorkflowTrace());
        endEventID = t.getEndEventID();
    }

    public int countOccurrences(String task) {
        int count = 0;
        for(String s : workflowTrace) {
            if(s.equals(task))
                count++;
        }
        return count;
    }


    public void addEvent(String event) {
        workflowTrace.add(event);
    }


    public String printWorkflowTrace() {
        String printout = "";
        for(String event : workflowTrace)
            printout += (event+"-");
        return printout;
    }


}
