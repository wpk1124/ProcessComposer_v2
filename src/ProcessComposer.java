import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import org.apache.commons.collections15.CollectionUtils;

import java.util.*;
import java.util.concurrent.Flow;

public class ProcessComposer {

    public static ProcessGraph ComposeBP(Log workflowlog) {
        ProcessGraph componentProcess = new ProcessGraph(true);
        for(Trace t : workflowlog.getWorkflowLog()) {
            List<String> tasks = new ArrayList<>(t.getWorkflowTrace());
            //add first task
            String firstTask = tasks.get(0);
            componentProcess.addNewFlowObject(firstTask, FlowObject.FlowObjectType.Task);
            componentProcess.addNewSequenceFlow("Start", firstTask);
            tasks.remove(firstTask);
            //add the other tasks
            for (String task : tasks) {
                componentProcess.addNewFlowObject(task, FlowObject.FlowObjectType.Task);
                componentProcess.addNewSequenceFlow(firstTask, task);
                firstTask = task;
            }
            //add end event
            componentProcess.addNewFlowObject("End", FlowObject.FlowObjectType.EndEvent);
            componentProcess.addNewSequenceFlow(firstTask, "End");
        }

        //refine edges
        Collection<SequenceFlow> edgesToRemove = new ArrayList<>();
        /*for(SequenceFlow edge : componentProcess.getEdges()) {
            Pair<FlowObject> endPoints;
            endPoints = componentProcess.getEndpoints(edge);
            String startTaskName = endPoints.getFirst().getObjectName();
            String endTaskName = endPoints.getSecond().getObjectName();
            SequenceFlow oppositeEdge = componentProcess.findEdge(endPoints.getSecond(), endPoints.getFirst());
            boolean doubledEdges = (oppositeEdge != null);
            boolean parallelExecution = workflowlog.parallelExecution(startTaskName, endTaskName);
            if(doubledEdges && parallelExecution) {
                edgesToRemove.add(edge);
                edgesToRemove.add(oppositeEdge);
            }
        } */

        for(SequenceFlow edge : componentProcess.getEdges()) {
            Pair<FlowObject> endPoints;
            boolean doubledEdges = false, executedEquinumerously  = false, parallelExecution = false;
            endPoints = componentProcess.getEndpoints(edge);
            String startTaskName = endPoints.getFirst().getObjectName();
            String endTaskName = endPoints.getSecond().getObjectName();
            SequenceFlow oppositeEdge = componentProcess.findEdge(endPoints.getSecond(), endPoints.getFirst());
            doubledEdges = (oppositeEdge != null);
            int oldIndexDiff = 0;
            for(Trace t1 : workflowlog.getWorkflowLog()) {
                List<String> logTasks = t1.getWorkflowTrace();
                int startIndex = logTasks.indexOf(startTaskName);
                int endIndex = logTasks.indexOf(endTaskName);
                int startOccurrences = t1.countOccurrences(startTaskName), endOccurrences = t1.countOccurrences(endTaskName);
                if(startOccurrences > 0 && startOccurrences == endOccurrences )
                    executedEquinumerously = true;
                if(oldIndexDiff*(startIndex-endIndex) < 0)
                    parallelExecution = true;
                oldIndexDiff = startIndex-endIndex;
            }
            if(doubledEdges && executedEquinumerously && parallelExecution) {
                edgesToRemove.add(edge);
                edgesToRemove.add(oppositeEdge);
            }
        }

        for(SequenceFlow e : edgesToRemove)
            componentProcess.removeEdge(e);

        //DEBUG - print intermediate model
        //ProcessGraph toPrint = new ProcessGraph(componentProcess);
        //toPrint.printGraph("bpgraph-nogateways.png");

        ProcessGraph BPGraph = new ProcessGraph(componentProcess);
        //add split gateways (out edges)
        for(FlowObject originalVertex : componentProcess.getVertices()) {
            Collection<FlowObject> successors = componentProcess.getSuccessors(originalVertex);
            List<SequenceFlow> outEdges = new ArrayList<>(componentProcess.getOutEdges(originalVertex));
            int outEdgesCount = outEdges.size();
            if(outEdgesCount == 2 && FlowObject.tasksAndEvents.contains(originalVertex.getObjectType())) {
                if(componentProcess.parallelExecution(outEdges.get(0), outEdges.get(1), workflowlog))
                    BPGraph.insertGateway(originalVertex, successors, FlowObject.FlowObjectType.ANDsplit);
                else
                    BPGraph.insertGateway(originalVertex, successors, FlowObject.FlowObjectType.XORsplit);
            }
            else if(outEdgesCount > 2 && FlowObject.tasksAndEvents.contains(originalVertex.getObjectType())) {
                //save edges as pairs of their endpoints
                List<Pair<String>> outEdgeEndpoints = new ArrayList<>();
                for(int i = 0; i < outEdgesCount; i++)
                    outEdgeEndpoints.add(new Pair<>(componentProcess.getSource(outEdges.get(i)).getObjectName(), componentProcess.getDest(outEdges.get(i)).getObjectName()));

                BPGraph.insertSplitGatewayStructure(identifyGateways(outEdges, componentProcess.getEdgeRelations(outEdges, workflowlog)), outEdgeEndpoints, originalVertex);
            }
        }

        ProcessGraph intermediateGraph = new ProcessGraph(BPGraph);
        intermediateGraph.printGraph("bpgraph-splitonly.png");

        //add merge gateways (in edges)
        for(FlowObject originalVertex : componentProcess.getVertices()) {
            //intermediateGraph - merged graph with split gateways
            FlowObject intermediateVertex = intermediateGraph.getFlowObject(originalVertex.getObjectName());
            Collection<FlowObject> predecessors = intermediateGraph.getPredecessors(intermediateVertex);
            List<SequenceFlow> inEdges = new ArrayList<>(componentProcess.getInEdges(originalVertex));
            int inEdgesCount = inEdges.size();

            if(inEdgesCount == 2 && FlowObject.tasksAndEvents.contains(originalVertex.getObjectType())) {
                if(componentProcess.parallelExecution(inEdges.get(0), inEdges.get(1), workflowlog))
                    BPGraph.insertGateway(predecessors, originalVertex, FlowObject.FlowObjectType.ANDjoin);
                else
                    BPGraph.insertGateway(predecessors, originalVertex, FlowObject.FlowObjectType.XORjoin);
            }
            else if(inEdgesCount > 2 && FlowObject.tasksAndEvents.contains(originalVertex.getObjectType())) {
                //save edges as pairs of their endpoints
                List<Pair<String>> inEdgeEndpoints = new ArrayList<>();
                for(int i = 0; i < inEdgesCount; i++)
                    inEdgeEndpoints.add(new Pair<>(componentProcess.getSource(inEdges.get(i)).getObjectName(), componentProcess.getDest(inEdges.get(i)).getObjectName()));
                BPGraph.insertMergeGatewayStructure(identifyGateways(inEdges, componentProcess.getEdgeRelations(inEdges, workflowlog)), inEdgeEndpoints, originalVertex);
            }
        }

        return BPGraph;
    }

    private static GatewayStructure identifyGateways(List<SequenceFlow> edges, int[][] edgesRelations) {
        StringBuilder inputData = new StringBuilder();
        inputData.append("input_edges = [");
        for(int i=0; i < edges.size(); i++) {
            inputData.append("\""+edges.get(i).getsequenceFlowID()+"\",");
        }
        inputData.deleteCharAt(inputData.length()-1);
        inputData.append("];\n");
        inputData.append("input_edges_relations = [");
        for(int i=0; i < edges.size(); i++) {
            for(int j = 0; j < edges.size(); j++)
                inputData.append(edgesRelations[i][j]+",");
        }
        inputData.deleteCharAt(inputData.length()-1);
        inputData.append("];");
        String dataFileName = MinizincInterface.prepareDataFile(inputData.toString(), "structure_data");
        String result = MinizincInterface.runSearch(MinizincInterface.SearchMode.StructureIdentification, dataFileName, 1, 1, false);
        String[] outputs = result.split("\\r?\\n");
        return new GatewayStructure(MinizincInterface.parseIntArray(outputs[0]), MinizincInterface.parseSquareIntArray(outputs[1]), MinizincInterface.parseIntArray(outputs[2]), Integer.parseInt(outputs[3]));
    }


// Disused methods
    public static List<Log> groupTraces(Log workflowLog) {
        //copy of the initial workflow log and its list of traces
        Log logSet = new Log(workflowLog.getWorkflowLog());
        List<Trace> traceList = logSet.getWorkflowLog();
        //declare list of log groups
        List<Log> logGroups = new ArrayList<>();

        while(traceList.size() > 0) {
            Log group = new Log();
            Trace firstTrace  = traceList.get(0);
            traceList.remove(firstTrace);
            group.addTrace(firstTrace);
            for(Trace t : new ArrayList<>(traceList)) {
                if(listEqualsIgnoreOrder(firstTrace.getWorkflowTrace(), t.getWorkflowTrace())) {
                    group.addTrace(t);
                    traceList.remove(t);
                }
            }
            logGroups.add(group);
        }
        int k=0;
        for(Log l : logGroups) {
            System.out.println("Group "+ ++k);
            System.out.println(l.printWorkflowLog());
        }
        return logGroups;
    }


    public static ProcessGraph buildProcessGraph(Log groupedLog) {
        ProcessGraph componentProcess = new ProcessGraph(true);
        for(Trace t : groupedLog.getWorkflowLog()) {
            List<String> tasks = t.getWorkflowTrace();
            //add first task
            String firstTask = tasks.get(0);
            componentProcess.addNewFlowObject(firstTask, FlowObject.FlowObjectType.Task);
            componentProcess.addNewSequenceFlow("Start", firstTask);
            tasks.remove(firstTask);
            //add the other tasks
            for (String task : tasks) {
                componentProcess.addNewFlowObject(task, FlowObject.FlowObjectType.Task);
                componentProcess.addNewSequenceFlow(firstTask, task);
                firstTask = task;
            }
            //add end event
            componentProcess.addNewFlowObject("End", FlowObject.FlowObjectType.EndEvent);
            componentProcess.addNewSequenceFlow(firstTask, "End");
        }
        //DEBUG - print intermediate model
//        ProcessGraph toPrint = new ProcessGraph(componentProcess);
//        toPrint.printGraph("intermediate.png");
        //refine edges
        Collection<SequenceFlow> edgesToRemove = new ArrayList<>();
        for(SequenceFlow edge : componentProcess.getEdges()) {
            Pair<FlowObject> endPoints;
            boolean doubledEdges = false, executedOnce = false, parallelExecution = false;
            endPoints = componentProcess.getEndpoints(edge);
            String startTaskName = endPoints.getFirst().getObjectName();
            String endTaskName = endPoints.getSecond().getObjectName();
            SequenceFlow oppositeEdge = componentProcess.findEdge(endPoints.getSecond(), endPoints.getFirst());
            doubledEdges = (oppositeEdge != null);
            int oldIndexDiff = 0;
            for(Trace t1 : groupedLog.getWorkflowLog()) {
                List<String> logTasks = t1.getWorkflowTrace();
                int startIndex = logTasks.indexOf(startTaskName);
                int endIndex = logTasks.indexOf(endTaskName);
                //if(startIndex == logTasks.lastIndexOf(startTaskName)
                //    && endIndex == logTasks.lastIndexOf(endTaskName))
                //    executedOnce = true;
                //else
                //    executedOnce = false;
                //if(executedOnce && (oldIndexDiff*(startIndex-endIndex) < 0))
                if(oldIndexDiff*(startIndex-endIndex) < 0)
                    parallelExecution = true;
                oldIndexDiff = startIndex-endIndex;
            }
            if(doubledEdges && parallelExecution) {
                edgesToRemove.add(edge);
                edgesToRemove.add(oppositeEdge);
            }
        }
        for(SequenceFlow e : edgesToRemove)
            componentProcess.removeEdge(e);

        return componentProcess;
    }

    public static ProcessGraph mergeGraphs(ProcessGraph graphA, ProcessGraph graphB) {
        if(graphA.getVertexCount() == 0)
            return graphB;
        if (graphB.getVertexCount() == 0)
            return graphA;
        ProcessGraph mergedGraph = new ProcessGraph(false);
        for(String s : CollectionUtils.union(graphA.getFlowObjectNames(), graphB.getFlowObjectNames())) {
            if(graphA.getFlowObject(s) != null)
                mergedGraph.addVertex(graphA.getFlowObject(s));
            else
                mergedGraph.addVertex(graphB.getFlowObject(s));
        }
        for(Pair<String> endpoints : CollectionUtils.intersection(graphA.getSequenceFlowEndpointNames(), graphB.getSequenceFlowEndpointNames())) {
            String currentCondition = graphA.getSequenceFlow(endpoints.getFirst(), endpoints.getSecond()).getCondition();
            mergedGraph.addEdge(new SequenceFlow(currentCondition), graphA.getFlowObject(endpoints.getFirst()), graphA.getFlowObject(endpoints.getSecond()));
        }

        //get new branching level
        StringBuilder edgeCondition = new StringBuilder("A;");
        if(graphA.getMaxBranchingLevel().length() > graphB.getMaxBranchingLevel().length())
            edgeCondition.append(graphA.getMaxBranchingLevel());
        else
            edgeCondition.append(graphB.getMaxBranchingLevel());


        // add first set difference with appropriate information
        for(Pair<String> endpoints : CollectionUtils.subtract(graphA.getSequenceFlowEndpointNames(), graphB.getSequenceFlowEndpointNames())) {
            String currentCondition = graphA.getSequenceFlow(endpoints.getFirst(), endpoints.getSecond()).getCondition();
            mergedGraph.addNewSequenceFlow(endpoints.getFirst(), endpoints.getSecond(), edgeCondition.toString());
        }
        for(Pair<String> endpoints : CollectionUtils.subtract(graphB.getSequenceFlowEndpointNames(), graphA.getSequenceFlowEndpointNames())) {
            String currentCondition = graphB.getSequenceFlow(endpoints.getFirst(), endpoints.getSecond()).getCondition();
            mergedGraph.addNewSequenceFlow(endpoints.getFirst(), endpoints.getSecond(), edgeCondition.toString());
        }
        return mergedGraph;
    }
    public static ProcessGraph createBPGraph(ProcessGraph mergedGraph) {
        ProcessGraph BPGraph = new ProcessGraph(mergedGraph);
        //add split gateways
        for(FlowObject originalVertex : mergedGraph.getVertices()) {
            Collection<FlowObject> successors = mergedGraph.getSuccessors(originalVertex);
            Collection<SequenceFlow> inEdges = mergedGraph.getInEdges(originalVertex);
            Collection<SequenceFlow> outEdges = mergedGraph.getOutEdges(originalVertex);

            int inLevel = mergedGraph.getMaxBranchingLevel(inEdges);
            int outLevel = mergedGraph.getMaxBranchingLevel(outEdges);
            boolean alternativeFlow = (inLevel != outLevel) || !mergedGraph.branchingLevelsConsistent(outEdges);
            //add split gateways
            if(successors.size() > 1) {
                if(alternativeFlow)
                    BPGraph.insertGateway(originalVertex, successors, FlowObject.FlowObjectType.XORsplit);
                else
                    BPGraph.insertGateway(originalVertex, successors, FlowObject.FlowObjectType.ANDsplit);
            }
        }

        ProcessGraph intermediateGraph = new ProcessGraph(BPGraph);

        //add merge gateways
        for(FlowObject originalVertex : mergedGraph.getVertices()) {
            //intermediateGraph - merged graph with split gateways
            FlowObject intermediateVertex = intermediateGraph.getFlowObject(originalVertex.getObjectName());
            Collection<FlowObject> predecessors = intermediateGraph.getPredecessors(intermediateVertex);

            Collection<SequenceFlow> inEdges = mergedGraph.getInEdges(originalVertex);
            Collection<SequenceFlow> outEdges = mergedGraph.getOutEdges(originalVertex);
            int inLevel = mergedGraph.getMaxBranchingLevel(inEdges);
            int outLevel = mergedGraph.getMaxBranchingLevel(outEdges);
            boolean alternativeFlow = (inLevel != outLevel) || !mergedGraph.branchingLevelsConsistent(inEdges);

            //add merge gateways
            if(predecessors.size() > 1) {
                if(alternativeFlow)
                    BPGraph.insertGateway(predecessors, originalVertex, FlowObject.FlowObjectType.XORjoin);
                else
                    BPGraph.insertGateway(predecessors, originalVertex, FlowObject.FlowObjectType.ANDjoin);
            }
        }

        return BPGraph;
    }

    private static <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }
}