import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import javafx.util.Pair;
import org.w3c.dom.*;

import javax.xml.crypto.Data;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class BPMNWriter {
    public static void writeBPMNXML(ProcessGraph processGraph, ProcessSpecification processSpec, String fileName, boolean withBPMNDI) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element rootElement = doc.createElement("definitions");
            rootElement.setAttribute("xmlns", "http://www.omg.org/spec/BPMN/20100524/MODEL");
            rootElement.setAttribute("xmlns:dc", "http://www.omg.org/spec/DD/20100524/DC");
            rootElement.setAttribute("xmlns:bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI");
            rootElement.setAttribute("xmlns:di","http://www.omg.org/spec/DD/20100524/DI");
            rootElement.setAttribute("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");
            rootElement.setAttribute("targetNamespace","http://www.omg.org/bpmn20");
            rootElement.setAttribute("exporter","PW");
            rootElement.setAttribute("xsi:schemaLocation","http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd");
            doc.appendChild(rootElement);

            //collaboration header
            Element collaboration = doc.createElement("collaboration");
            String collaborationUID = generateIDfromHashCode(collaboration.hashCode());
            collaboration.setAttribute("id", collaborationUID);
            collaboration.setIdAttribute("id", true);
            rootElement.appendChild(collaboration);

            //process part
            Element process = doc.createElement("process");
            String processUID = generateIDfromHashCode(processGraph.hashCode());
            process.setAttribute("id", processUID);
            process.setIdAttribute("id", true);
            //process.setAttribute("isClosed", "false");
            //process.setAttribute("isExecutable", "false");
            process.setAttribute("name", processGraph.getMainPool());
            //process.setAttribute("processType", "none");
            rootElement.appendChild(process);

            //lane set
            //!!! - include only lanes which are used in the process
            List<String> processLanes = processSpec.getParticipants();
            Element laneSet = doc.createElement("laneSet");
            laneSet.setAttribute("id", generateIDfromHashCode(processLanes.hashCode()));
            laneSet.setIdAttribute("id", true);
            process.appendChild(laneSet);
            //lanes
            for(String laneName : processLanes) {
                if(!laneName.isEmpty()) {
                    Element newLane = doc.createElement("lane");
                    newLane.setAttribute("id", generateIDfromHashCode(laneName.hashCode()));
                    newLane.setIdAttribute("id", true);
                    newLane.setAttribute("name", laneName);
                    laneSet.appendChild(newLane);
                }
            }

            //flow objects
            for(FlowObject v : processGraph.getVertices()) {
                FlowObject.FlowObjectType objectType = v.getObjectType();
                String flowObjectUID = generateIDfromHashCode(v.getObjectID());
                Element newElement, laneElement = null;
                switch (objectType) {
                    case StartEvent:
                        newElement = doc.createElement("startEvent");
                        //a jezeli nastepnik jest bramka to sie wysypie :( -- trzeba zrobic funkcje, ktora zwroci najblizszy task? - do dopisania w rozdziale 6
                        laneElement = doc.getElementById(getLaneUID(processGraph.getClosestActivityName(v.getObjectName(), true), processSpec));
                        break;
                    case EndEvent:
                        newElement = doc.createElement("endEvent");
                        laneElement = doc.getElementById(getLaneUID(processGraph.getClosestActivityName(v.getObjectName(), false), processSpec));
                        break;
                    case Task:
                        newElement = doc.createElement("task");
                        laneElement = doc.getElementById(getLaneUID(v.getObjectName(), processSpec));
                        break;
                    case XORsplit: case XORjoin: case ANDjoin: case ANDsplit:
                        String gatewayType = "", gatewayDirection = "";
                        if(objectType == FlowObject.FlowObjectType.XORjoin || objectType == FlowObject.FlowObjectType.XORsplit)
                            gatewayType = "exclusive";
                        else
                            gatewayType = "parallel";
                        if(objectType == FlowObject.FlowObjectType.XORjoin || objectType == FlowObject.FlowObjectType.ANDjoin) {
                            gatewayDirection = "Converging";
                            laneElement = doc.getElementById(getLaneUID(processGraph.getClosestActivityName(v.getObjectName(), true), processSpec));
                        }
                        else {
                            gatewayDirection = "Diverging";
                            laneElement = doc.getElementById(getLaneUID(processGraph.getClosestActivityName(v.getObjectName(), false), processSpec));
                        }
                        newElement = doc.createElement(gatewayType+"Gateway");
                        newElement.setAttribute("gatewayDirection", gatewayDirection);
                        for(SequenceFlow e : processGraph.getInEdges(v)) {
                            Element incoming = doc.createElement("incoming");
                            incoming.appendChild(doc.createTextNode(generateIDfromHashCode(e.hashCode())));
                            newElement.appendChild(incoming);
                        }
                        for(SequenceFlow e : processGraph.getOutEdges(v)) {
                            Element outgoing = doc.createElement("outgoing");
                            outgoing.appendChild(doc.createTextNode(generateIDfromHashCode(e.hashCode())));
                            newElement.appendChild(outgoing);
                        }
                        break;
                    default:
                        newElement = doc.createElement("extensionElements");
                        break;
                }
                newElement.setAttribute("id", generateIDfromHashCode(v.getObjectID()));
                newElement.setIdAttribute("id", true);
                newElement.setAttribute("name", v.getObjectName());
                process.appendChild(newElement);
                if(laneElement != null) {
                    Element laneFlowNodeRef = doc.createElement("FlowNodeRef");
                    laneFlowNodeRef.appendChild(doc.createTextNode(flowObjectUID));
                    laneElement.appendChild(laneFlowNodeRef);
                }
            }

            //sequence flows
            for(SequenceFlow e : processGraph.getEdges()) {
                int sourceRef = processGraph.getSource(e).getObjectID();
                int targetRef = processGraph.getDest(e).getObjectID();
                Element sequenceFlow = doc.createElement("sequenceFlow");
                sequenceFlow.setAttribute("id", generateIDfromHashCode(e.hashCode()));
                sequenceFlow.setIdAttribute("id", true);
                //name for partial assignment
                sequenceFlow.setAttribute("name", "");
                sequenceFlow.setAttribute("sourceRef", generateIDfromHashCode(sourceRef));
                sequenceFlow.setAttribute("targetRef", generateIDfromHashCode(targetRef));
                process.appendChild(sequenceFlow);
            }

            //participant definitions
            Element participant = doc.createElement("participant");
            String mainParticipantUID = generateIDfromHashCode(processGraph.getMainPool().hashCode());
            participant.setAttribute("id", mainParticipantUID);
            participant.setIdAttribute("id", true);
            participant.setAttribute("name", processGraph.getMainPool());
            participant.setAttribute("processRef", processUID);
            collaboration.appendChild(participant);

            //additional pools for external collaborators
            for(String collaborator : processSpec.getCollaborators()) {
                if (!collaborator.isEmpty()) {
                    participant = doc.createElement("participant");
                    String newParticipantUID = generateIDfromHashCode(collaborator.hashCode());
                    participant.setAttribute("id", newParticipantUID);
                    participant.setIdAttribute("id", true);
                    participant.setAttribute("name", collaborator);
                    collaboration.appendChild(participant);
                }
            }

            //message flows
            for(FlowObject v : processGraph.getVertices()) {
                ActivityDescription correspondingAD = processSpec.getActivityByName(v.getObjectName());
                if(correspondingAD != null) {
                    String collaboratorName = correspondingAD.getCollaborator();
                    if (!collaboratorName.isEmpty()) {
                        List<DataEntity> externalInputs = correspondingAD.getRequiredDEsByInterfaceMode(DataEntity.interfaceMode.external);
                        DataEntity currentDE;
                        for (int i = 0; i < externalInputs.size(); i++) {
                            currentDE = externalInputs.get(i);
                            collaboration.appendChild(createMessageFlow(doc, currentDE.hashCode(), currentDE.getName(), collaboratorName.hashCode(), v.getObjectID()));
                        }
                        List<DataEntity> externalOutputs = correspondingAD.getCreatedDEsByInterfaceMode(DataEntity.interfaceMode.external);
                        for (int i = 0; i < externalOutputs.size(); i++) {
                            currentDE = externalOutputs.get(i);
                            collaboration.appendChild(createMessageFlow(doc, currentDE.hashCode(), currentDE.getName(), v.getObjectID(), collaboratorName.hashCode()));
                        }
                    }
                }
            }

            if(withBPMNDI) {
                Element bpmndiDiagram = doc.createElement("bpmndi:BPMNDiagram");
                bpmndiDiagram.setAttribute("id", generateIDfromHashCode(bpmndiDiagram.hashCode()));
                bpmndiDiagram.setIdAttribute("id", true);
                rootElement.appendChild(bpmndiDiagram);

                Element bpmnPlane = doc.createElement("bpmndi:BPMNPlane");
                bpmnPlane.setAttribute("bpmnElement", collaborationUID);
                bpmnPlane.setAttribute("id", generateIDfromHashCode(bpmnPlane.hashCode()));
                bpmnPlane.setIdAttribute("id", true);
                bpmndiDiagram.appendChild(bpmnPlane);

                Double minX = 10.0, minY = 10.0, maxX = 1200.0, maxY = 400.0;
                processGraph.setLayout(new Point2D.Double(minX, minY));
                //create main pool
                Element bpmnShape = doc.createElement("bpmndi:BPMNShape");
                bpmnShape.setAttribute("bpmnElement", mainParticipantUID);
                bpmnShape.setAttribute("id", generateIDforGui(mainParticipantUID));
                bpmnShape.setIdAttribute("id", true);
                bpmnShape.setAttribute("isHorizontal", "true");
                Element bounds = doc.createElement("dc:Bounds");
                bounds.setAttribute("x", minX.toString());
                bounds.setAttribute("y", minY.toString());
                bounds.setAttribute("width", maxX.toString());
                bounds.setAttribute("height", maxY.toString());
                bpmnShape.appendChild(bounds);
                bpmnShape.appendChild(doc.createElement("bpmndi:BPMNLabel"));
                bpmnPlane.appendChild(bpmnShape);

                NodeList collaboratorList = doc.getElementsByTagName("participant");
                for(int i=0; i < collaboratorList.getLength(); i++) {
                    Element currentElement = (Element) collaboratorList.item(i);
                    String collaboratorUID = currentElement.getAttributes().getNamedItem("id").getTextContent();
                    if(!currentElement.hasAttribute("processRef")) {
                        bpmnPlane.appendChild(createBPMNshape(doc, collaboratorUID, minX, maxY+(minY*2), maxX, maxY/4, 1));
                    }
                }

                NodeList laneNodeList = doc.getElementsByTagName("lane");
                for(int i=0; i < laneNodeList.getLength(); i++) {
                    String laneUID = laneNodeList.item(i).getAttributes().getNamedItem("id").getTextContent();
                    Double laneY = minY + ((double) i/laneNodeList.getLength())*maxY;
                    Double laneHeight = maxY/laneNodeList.getLength();
                    bpmnPlane.appendChild(createBPMNshape(doc, laneUID, minX, laneY, maxX, laneHeight, 1));
                }

                for (FlowObject v : processGraph.getVertices()) {
                    //Point2D coordinates = graphLayout.transform(v);
                    Point2D coordinates = v.getCoordinates();
                    double width = 25.0, height = 25.0;
                    if (v.getObjectType() == FlowObject.FlowObjectType.Task) {
                        width = 80.0;
                        height = 40.0;
                    }
                    bpmnPlane.appendChild(createBPMNshape(doc,generateIDfromHashCode(v.getObjectID()), coordinates.getX(), coordinates.getY(), width, height, 0));
                }

                for (SequenceFlow e : processGraph.getEdges()) {
                    FlowObject startVertex = processGraph.getSource(e), endVertex = processGraph.getDest(e);
                    Double startX = startVertex.getCoordinates().getX() + startVertex.getWidth() / 2;
                    Double startY = startVertex.getCoordinates().getY() + startVertex.getHeight() / 2;
                    Double endX = endVertex.getCoordinates().getX() + endVertex.getWidth() / 2;
                    Double endY = endVertex.getCoordinates().getY() + endVertex.getHeight() / 2;

                    Element edge = doc.createElement("bpmndi:BPMNEdge");
                    String edgeUID = generateIDfromHashCode(e.hashCode());
                    edge.setAttribute("bpmnElement", edgeUID);
                    edge.setAttribute("id", generateIDforGui(edgeUID));
                    edge.setIdAttribute("id", true);

                    Element waypoint1 = doc.createElement("di:waypoint");
                    waypoint1.setAttribute("x", startX.toString());
                    waypoint1.setAttribute("y", startY.toString());
                    edge.appendChild(waypoint1);

                    Element waypoint2 = doc.createElement("di:waypoint");
                    waypoint2.setAttribute("x", endX.toString());
                    waypoint2.setAttribute("y", endY.toString());
                    edge.appendChild(waypoint2);

                    bpmnPlane.appendChild(edge);
                }

                //message flow gui
                NodeList messageFlowList = doc.getElementsByTagName("messageFlow");
                for(int i=0; i < messageFlowList.getLength(); i++) {
                    Element currentElement = (Element) messageFlowList.item(i);
                    String messageFlowUID = currentElement.getAttributes().getNamedItem("id").getTextContent();
                    String sourceRef = currentElement.getAttributes().getNamedItem("sourceRef").getTextContent();
                    String targetRef = currentElement.getAttributes().getNamedItem("targetRef").getTextContent();
                    Double startX, startY, endX, endY;
                    if(doc.getElementById(targetRef).getTagName().equals("participant")) {
                        FlowObject startVertex = processGraph.getFlowObjectByHashCode(retrieveHashCodefromID(sourceRef));
                        startX = startVertex.getCoordinates().getX() + startVertex.getWidth() / 2;
                        startY = startVertex.getCoordinates().getY() + startVertex.getHeight() / 2;
                        endX = startX;
                        endY = 420.0;
                    }
                    else {
                        FlowObject startVertex = processGraph.getFlowObjectByHashCode(retrieveHashCodefromID(targetRef));
                        endX = startVertex.getCoordinates().getX() + startVertex.getWidth() / 2;
                        endY = startVertex.getCoordinates().getY() + startVertex.getHeight() / 2;
                        startX = endX;
                        startY = 420.0;;
                    }

                    Element edge = doc.createElement("bpmndi:BPMNEdge");
                    edge.setAttribute("bpmnElement", messageFlowUID);
                    edge.setAttribute("id", generateIDforGui(messageFlowUID));
                    edge.setIdAttribute("id", true);

                    Element waypoint1 = doc.createElement("di:waypoint");
                    waypoint1.setAttribute("x", startX.toString());
                    waypoint1.setAttribute("y", startY.toString());
                    edge.appendChild(waypoint1);

                    Element waypoint2 = doc.createElement("di:waypoint");
                    waypoint2.setAttribute("x", endX.toString());
                    waypoint2.setAttribute("y", endY.toString());
                    edge.appendChild(waypoint2);

                    bpmnPlane.appendChild(edge);
                }

            }

            //save results to file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(fileName));
            transformer.transform(source, result);
        }
        catch (ParserConfigurationException pace) {
            System.out.println("Cannot create XML file. Parser error.");
        }
        catch (TransformerException tace) {
            System.out.println("Invalid transformer configuration.");
        }
    }

    private static String generateIDfromHashCode(Integer code) {
        String codeString = (code < 0) ? code.toString().replace("-","0") : code.toString();
        return "xid-"+codeString;
    }

    private static Integer retrieveHashCodefromID(String uid) {
        String codeString = uid.replace("xid-","");
        Integer hashCode = (codeString.startsWith("0")) ? -Integer.parseInt(codeString) : Integer.parseInt(codeString);
        return hashCode;
    }

    private static String generateIDforGui(String uid) {
        return uid+"_gui";
    }


    private static Element createBPMNshape(Document doc, String refUID, Double ox, Double oy, Double width, Double height, Integer horizontalSetting) {
        Element bpmnShape = doc.createElement("bpmndi:BPMNShape");
        bpmnShape.setAttribute("bpmnElement", refUID);
        bpmnShape.setAttribute("id", generateIDforGui(refUID));
        bpmnShape.setIdAttribute("id", true);
        if(horizontalSetting == 1)
            bpmnShape.setAttribute("isHorizontal", "true");
        Element bounds = doc.createElement("dc:Bounds");
        bounds.setAttribute("x", ox.toString());
        bounds.setAttribute("y", oy.toString());
        bounds.setAttribute("width", width.toString());
        bounds.setAttribute("height", height.toString());
        bpmnShape.appendChild(bounds);
        bpmnShape.appendChild(doc.createElement("bpmndi:BPMNLabel"));
        return bpmnShape;
    }

    private static Element createMessageFlow(Document doc, Integer id, String name, Integer sourceRefID, Integer targetRefID) {
        Element messageFlow = doc.createElement("messageFlow");
        messageFlow.setAttribute("id", generateIDfromHashCode(id));
        messageFlow.setIdAttribute("id", true);
        messageFlow.setAttribute("name", name);
        messageFlow.setAttribute("sourceRef", generateIDfromHashCode(sourceRefID));
        messageFlow.setAttribute("targetRef", generateIDfromHashCode(targetRefID));
        return messageFlow;
    }

    private static String getLaneUID(String activityName, ProcessSpecification spec) {
        ActivityDescription activity = spec.getActivityByName(activityName);
        String laneUID = "";
        if(activity != null)
            laneUID = generateIDfromHashCode(activity.getParticipant().hashCode());
        return laneUID;
    }

//deprecated
    public static void writeBPMNXML(ProcessGraph processGraph, String fileName, boolean withBPMNDI) {
        try {
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" targetNamespace=\"http://www.omg.org/bpmn20\" exporter=\"PW\" xsi:schemaLocation=\"http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd\">\n");
            writer.println("<collaboration id=\""+writer.hashCode()+"\">");
            writer.println("<participant id=\""+processGraph.getMainPool().hashCode()+"\" name=\""+processGraph.getMainPool()+"\" processRef=\"proc_"+processGraph.hashCode()+"\" />");
            writer.println("</collaboration>");
            writer.println("<process id=\"proc_"+processGraph.hashCode()+"\" name=\""+processGraph.getMainPool()+"\">");
            for(FlowObject v : processGraph.getVertices()) {
                FlowObject.FlowObjectType objectType = v.getObjectType();

                switch (objectType) {
                    case StartEvent:
                        writer.println("<startEvent id=\""+v.getObjectID()+"\" name=\""+v.getObjectName()+"\"/>");
                        break;
                    case EndEvent:
                        writer.println("<endEvent id=\""+v.getObjectID()+"\" name=\""+v.getObjectName()+"\"/>");
                        break;
                    case Task:
                        writer.println("<task id=\""+v.getObjectID()+"\" name=\""+v.getObjectName()+"\"/>");
                        break;
                    case XORsplit: case XORjoin: case ANDjoin: case ANDsplit:
                        String gatewayType = "", gatewayDirection = "";
                        if(objectType == FlowObject.FlowObjectType.XORjoin || objectType == FlowObject.FlowObjectType.XORsplit)
                            gatewayType = "exclusive";
                        else
                            gatewayType = "parallel";
                        if(objectType == FlowObject.FlowObjectType.XORjoin || objectType == FlowObject.FlowObjectType.ANDjoin)
                            gatewayDirection = "Converging";
                        else
                            gatewayDirection = "Diverging";

                        writer.println("<"+gatewayType+"Gateway id=\""+v.getObjectID()+"\" name=\""+v.getObjectName()+"\" gatewayDirection=\""+gatewayDirection+"\">");
                        for(SequenceFlow e : processGraph.getInEdges(v)) {
                            writer.println("<incoming>");
                            writer.println(e.hashCode());
                            writer.println("</incoming>");
                        }
                        for(SequenceFlow e : processGraph.getOutEdges(v)) {
                            writer.println("<outgoing>");
                            writer.println(e.hashCode());
                            writer.println("</outgoing>");
                        }
                        writer.println("</"+gatewayType+"Gateway>");
                        break;
                        default:
                            break;
                }
            }
            for(SequenceFlow e : processGraph.getEdges()) {
                int sourceRef = processGraph.getSource(e).getObjectID();
                int targetRef = processGraph.getDest(e).getObjectID();
                writer.println("<sequenceFlow id=\""+e.hashCode()+"\" name=\"\" sourceRef=\""+sourceRef+"\" targetRef=\""+targetRef+"\"/>");
            }
            writer.println("</process>");

            //print BPMN DI data
            if(withBPMNDI) {
                writer.println("<bpmndi:BPMNDiagram id=\"id_" + processGraph.hashCode() + "\">");
                writer.println("<bpmndi:BPMNPlane bpmnElement=\"proc_" + processGraph.hashCode() + "\">");
                //Layout<FlowObject, SequenceFlow> graphLayout = new ISOMLayout<>(processGraph);
                //VisualizationViewer<FlowObject, SequenceFlow> vv = new VisualizationViewer<>(graphLayout);
                //VisualizationImageServer<FlowObject, SequenceFlow> vis = new VisualizationImageServer<>(graphLayout, graphLayout.getSize());
                //RenderContext<FlowObject, SequenceFlow> context = vis.getRenderContext();
                processGraph.setLayout(new Point2D.Double(10.0, 10.0));

                for (FlowObject v : processGraph.getVertices()) {
                    //Point2D coordinates = graphLayout.transform(v);
                    Point2D coordinates = v.getCoordinates();
                    double width = 25.0, height = 25.0;
                    if (v.getObjectType() == FlowObject.FlowObjectType.Task) {
                        width = 80.0;
                        height = 40.0;
                    }
                    writer.println("<bpmndi:BPMNShape bpmnElement=\"" + v.getObjectID() + "\">");
                    writer.println("<dc:Bounds x=\"" + coordinates.getX() + "\" y=\"" + coordinates.getY() + "\" width=\"" + width + "\" height=\"" + height + "\"/>");
                    writer.println("<bpmndi:BPMNLabel/>");
                    writer.println("</bpmndi:BPMNShape>");
                }

                for (SequenceFlow e : processGraph.getEdges()) {
                    FlowObject startVertex = processGraph.getSource(e), endVertex = processGraph.getDest(e);
                    Double startX = startVertex.getCoordinates().getX() + startVertex.getWidth() / 2;
                    Double startY = startVertex.getCoordinates().getY() + startVertex.getHeight() / 2;
                    Double endX = endVertex.getCoordinates().getX() + endVertex.getWidth() / 2;
                    Double endY = endVertex.getCoordinates().getY() + endVertex.getHeight() / 2;
                    //Point2D startPoint = graphLayout.transform(processGraph.getSource(e));
                    //Point2D endPoint = graphLayout.transform(processGraph.getDest(e));
                    writer.println("<bpmndi:BPMNEdge bpmnElement=\"" + e.hashCode() + "\">");
                    writer.println("<di:waypoint x=\"" + startX + "\" y=\"" + startY + "\"/>");
                    writer.println("<di:waypoint x=\"" + endX + "\" y=\"" + endY + "\"/>");
                    //writer.println("<di:waypoint x=\""+startPoint.getX()+"\" y=\""+startPoint.getY()+"\"/>");
                    //writer.println("<di:waypoint x=\""+endPoint.getX()+"\" y=\""+endPoint.getY()+"\"/>");
                    writer.println("</bpmndi:BPMNEdge>");
                }

                writer.println("</bpmndi:BPMNPlane>");
                writer.println("</bpmndi:BPMNDiagram>");
            }
            writer.println("</definitions>");
            writer.close();
        }
        catch (FileNotFoundException fino){
            System.out.println("File not found.");
        }
        catch (UnsupportedEncodingException uoo) {
            System.out.println("Unsupported encoding.");
        }
    }
}
