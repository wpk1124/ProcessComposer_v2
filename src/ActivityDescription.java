import javax.xml.crypto.Data;
import java.util.List;

public class ActivityDescription {
    public enum Type {
        Task
    }

    //mandatory elements
    private String activityName;
    private List<DataEntity> requiredDEs;
    private List<DataEntity> createdDEs;

    //optional elements
    private Type activityType;
    private String participant;
    private List<DataEntity> forbiddenDEs; //czy optional?
    private List<DataEntity> deletedDEs;
    private int maxNumberOfRepetitions;


    //to be developed
    private String boundaryEvents;

    public ActivityDescription(String name, List<DataEntity> reqDEs, List<DataEntity> creDEs) {
        requiredDEs = reqDEs;
        createdDEs = creDEs;
        activityType = Type.Task;
        maxNumberOfRepetitions = 1;
    }

}
