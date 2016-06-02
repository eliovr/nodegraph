/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodegraph;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 *
 * @author elio
 */
public class GraphEdge {
    public static final String[] TYPES = {"Tapered", "Arrowed"};
    public static final byte 
            TYPE_TAPERED = 0,
            TYPE_ARROWED = 1;
    
    public static final byte 
            DIRECTION_NONE = 0,
            DIRECTION_ONEWAY = 1,
            DIRECTION_BOTHWAYS = 2;
    
    private GraphNode source;
    private GraphNode target;
    private byte direction;
    private byte edgeType;
    private double width;
    private double grain;
    
    private final Group bodyGroup;
    private final Path path;
    private final Group grainGroup;
    
    private final ObjectProperty<Color> color = new SimpleObjectProperty();
    private final ColorAdjust colorAdjust;
    private final BoxBlur blur;
    
    public GraphEdge (GraphNode fromNode, GraphNode toNode, byte edgeType, byte direction) {
        this.source = fromNode;
        this.target = toNode;
        this.edgeType = edgeType;
        this.direction = direction;
        
        width = 1.0;
        grain = 0.0;
        bodyGroup = new Group();
        path = new Path();
        grainGroup = new Group();
        colorAdjust = new ColorAdjust();
        blur = new BoxBlur();
        
        path.strokeProperty().bind(color);
        path.fillProperty().bind(color);
        path.setEffect(colorAdjust);
        
        blur.setIterations(3);
        bodyGroup.setEffect(blur);
        
        bodyGroup.getChildren().add(path);
    }
    
    public void update () {
        Point2D sourcePos = source.getPosition();
        Point2D targetPos = target.getPosition();
        path.getElements().clear();
        
        if (direction == DIRECTION_NONE) {
            path.getElements().add(new MoveTo(sourcePos.getX(), sourcePos.getY()));
            path.getElements().add(new LineTo(targetPos.getX(), targetPos.getY()));
        } else {
            switch (edgeType) {
                case TYPE_ARROWED:
                    createArrow(sourcePos, targetPos);
                    break;
                case TYPE_TAPERED:
                    createTapered(sourcePos, targetPos);
            }
        }
    }
    
    private void createArrow (Point2D sourcePos, Point2D targetPos) {
        Point2D other = new Point2D(targetPos.getX() + 10, targetPos.getY());

        double radius = GraphNode.RADIUS * 6 / Math.PI;
        double openness = Math.PI / 20;
        double angle = targetPos.angle(sourcePos, other) * Math.PI / 180;
        angle = targetPos.getY() <= sourcePos.getY() ? angle : -angle;
        other = new Point2D(
                targetPos.getX() + GraphNode.RADIUS * Math.cos(angle), 
                targetPos.getY() + GraphNode.RADIUS * Math.sin(angle));
        angle = openness + angle;

        double x = targetPos.getX() + radius * Math.cos(angle);
        double y = targetPos.getY() + radius * Math.sin(angle);

        path.getElements().add(new MoveTo(sourcePos.getX(), sourcePos.getY()));
        path.getElements().add(new LineTo(other.getX(), other.getY()));
        path.getElements().add(new LineTo(x, y));
        x = targetPos.getX() + radius * Math.cos((Math.PI*2) + angle - (openness*2));
        y = targetPos.getY() + radius * Math.sin((Math.PI*2) + angle - (openness*2));
        path.getElements().add(new LineTo(x, y));
        
        path.getElements().add(new LineTo(other.getX(), other.getY()));
        path.setStrokeWidth(width * 15);
        
        if (grain > 0) {
            grainGroup.getChildren().clear();
            createGrain(sourcePos, other);
        }
    }
    
    private void createTapered (Point2D sourcePos, Point2D targetPos) {
        Point2D other = new Point2D(targetPos.getX() + 10, targetPos.getY());

        double radius = GraphNode.RADIUS * (width * 3) / Math.PI;
        double angle = targetPos.angle(sourcePos, other) * Math.PI / 180;
        angle = targetPos.getY() <= sourcePos.getY() ? (Math.PI / 2) + angle : (Math.PI / 2) - angle;

        double x = sourcePos.getX() + radius * Math.cos((Math.PI) + angle);
        double y = sourcePos.getY() + radius * Math.sin((Math.PI) + angle);

        path.getElements().add(new MoveTo(x, y));
        x = sourcePos.getX() + radius * Math.cos((Math.PI*2) + angle);
        y = sourcePos.getY() + radius * Math.sin((Math.PI*2) + angle);
        path.getElements().add(new LineTo(x, y));
        path.getElements().add(new LineTo(targetPos.getX(), targetPos.getY()));
        path.getElements().add(new ClosePath());
        path.setStrokeWidth(1.0);
        
        if (grain > 0) {
            grainGroup.getChildren().clear();
            createGrain(sourcePos, targetPos);
        }
    }
    
    private void createGrain (Point2D source, Point2D target) {
        double radius = GraphNode.RADIUS * width / Math.PI;
        if (edgeType == TYPE_TAPERED) radius *= 3;
        double angle = target.angle(source, new Point2D(target.getX()+10, target.getY()));
        angle *= Math.PI / 180;
        angle = target.getY() <= source.getY() ? (Math.PI / 2) + angle : (Math.PI / 2) - angle;

        double x = source.getX() + radius * Math.cos((Math.PI) + angle);
        double y = source.getY() + radius * Math.sin((Math.PI) + angle);
        Point2D sourceA = new Point2D(x, y);
        
        x = source.getX() + radius * Math.cos((Math.PI*2) + angle);
        y = source.getY() + radius * Math.sin((Math.PI*2) + angle);
        Point2D sourceB = new Point2D(x, y);
        
        while (sourceA.distance(target) > grain*3) {
            Line l = new Line(sourceA.getX(), sourceA.getY(), sourceB.getX(), sourceB.getY());
            l.setStroke(Color.WHITE);
            l.setStrokeWidth(grain);
            grainGroup.getChildren().add(l);
            
            sourceA = newPointInLine(sourceA, target, grain*2, false);
            sourceB = newPointInLine(sourceB, target, grain*2, false);
        }
    }
    
    /** Returns a new position based on the line drawn between p1 and p2.
     * @param p1 is the moving point e.i. the one moving towards or away p2.
     * @param p2 is the reference point e.i. the one from which p1 will move towards or away from.
     * @param stepSize distance to move towards or away.
     * @param getAway true if p1 is to move away from p2, false otherwise.
     */
    private Point2D newPointInLine(Point2D p1, Point2D p2, double stepSize, boolean getAway){
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        
        double mag = Math.sqrt( Math.pow(dx, 2) + Math.pow(dy, 2));
        
        double x3 = getAway? p1.getX() - dx * stepSize / mag : p1.getX() + dx * stepSize / mag;
        double y3 = getAway? p1.getY() - dy * stepSize / mag : p1.getY() + dy * stepSize / mag;
        
        return new Point2D(x3, y3);
    }
    
    
    public void setWidth (double w) {
        width = w <= 0 ? 0.1 : w;
    }
    
    public void setHue (double h) {
        colorAdjust.setHue(h);
    }
    
    public void setOpacity (double o) {
        path.setOpacity(o);
    }
    
    public void setFuzziness (double f) {
        f *= 20;
        blur.setHeight(f);
        blur.setWidth(f);
    }
    
    public void setBrightness (double b) {
        colorAdjust.setBrightness(b);
    }
    
    public ObjectProperty getColorProperty () {
        return color;
    }
    
    /**
     * @return the source
     */
    public GraphNode getSource() {
        return source;
    }

    /**
     * @param fromNode the source to set
     */
    public void setSource(GraphNode fromNode) {
        this.source = fromNode;
    }

    /**
     * @return the destination
     */
    public GraphNode getTarget() {
        return target;
    }

    /**
     * @param toNode the destination to set
     */
    public void setTarget(GraphNode toNode) {
        this.target = toNode;
    }

    /**
     * @return the path
     */
    public Group getBody() {
        return bodyGroup;
    }
    
    public Group getGrainGroup() {
        return grainGroup;
    }

    /**
     * @return the edgeType
     */
    public byte getEdgeType() {
        return edgeType;
    }

    /**
     * @param edgeType the edgeType to set
     */
    public void setEdgeType(byte edgeType) {
        this.edgeType = edgeType;
    }

    /**
     * @return the direction
     */
    public byte getDirection() {
        return direction;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(byte direction) {
        this.direction = direction;
    }

    /**
     * @return the grain
     */
    public double getGrain() {
        return grain;
    }

    /**
     * @param grain the grain to set
     */
    public void setGrain(double grain) {
        this.grain = grain * 40;
    }
}
