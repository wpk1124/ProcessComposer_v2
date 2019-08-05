import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProcessSpecification {
    //required
    private String mainPool;
    private List<DataEntity> initialState;
    private List<DataEntity> goalState;

    //optional
    private List<List<DataEntity>> errorStates;
    private List<ActivityDescription> activityDescriptions;
    private List<DataEntity> dataEntities;
    private List<String> participants;
    private List<String> collaborators;

    public String getMainPool() { return mainPool; }
    public List<String> getCollaborators() {return collaborators;}
    public List<String> getParticipants() {return participants;}

    private ProcessSpecification() {
        errorStates = new ArrayList<>();
        activityDescriptions = new ArrayList<>();
        dataEntities = new ArrayList<>();
        participants = new ArrayList<>();
        collaborators = new ArrayList<>();
    }

    //disused
    public ProcessSpecification(String pool, List<DataEntity> init, List<DataEntity> goal) {
        this();
        mainPool = pool;
        initialState = init;
        goalState = goal;

        //add initial state
        dataEntities.addAll(init);

        //add final state
        for(DataEntity de : goal) {
            addDataEntityIfNotExists(de);
        }
    }

    public ProcessSpecification(String pool, List<DataEntity> init, List<DataEntity> goal, List<ActivityDescription> descriptions) {
        this();
        mainPool = pool;
        initialState = init;
        goalState = goal;

        //add initial state
        if(init.size() > 0)
            dataEntities.addAll(init);

        //add final state
        for(DataEntity de : goal) {
            addDataEntityIfNotExists(de);
        }

        importActivityDescriptions(descriptions);
    }

    public ProcessSpecification (String requirementsFileName) {
        this();
        try {
            File inputFile = new File(requirementsFileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            mainPool = doc.getElementsByTagName("mainPool").item(0).getTextContent();
            initialState = parseDataEntitiesFromXML(doc.getElementsByTagName("initialState").item(0).getChildNodes());
            goalState = parseDataEntitiesFromXML(doc.getElementsByTagName("goalState").item(0).getChildNodes());

            //error states should be managed as Closed World Assumption (DE not indicated -> forbidden), goal state on the contrary
            NodeList errorStateList = doc.getElementsByTagName("errorStates").item(0).getChildNodes();
            for(int i = 0; i < errorStateList.getLength(); i++) {
                if(errorStateList.item(i).getChildNodes().getLength() > 0)
                    errorStates.add(parseDataEntitiesFromXML(errorStateList.item(i).getChildNodes()));
            }

            //add initial state
            if(initialState.size() > 0)
                dataEntities.addAll(initialState);

            //add final state
            for(DataEntity de : goalState) {
                addDataEntityIfNotExists(de);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ActivityDescription getActivityByName(String activityName) {
        for(ActivityDescription ad : activityDescriptions) {
            if(ad.getActivityName().equals(activityName))
                return ad;
        }
        return null;
    }

    private static List<DataEntity> parseDataEntitiesFromXML(NodeList deNodes) {
        List<DataEntity> dataEntitiesToParse = new ArrayList<>();
        for(int i = 0; i < deNodes.getLength(); i++) {
            Node currentNode = deNodes.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element dataEntity = (Element) deNodes.item(i);
                dataEntitiesToParse.add(new DataEntity(dataEntity.getTextContent(), dataEntity.getAttribute("type"), dataEntity.getAttribute("inputSource"), dataEntity.getAttribute("routing")));
            }
        }
        return dataEntitiesToParse;
    }

    private boolean containsDataEntity(DataEntity newDE) {
        return DataEntity.isMember(newDE, dataEntities);
    }

    private boolean containsActivity(ActivityDescription newAD) {
        for(ActivityDescription storedAD : activityDescriptions) {
            if(storedAD.getActivityName().equals(newAD.getActivityName()))
                return true;
        }
        return false;
    }

    private void addDataEntityIfNotExists(DataEntity de) {
        if (!containsDataEntity(de))
            dataEntities.add(de);
    }

    private void addDataEntitiesIfNotExist(List<DataEntity> newDEs) {
        for(DataEntity de : newDEs)
            addDataEntityIfNotExists(de);
    }

    private void addActivityDescriptionIfNotExists(ActivityDescription ad) {
        if(!containsActivity(ad))
            activityDescriptions.add(ad);
    }

    private void addColaboratorIfNotExists(String collaborator) {
        if(!collaborators.contains(collaborator))
            collaborators.add(collaborator);
    }

    private void addParticipantIfNotExists(String collaborator) {
        if(!participants.contains(collaborator))
            participants.add(collaborator);
    }

    public void importActivityDescriptions(List<ActivityDescription> descriptions) {
        for(ActivityDescription ad : descriptions) {
            addActivityDescriptionIfNotExists(ad);

            //add refined data entities to the repository
            addDataEntitiesIfNotExist(ad.getAllUsedDEsNoDistinct());
            addColaboratorIfNotExists(ad.getCollaborator());
        }
    }

    public void importActivityDescriptionsFromDirectory(String dirName) {
        File folder = new File(dirName);
        File[] listOfFiles = folder.listFiles();
        try {
            for (File f : listOfFiles) {
                if (f.isFile() && f.getName().matches("^activity_.*\\.xml$"))
                    importActivityDescriptionFromXML(dirName+"/"+f.getName());
            }
        }
        catch (NullPointerException nupe) {
            System.out.println("No files in directory "+dirName+".");
        }

    }

    public void importActivityDescriptionFromXML(String fileName) {
        try {
            File inputFile = new File(fileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            String activityName = doc.getElementsByTagName("activityName").item(0).getTextContent();
            List<DataEntity> reqDE = parseDataEntitiesFromXML(doc.getElementsByTagName("requiredDataEntities").item(0).getChildNodes());
            List<DataEntity> crDE = parseDataEntitiesFromXML(doc.getElementsByTagName("createdDataEntities").item(0).getChildNodes());
            List<DataEntity> forbDE = parseDataEntitiesFromXML(doc.getElementsByTagName("forbiddenDataEntities").item(0).getChildNodes());
            List<DataEntity> delDE = parseDataEntitiesFromXML(doc.getElementsByTagName("deletedDataEntities").item(0).getChildNodes());
            String maxRepetitions = doc.getElementsByTagName("maxNumberOfRepetitions").item(0).getTextContent();
            Integer maxNumberOfRepetitions = (maxRepetitions.isEmpty() ? 1 : Integer.parseInt(maxRepetitions));

            ActivityDescription ad = new ActivityDescription(activityName, reqDE, crDE, forbDE, delDE, maxNumberOfRepetitions);
            ad.setParticipant(doc.getElementsByTagName("participant").item(0).getTextContent());
            ad.setCollaborator(doc.getElementsByTagName("collaborator").item(0).getTextContent());

            addActivityDescriptionIfNotExists(ad);

            //add refined data entities to the repository
            addDataEntitiesIfNotExist(ad.getAllUsedDEsNoDistinct());
            addParticipantIfNotExists(ad.getParticipant());
            addColaboratorIfNotExists(ad.getCollaborator());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addErrorState(List<DataEntity> error) {
        errorStates.add(error);
        addDataEntitiesIfNotExist(error);
    }

    public void addErrorStates(List<List<DataEntity>> errors) {
        for (List<DataEntity> error: errors) {
            addErrorState(error);
            addDataEntitiesIfNotExist(error);
        }
    }

    private Integer[] defineInitStateArray(List<DataEntity> deList) {
        Integer[] initStateVector = new Integer[dataEntities.size()];
        int i = 0;
        for(DataEntity de : dataEntities) {
            if(DataEntity.isMember(de, deList))
                initStateVector[i] = 1;
            else
                initStateVector[i] = 0;
            i++;
        }
        return initStateVector;
    }

    private Integer[] defineFinalStateArray(List<DataEntity> deList) {
        Integer[] initStateVector = new Integer[dataEntities.size()];
        int i = 0;
        //forbidden outcomes not defined
        for(DataEntity de : dataEntities) {
            if(DataEntity.isMember(de, deList))
                initStateVector[i] = 1;
            else
                initStateVector[i] = -1;
            i++;
        }
        return initStateVector;
    }

    private Integer[] defineFinalStateArray(List<DataEntity> deList, List<List<DataEntity>> errorStateList) {
        Integer[] initStateVector = new Integer[dataEntities.size()*(1+errorStateList.size())];
        int i = 0;
        //forbidden outcomes not defined
        for(DataEntity de : dataEntities) {
            if(DataEntity.isMember(de, deList))
                initStateVector[i] = 1;
            else
                initStateVector[i] = -1;
            i++;
        }
        for(List<DataEntity> errorState : errorStateList) {
            for(DataEntity de : dataEntities) {
                if(DataEntity.isMember(de, errorState))
                    initStateVector[i] = 1;
                else
                    initStateVector[i] = 0;
                i++;
            }
        }
        return initStateVector;
    }


    private Integer[] defineTaskConditions() {
        int i = 0;
        Integer[] taskConditions = new Integer[activityDescriptions.size()*dataEntities.size()];
        for(ActivityDescription ad : activityDescriptions) {
            for(DataEntity de : dataEntities) {
                if(DataEntity.isMember(de, ad.getRequiredDEsByInterfaceMode(DataEntity.interfaceMode.internal)))
                    taskConditions[i] = 1;
                else if(DataEntity.isMember(de, ad.getForbiddenDEs()))
                    taskConditions[i] = 0;
                else
                    taskConditions[i] = -1;
                i++;

                //debug
                //System.out.println("index: " + i + " AD: " + ad.getActivityName() + " DE: " + de.getName() + " value: " + taskConditions[i-1]);
            }
        }
        return taskConditions;
    }

    private Integer[] defineTaskEffects() {
        int i = 0;
        Integer[] taskEffects = new Integer[activityDescriptions.size()*dataEntities.size()];
        for(ActivityDescription ad : activityDescriptions) {
            for(DataEntity de : dataEntities) {
                if(DataEntity.isMember(de, ad.getCreatedDEsByInterfaceMode(DataEntity.interfaceMode.internal)))
                    taskEffects[i] = 1;
                else if(DataEntity.isMember(de, ad.getDeletedDEs()))
                    taskEffects[i] = 0;
                else
                    taskEffects[i] = -1;
                i++;
            }
        }
        return taskEffects;
    }

    private Integer[] defineExecutionVector() {
        Integer[] executionVector = new Integer[activityDescriptions.size()];
        int i = 0;
        for(ActivityDescription ad : activityDescriptions) {
            executionVector[i] = activityDescriptions.get(i).getMaxNumberOfRepetitions();
            i++;
        }
        return executionVector;
    }

    private String generateMiniZincIntArray(Integer[] intArray) {
        StringBuilder intArrayString = new StringBuilder();
        intArrayString.append("[");
        for(Integer i : intArray) {
            intArrayString.append(i.toString());
            intArrayString.append(",");
        }
        //remove last coma
        intArrayString.setLength(intArrayString.length()-1);
        intArrayString.append("];\n");
        return intArrayString.toString();
    }

    public String generateMinizincData() {
        activityDescriptions.sort(ActivityDescription.comparator);
        dataEntities.sort(DataEntity.comparator);
        String fileName = "process_data.dzn";
        //define initial state
        Integer[] initStateVector = defineInitStateArray(initialState);
        Integer[] goalStateVector = defineFinalStateArray(goalState, errorStates);
        //define MTE, MTC, eT
        Integer[] taskConditions = defineTaskConditions();
        Integer[] taskEffects = defineTaskEffects();
        Integer[] executionVector = defineExecutionVector();

        //generate MiniZinc file  example to be validated!!!
        StringBuilder fileData = new StringBuilder();
        //1. Data Entities
        fileData.append("input_states_names = [");
        for(DataEntity de : dataEntities) {
            fileData.append("\"");
            fileData.append(de.getName());
            fileData.append("\",");
        }
        //remove last coma
        fileData.setLength(fileData.length()-1);
        fileData.append("];\n");

        //2. Activities
        fileData.append("input_tasks_names = [");
        for(ActivityDescription ad : activityDescriptions) {
            fileData.append("\"");
            fileData.append(ad.getActivityName());
            fileData.append("\",");
        }
        //remove last coma
        fileData.setLength(fileData.length()-1);
        fileData.append("];\n");

        //3. Task Conditions
        fileData.append("input_tasks_conditions = ");
        fileData.append(generateMiniZincIntArray(taskConditions));

        //4. Task Effects
        fileData.append("input_tasks_effects = ");
        fileData.append(generateMiniZincIntArray(taskEffects));

        //5. Initial State
        fileData.append("input_initial_state =");
        fileData.append(generateMiniZincIntArray(initStateVector));

        //6. Goal State
        fileData.append("input_goal_state =");
        fileData.append(generateMiniZincIntArray(goalStateVector));

        //7. Task executions
        fileData.append("input_tasks_executions =");
        fileData.append(generateMiniZincIntArray(executionVector));

        try {
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            writer.println(fileData.toString());
            writer.close();
        }
        catch (FileNotFoundException fino){
            System.out.println("File not found.");
        }
        catch (UnsupportedEncodingException uoo) {
            System.out.println("Unsupported encoding.");
        }

        return fileName;
    }
}
