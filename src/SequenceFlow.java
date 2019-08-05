
public class SequenceFlow {
    private int sequenceFlowID;
    //disused
    private int startObjectID;
    //disused
    private int endObjectID;
    private String flowCondition;

    public SequenceFlow() {
        sequenceFlowID = this.hashCode();
        flowCondition = "";
    }

    public SequenceFlow(String condition) {
        sequenceFlowID = this.hashCode();
        flowCondition = condition;
    }

    //disused
    public SequenceFlow(int startObject, int endObject) {
        sequenceFlowID = this.hashCode();
        startObjectID = startObject;
        endObjectID = endObject;
    }

    //disused
    public SequenceFlow(int startObject, int endObject, String condition) {
        sequenceFlowID = this.hashCode();
        startObjectID = startObject;
        endObjectID = endObject;
        flowCondition = condition;
    }

    //disused
    public SequenceFlow(FlowObject startObject, FlowObject endObject) {
        sequenceFlowID = this.hashCode();
        startObjectID = startObject.getObjectID();
        endObjectID = endObject.getObjectID();
    }

    public int getsequenceFlowID() {return sequenceFlowID;}
    //disused
    public int getstartObjectID() {return startObjectID;}
    //disused
    public int getendObjectID() {return endObjectID;}
    public String getCondition() {return flowCondition;}

    //disused
    public int getBranchingLevel() {
        return flowCondition.length() - flowCondition.replace(";","").length();
    }
}
