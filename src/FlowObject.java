import edu.uci.ics.jung.graph.util.Pair;
import org.apache.commons.collections15.ListUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Flow;

public class FlowObject {
    private int objectID;
    private String objectName;
    public enum FlowObjectType {
        Task, XORsplit, XORjoin, ANDsplit, ANDjoin, StartEvent, EndEvent, DummyObject
    }
    private FlowObjectType objectType;
    private Point2D coordinates;
    private Pair<Double> dimensions;
    private String gatewayCondition;

    public static final List<FlowObjectType> splitGateways = Arrays.asList(FlowObject.FlowObjectType.XORsplit, FlowObject.FlowObjectType.ANDsplit);
    public static final List<FlowObjectType> joinGateways = Arrays.asList(FlowObject.FlowObjectType.XORjoin, FlowObject.FlowObjectType.ANDjoin);
    public static final List<FlowObjectType> gateways = ListUtils.union(splitGateways, joinGateways);
    public static final List<FlowObjectType> tasksAndEvents = Arrays.asList(FlowObject.FlowObjectType.Task, FlowObject.FlowObjectType.EndEvent, FlowObject.FlowObjectType.StartEvent);

    public FlowObject(String name, FlowObjectType type) {
        objectID = this.hashCode();
        objectName = name;
        objectType = type;
        setDefaultDimensions(type);
    }

    public FlowObject(FlowObjectType type) {
        objectID = this.hashCode();
        objectName = type.toString() + "" + this.hashCode();
        objectType = type;
        setDefaultDimensions(type);
    }

    public FlowObject(FlowObjectType type, String connectedObjectName) {
        objectID = this.hashCode();
        objectName = type.toString() + "#" + connectedObjectName;
        objectType = type;
        setDefaultDimensions(type);
    }

    public FlowObject(FlowObject fo) {
        objectID = this.hashCode();
        objectName = fo.objectName;
        objectType = fo.objectType;
        dimensions = new Pair<>(fo.dimensions);
    }

    private void setDefaultDimensions(FlowObjectType type) {
        if(type == FlowObject.FlowObjectType.Task)
            setDimensions(80.0, 40.0);
        else
            setDimensions(25.0, 25.0);
    }

    public int getObjectID() {return objectID;}
    public String getObjectName() {return objectName;}
    public FlowObjectType getObjectType() {return objectType;}
    public Pair<Double> getDimensions() {return dimensions;}
    public Double getWidth() {return dimensions.getFirst();}
    public Double getHeight() {return dimensions.getSecond();}
    public Point2D getCoordinates() {return coordinates;}
    public void setDimensions(Double width, Double height) {
        dimensions = new Pair<>(width, height);
    }
    public void setCoordinates(Double x, Double y) {
        coordinates = new Point2D.Double(x,y);
    }
    public void setCoordinates(Point2D point) {
        coordinates = point;
    }
}
