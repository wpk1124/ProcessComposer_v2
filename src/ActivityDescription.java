import org.apache.commons.collections15.ListUtils;

import javax.xml.crypto.Data;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

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
    private List<DataEntity> forbiddenDEs = new ArrayList<>(); //czy optional?
    private List<DataEntity> deletedDEs = new ArrayList<>();
    private String collaborator;
    private int maxNumberOfRepetitions = 1;

    //to be developed
    private String boundaryEvents;

    public String getActivityName() {
        return activityName;
    }

    public List<DataEntity> getCreatedDEs() {
        return createdDEs;
    }

    public List<DataEntity> getCreatedDEsByInterfaceMode(DataEntity.interfaceMode mode) {
        List<DataEntity> result = new ArrayList<>();
        for(DataEntity de : createdDEs) {
            if(de.getDestination() == mode)
                result.add(de);
        }
        return result;
    }

    public List<DataEntity> getDeletedDEs() {
        return deletedDEs;
    }

    public List<DataEntity> getForbiddenDEs() {
        return forbiddenDEs;
    }

    public List<DataEntity> getRequiredDEs() {
        return requiredDEs;
    }

    public List<DataEntity> getRequiredDEsByInterfaceMode(DataEntity.interfaceMode mode) {
        List<DataEntity> result = new ArrayList<>();
        for(DataEntity de : requiredDEs) {
            if(de.getSource() == mode)
                result.add(de);
        }
        return result;
    }

    public int getMaxNumberOfRepetitions() {return maxNumberOfRepetitions;}

    public List<DataEntity> getAllUsedDEsNoDistinct() {
        List<DataEntity> dataEntityList = new ArrayList<>();
        for(DataEntity de : createdDEs) {
            if(de.getDestination() == DataEntity.interfaceMode.internal)
                dataEntityList.add(de);
        }
        for(DataEntity de : requiredDEs) {
            if(de.getSource() == DataEntity.interfaceMode.internal)
                dataEntityList.add(de);
        }
//        dataEntityList.addAll(createdDEs);
//        dataEntityList.addAll(requiredDEs);
        dataEntityList.addAll(deletedDEs);
        dataEntityList.addAll(forbiddenDEs);

        return dataEntityList;
    }

    public String getCollaborator() {
        return collaborator;
    }
    public void setCollaborator(String actCollaborator) {collaborator = actCollaborator;}

    public String getParticipant() {return participant;}
    public void setParticipant(String actParticipant) {participant = actParticipant;}

    public ActivityDescription(String name, List<DataEntity> reqDEs, List<DataEntity> creDEs) {
        activityName = name;
        requiredDEs = reqDEs;
        createdDEs = creDEs;
        activityType = Type.Task;
    }

    public ActivityDescription(String name, List<DataEntity> reqDEs, List<DataEntity> creDEs, int repetitions) {
        this(name, reqDEs, creDEs);
        maxNumberOfRepetitions = repetitions;
    }

    public ActivityDescription(String name, List<DataEntity> reqDEs, List<DataEntity> creDEs,  List<DataEntity> forbDEs, List<DataEntity> delDEs, int repetitions) {
        this(name, reqDEs, creDEs, repetitions);
        forbiddenDEs = forbDEs;
        deletedDEs = delDEs;
    }

    public static Comparator<ActivityDescription> comparator = new Comparator<>() {
        @Override
        public int compare(ActivityDescription o1, ActivityDescription o2) {
            return o1.getActivityName().compareTo(o2.getActivityName());
        }
    };

}
