import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import org.apache.commons.collections15.CollectionUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Flow;


public class ProcessGraph extends DirectedSparseGraph<FlowObject, SequenceFlow> {

    public ProcessGraph(boolean createStartEvent) {
        if(createStartEvent)
            this.addVertex(new FlowObject("Start", FlowObject.FlowObjectType.StartEvent));
    };

    public ProcessGraph(ProcessGraph sourceGraph) {
        for(FlowObject fo : sourceGraph.getVertices())
            this.addNewFlowObject(fo.getObjectName(), fo.getObjectType());
        for(SequenceFlow sf : sourceGraph.getEdges()) {
            Pair<FlowObject> endObjects = sourceGraph.getEndpoints(sf);
            this.addNewSequenceFlow(endObjects.getFirst().getObjectName(), endObjects.getSecond().getObjectName());
        }
    }

    public FlowObject getFlowObject(String name) {
        for(FlowObject o : this.getVertices()) {
            if(o.getObjectName().equals(name))
                return o;
        }
        return null;
    }

    public Collection<String> getFlowObjectNames() {
        Collection<String> flowObjectNames = new ArrayList<>();
        for(FlowObject v : this.getVertices()) {
            flowObjectNames.add(v.getObjectName());
        }
        return flowObjectNames;
    }

    public FlowObject getStartEvent() {
        for(FlowObject v : this.getVertices()) {
            if(v.getObjectType() == FlowObject.FlowObjectType.StartEvent)
                return v;
        }
        return null;
    }

    public Collection<Pair<FlowObject>> getSequenceFlowEndpoints() {
        Collection<Pair<FlowObject>> endpoints = new ArrayList<>();
        for(SequenceFlow e : this.getEdges()) {
            endpoints.add(this.getEndpoints(e));
        }
        return endpoints;
    }

    public Collection<Pair<String>> getSequenceFlowEndpointNames() {
        Collection<Pair<String>> endpointNames = new ArrayList<>();
        for(SequenceFlow e : this.getEdges()) {
            Pair<String> endpoints = new Pair<>(this.getSource(e).getObjectName(), this.getDest(e).getObjectName());
            endpointNames.add(endpoints);
        }
        return endpointNames;
    }

    public SequenceFlow getSequenceFlow(String startObjectName, String endObjectName) {
        FlowObject start = getFlowObject(startObjectName), end = getFlowObject(endObjectName);
        return findEdge(start, end);
    }

    public SequenceFlow getSequenceFlow(Pair<String> endObjectNames) {
        FlowObject start = getFlowObject(endObjectNames.getFirst()), end = getFlowObject(endObjectNames.getSecond());
        return findEdge(start, end);
    }

    public FlowObject addNewFlowObject(String objectName, FlowObject.FlowObjectType objectType) {
        if(this.getFlowObject(objectName) == null) {
            FlowObject o = new FlowObject(objectName, objectType);
            this.addVertex(o);
            return o;
        }
        else
            return null;
    }

    public SequenceFlow addNewSequenceFlow(String startObjectName, String endObjectName) {
        FlowObject start = getFlowObject(startObjectName), end = getFlowObject(endObjectName);
        if(findEdge(start, end) == null) {
            //create new sequence flow
            SequenceFlow s = new SequenceFlow();
            addEdge(s, start, end);
            return s;
        }
        else
            return null;
    }

    public SequenceFlow addNewSequenceFlow(String startObjectName, String endObjectName, String flowCondition) {
        FlowObject start = getFlowObject(startObjectName), end = getFlowObject(endObjectName);
        if(findEdge(start, end) == null) {
            //create new sequence flow with condition
            SequenceFlow s = new SequenceFlow(flowCondition);
            addEdge(s, start, end);
            return s;
        }
        else
            return null;
    }

    public boolean insertGateway(Collection<FlowObject> sourceSet, FlowObject destinationObject, FlowObject.FlowObjectType gatewayType) {
        String destObjectName = destinationObject.getObjectName();
        FlowObject gateway = new FlowObject(gatewayType,  destObjectName);
        //add merge gateway
        if(sourceSet.size() > 1) {
            this.addVertex(gateway);
            for(FlowObject v : sourceSet) {
                this.addNewSequenceFlow(v.getObjectName(), gateway.getObjectName());
                SequenceFlow edgeToRemove = this.getSequenceFlow(v.getObjectName(), destObjectName);
                this.removeEdge(edgeToRemove);
            }
            this.addNewSequenceFlow(gateway.getObjectName(), destObjectName);
            return true;
        }
        else
            return false;
    }

    public boolean insertMergeGatewayStructure(GatewayStructure structure, List<Pair<String>> inEdges, FlowObject destinationObject) {
        String destObjectName = destinationObject.getObjectName();
        List<FlowObject> gateways = new ArrayList<>();
        List<Pair<String>> edgesToRemove = new ArrayList<>();
        int numOfGateways = structure.getGatewayTypes().length;
        if(inEdges.size() > 1) {
            for(int gatewayType : structure.getGatewayTypes()) {
                if(gatewayType == -1)
                    gateways.add(new FlowObject(FlowObject.FlowObjectType.XORjoin));
                else if(gatewayType == 1)
                    gateways.add(new FlowObject(FlowObject.FlowObjectType.ANDjoin));
                else
                    gateways.add(new FlowObject(FlowObject.FlowObjectType.DummyObject));
            }
            for(FlowObject g : gateways) {
                if(FlowObject.gateways.contains(g.getObjectType()))
                    this.addVertex(g);
            }

            for(int i = 0; i < inEdges.size(); i++) {
                String startObjectName = inEdges.get(i).getFirst();
                FlowObject directSuccessor = this.getDirectSuccessor(startObjectName);
                //check if the start object has a gateway successor
                if(directSuccessor != null && FlowObject.splitGateways.contains(directSuccessor.getObjectType())) {
                    this.addNewSequenceFlow(directSuccessor.getObjectName(), gateways.get(structure.getEdgeGateways()[i]-1).getObjectName());
                    edgesToRemove.add(new Pair<>(directSuccessor.getObjectName(), destObjectName));
                }
                else {
                    this.addNewSequenceFlow(startObjectName, gateways.get(structure.getEdgeGateways()[i] - 1).getObjectName());
                    edgesToRemove.add(new Pair<>(startObjectName, destObjectName));
                }
            }
            int[][] gatewayConnections = structure.getGatewayConnections();
            for(int i = 0; i < numOfGateways; i++) {
                for(int j = 0; j < numOfGateways; j++) {
                    if(gatewayConnections[i][j] == 1)
                        this.addNewSequenceFlow(gateways.get(i).getObjectName(), gateways.get(j).getObjectName());
                        //this.addNewSequenceFlow(gateways.get(structure.getEdgeGateways()[i]-1).getObjectName(), gateways.get(structure.getEdgeGateways()[j]-1).getObjectName());
                }
            }
            this.addNewSequenceFlow(gateways.get(structure.getOutputGateway()-1).getObjectName(), destObjectName);

            for(Pair<String> endObjects : edgesToRemove) {
                this.removeEdge(this.getSequenceFlow(endObjects));
            }
            return true;
        }
        else
            return false;
    }

    public boolean insertSplitGatewayStructure(GatewayStructure structure, List<Pair<String>> outEdges, FlowObject sourceObject) {
        String sourceObjectName = sourceObject.getObjectName();
        List<FlowObject> gateways = new ArrayList<>();
        int numOfGateways = structure.getGatewayTypes().length;
        if(outEdges.size() > 1) {
            for(int gatewayType : structure.getGatewayTypes()) {
                if(gatewayType == -1)
                    gateways.add(new FlowObject(FlowObject.FlowObjectType.XORsplit));
                else if(gatewayType == 1)
                    gateways.add(new FlowObject(FlowObject.FlowObjectType.ANDsplit));
                else
                    gateways.add(new FlowObject(FlowObject.FlowObjectType.DummyObject));
            }
            for(FlowObject g : gateways) {
                if(FlowObject.gateways.contains(g.getObjectType()))
                    this.addVertex(g);
            }
            for(int i = 0; i < outEdges.size(); i++) {
                String endObjectName = outEdges.get(i).getSecond();
                this.addNewSequenceFlow(gateways.get(structure.getEdgeGateways()[i]-1).getObjectName(), endObjectName);
            }
            int[][] gatewayConnections = structure.getGatewayConnections();
            for(int i = 0; i < numOfGateways; i++) {
                for(int j = 0; j < numOfGateways; j++) {
                    if(gatewayConnections[i][j] == -1)
                        this.addNewSequenceFlow(gateways.get(i).getObjectName(), gateways.get(j).getObjectName());
                        //this.addNewSequenceFlow(gateways.get(structure.getEdgeGateways()[i]-1).getObjectName(), gateways.get(structure.getEdgeGateways()[j]-1).getObjectName());
                }
            }
            this.addNewSequenceFlow(sourceObjectName, gateways.get(structure.getOutputGateway()-1).getObjectName());

            for(Pair<String> endObjects : outEdges) {
                this.removeEdge(this.getSequenceFlow(endObjects));
            }
            return true;
        }
        else
            return false;
    }


    public boolean insertGateway(FlowObject sourceObject, Collection<FlowObject> destinationSet, FlowObject.FlowObjectType gatewayType) {
        String sourceObjectName = sourceObject.getObjectName();
        FlowObject gateway = new FlowObject(gatewayType, sourceObjectName);
        //add split gateway
        if(destinationSet.size() > 1) {
            this.addVertex(gateway);
            this.addNewSequenceFlow(sourceObjectName, gateway.getObjectName());
            for(FlowObject v: destinationSet) {
                this.addNewSequenceFlow(gateway.getObjectName(), v.getObjectName());
                SequenceFlow edgeToRemove = this.getSequenceFlow(sourceObjectName, v.getObjectName());
                this.removeEdge(edgeToRemove);
            }
            return true;
        }
        else
            return false;
    }

    public FlowObject getDirectSuccessor(String objectName) {
        FlowObject vertex = this.getFlowObject(objectName), successor = null;
        if(vertex != null) {
            Object[] successors = this.getSuccessors(vertex).toArray();
            if(successors.length == 1)
                successor =  (FlowObject) successors[0];
        }
        return successor;
    }

    public String getMaxBranchingLevel() {
        int branchingLevel = 0;
        StringBuilder edgeCondition = new StringBuilder("");
        for(SequenceFlow e : this.getEdges()) {
            if(branchingLevel < e.getBranchingLevel())
                branchingLevel = e.getBranchingLevel();
        }
        for(int i=0; i < branchingLevel; i++) {
            edgeCondition.append("A;");
        }
        return edgeCondition.toString();
    }

    public int getMaxBranchingLevel(Collection<SequenceFlow> edges) {
        int branchingLevel = 0;
        for(SequenceFlow e : edges) {
            if(branchingLevel < e.getBranchingLevel())
                branchingLevel = e.getBranchingLevel();
        }
        return branchingLevel;
    }

    public boolean branchingLevelsConsistent(Collection<SequenceFlow> edges) {
        boolean consistent = false;
        int i=0;
        Integer[] branchingLevels = new Integer[edges.size()];
        for(SequenceFlow e : edges) {
            branchingLevels[i] = e.getBranchingLevel();
            i++;
        }
        if(Arrays.stream(branchingLevels).distinct().count() == 1)
            consistent = true;

        return consistent;
    }

    public boolean parallelExecution(FlowObject taskA, FlowObject taskB, Log workflowLog) {
        return workflowLog.parallelExecution(this, taskA.getObjectName(), taskB.getObjectName());
    }

    public boolean parallelExecution(SequenceFlow flowA, SequenceFlow flowB, Log workflowLog) {
        FlowObject sourceA = this.getSource(flowA), sourceB = this.getSource(flowB);
        FlowObject destA = this.getDest(flowA), destB = this.getDest(flowB);
        if(sourceA.getObjectID() == sourceB.getObjectID())
            return parallelExecution(destA, destB, workflowLog);
        else if(destA.getObjectID() == destB.getObjectID())
            return parallelExecution(sourceA, sourceB, workflowLog);
        else
            return false;
    }

    public int[][] getEdgeRelations(List<SequenceFlow> edges, Log workflowLog) {
        int edgeCount = edges.size();
        int[][] edgeRelations = new int[edgeCount][edgeCount];
        for(int i = 0; i < edgeCount; i++) {
            for(int j = 0; j < edgeCount; j++) {
                if(i == j)
                    edgeRelations[i][j] = 0;
                else if(this.parallelExecution(edges.get(i), edges.get(j), workflowLog))
                    edgeRelations[i][j] = 1;
                else
                    edgeRelations[i][j] = -1;
            }
        }
        return edgeRelations;
    }

    public Collection<FlowObject> getSuccessors(Collection<FlowObject> flowObjects) {
        Collection<FlowObject> successors = new ArrayList<>();
        for(FlowObject v : flowObjects) {
            successors.addAll(this.getSuccessors(v));
        }
        return successors;
    }

    public void setLayout(Point2D startingPoint) {
        Collection<FlowObject> currentVertices = new ArrayList<>();
        Collection<FlowObject> assignedVertices = new ArrayList<>(this.getVertices());
        FlowObject startEvent = this.getStartEvent();
        startEvent.setCoordinates(startingPoint);
        assignedVertices.remove(startEvent);
        currentVertices.add(startEvent);
        Double defaultXSpacing = 120.0, defaultYSpacing = 80.0;
        Double xOffset = defaultXSpacing, yOffset;

        do {
            currentVertices = this.getSuccessors(currentVertices);
            int i=0;
            yOffset = 0.0;
            for(FlowObject v : CollectionUtils.intersection(currentVertices, assignedVertices)) {
                if(i>0) {
                    yOffset += defaultYSpacing;
                }
                v.setCoordinates(startingPoint.getX() + xOffset, startingPoint.getY() + yOffset);
                assignedVertices.remove(v);
                i++;
            }
            xOffset += defaultXSpacing;
        }
        while ((this.getSuccessors(currentVertices) != null) && assignedVertices.size() > 0);
    }


    public void printGraph(String fileName) {
        Layout<FlowObject, SequenceFlow> graphLayout = new ISOMLayout<>(this);
        VisualizationViewer<FlowObject, SequenceFlow> vv = new VisualizationViewer<>(graphLayout);
        VisualizationImageServer<FlowObject, SequenceFlow> vis = new VisualizationImageServer<>(graphLayout, graphLayout.getSize());
        vis.setBackground(Color.gray);

        RenderContext<FlowObject, SequenceFlow> context = vis.getRenderContext();

        //context.setEdgeLabelTransformer(new ToStringLabeller<>());
        context.setEdgeShapeTransformer(new EdgeShape.Line<>());
        /*context.setEdgeLabelTransformer(new ToStringLabeller() {
            public String transform(Object v) {
                return ((SequenceFlow)v).getCondition();
            }
        }); */

        context.setVertexLabelTransformer(new ToStringLabeller() {
            public String transform(Object v) {
                return ((FlowObject)v).getObjectName();
            }
        });
        vis.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);



        BufferedImage image = (BufferedImage) vis.getImage(
                new Point2D.Double(graphLayout.getSize().getWidth() / 2,
                        graphLayout.getSize().getHeight() / 2),
                        new Dimension(graphLayout.getSize())
        );

        File outputFile = new File(fileName);
        try{
            ImageIO.write(image, "png", outputFile);
        }
        catch (IOException e) {
            System.out.println("Cannot write file " + fileName + ".");
        }
    }

}
