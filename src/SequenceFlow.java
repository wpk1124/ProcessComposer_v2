
public class SequenceFlow {
    private int sequenceFlowID;
    private int startObjectID;
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

    public SequenceFlow(int startObject, int endObject) {
        sequenceFlowID = this.hashCode();
        startObjectID = startObject;
        endObjectID = endObject;
    }

    public SequenceFlow(int startObject, int endObject, String condition) {
        sequenceFlowID = this.hashCode();
        startObjectID = startObject;
        endObjectID = endObject;
        flowCondition = condition;
    }

    public SequenceFlow(FlowObject startObject, FlowObject endObject) {
        sequenceFlowID = this.hashCode();
        startObjectID = startObject.getObjectID();
        endObjectID = endObject.getObjectID();
    }

    public int getsequenceFlowID() {return sequenceFlowID;}
    public int getstartObjectID() {return startObjectID;}
    public int getendObjectID() {return endObjectID;}
    public String getCondition() {return flowCondition;}

    public int getBranchingLevel() {
        return flowCondition.length() - flowCondition.replace(";","").length();
    }
}
