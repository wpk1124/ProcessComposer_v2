import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import javafx.util.Pair;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class BPMNWriter {
    public static void writeBPMNXML(ProcessGraph processGraph, String fileName, boolean withBPMNDI) {
        try {
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" targetNamespace=\"http://www.omg.org/bpmn20\" exporter=\"PW\" xsi:schemaLocation=\"http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd\">\n");
            writer.println("<process id=\"proc_"+processGraph.hashCode()+"\">");
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
