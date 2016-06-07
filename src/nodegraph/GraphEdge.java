/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodegraph;

import java.util.Calendar;
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
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

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
        path.setStrokeLineJoin(StrokeLineJoin.MITER);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        
        blur.setIterations(3);
        bodyGroup.setEffect(blur);
        
        bodyGroup.getChildren().add(path);
    }
    
    public void update () {
        double targetOffSet = GraphNode.RADIUS;
        if (edgeType == TYPE_ARROWED) targetOffSet += (width * 10);
        
        Point2D sourcePos = source.getPosition();
        Point2D targetPos = newPointInLine(target.getPosition(), sourcePos, targetOffSet);
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
        Point2D refPos = new Point2D(targetPos.getX() + 10, targetPos.getY());
        double strokeWidth = width * 15;
        // Arrow radious...
        double radius = GraphNode.RADIUS * 2 / Math.PI;
        // Arrow openness...
        double openness = Math.PI / 10;
        double angle = targetPos.angle(sourcePos, refPos) * Math.PI / 180;
        angle = targetPos.getY() <= sourcePos.getY() ? angle : -angle;
        
        // Second vertice of the arrow...
        Point2D arrowA = new Point2D(
                targetPos.getX() + radius * Math.cos(angle + openness), 
                targetPos.getY() + radius * Math.sin(angle + openness));
        // Third vertice of the arrow...
        Point2D arrowB = new Point2D(
                targetPos.getX() + radius * Math.cos(angle - openness), 
                targetPos.getY() + radius * Math.sin(angle - openness));
                
        path.getElements().addAll(
                new MoveTo(sourcePos.getX(), sourcePos.getY()),
                new LineTo(targetPos.getX(), targetPos.getY()),
                new LineTo(arrowA.getX(), arrowA.getY()),
                new LineTo(arrowB.getX(), arrowB.getY()),
                new LineTo(targetPos.getX(), targetPos.getY()));
        
        path.setStrokeWidth(strokeWidth);
        
        if (grain > 0.0) {
            grainGroup.getChildren().clear();
            double grainPadding = grain * 2;
            double grainLength = grain * 3;
            angle = targetPos.angle(sourcePos, refPos) * Math.PI / 180;
            angle = targetPos.getY() <= sourcePos.getY() ? (Math.PI / 2) + angle : (Math.PI / 2) - angle;
            
            Point2D sourceA = new Point2D(
                    sourcePos.getX() + strokeWidth * Math.cos((Math.PI*2) + angle), 
                    sourcePos.getY() + strokeWidth * Math.sin((Math.PI*2) + angle));
            Point2D sourceB = new Point2D(
                sourcePos.getX() + strokeWidth * Math.cos(Math.PI + angle), 
                sourcePos.getY() + strokeWidth * Math.sin(Math.PI + angle));
            
            while (sourceA.distance(arrowA) > grainLength) {
                sourceA = newPointInLine(sourceA, arrowA, grainPadding);
                sourceB = newPointInLine(sourceB, arrowB, grainPadding);

                Line l = new Line(sourceA.getX(), sourceA.getY(), sourceB.getX(), sourceB.getY());
                l.setStroke(Color.WHITE);
                l.setStrokeWidth(grain);
                grainGroup.getChildren().add(l);
            }
            
            Path arrowHead = new Path(
                    new MoveTo(targetPos.getX(), targetPos.getY()),
                    new LineTo(arrowA.getX(), arrowA.getY()),
                    new LineTo(arrowB.getX(), arrowB.getY()),
                    new ClosePath()
            );
            arrowHead.setFill(Color.WHITE);
            arrowHead.setStroke(Color.WHITE);
            arrowHead.setStrokeWidth(strokeWidth);
            
            grainGroup.getChildren().add(arrowHead);
            path.setClip(grainGroup);
        }
    }
    
    private void createTapered (Point2D sourcePos, Point2D targetPos) {
        Point2D refPos = new Point2D(targetPos.getX() + 10, targetPos.getY());
        double radius = GraphNode.RADIUS * (width * 3) / Math.PI;
        double angle = targetPos.angle(sourcePos, refPos) * Math.PI / 180;
        angle = targetPos.getY() <= sourcePos.getY() ? (Math.PI / 2) + angle : (Math.PI / 2) - angle;

        Point2D sourceA = new Point2D(
                sourcePos.getX() + radius * Math.cos(Math.PI + angle), 
                sourcePos.getY() + radius * Math.sin(Math.PI + angle));
        Point2D sourceB = new Point2D(
                sourcePos.getX() + radius * Math.cos((Math.PI*2) + angle), 
                sourcePos.getY() + radius * Math.sin((Math.PI*2) + angle));
        
        path.getElements().addAll(
                new MoveTo(sourceA.getX(), sourceA.getY()),
                new LineTo(sourceB.getX(), sourceB.getY()),
                new LineTo(targetPos.getX(), targetPos.getY()),
                new ClosePath()
        );
        
        path.setStrokeWidth(1.0);
        
        if (grain > 0.0) {
            double grainPadding = grain * 2;
            double grainLength = grain * 3;
            grainGroup.getChildren().clear();
            
            while (sourceA.distance(targetPos) > grainLength) {
                sourceA = newPointInLine(sourceA, targetPos, grainPadding);
                sourceB = newPointInLine(sourceB, targetPos, grainPadding);

                Line l = new Line(sourceA.getX(), sourceA.getY(), sourceB.getX(), sourceB.getY());
                l.setStroke(Color.WHITE);
                l.setStrokeWidth(grain);
                grainGroup.getChildren().add(l);
            }
            
            path.setClip(grainGroup);
        }
    }
    
    /** Returns a new position based on the line drawn between p1 and p2.
     * @param p1 is the moving point e.i. the one moving towards or away p2.
     * @param p2 is the reference point e.i. the one from which p1 will move towards or away from.
     * @param stepSize distance to move towards or away.
     * @param getAway true if p1 is to move away from p2, false otherwise.
     */
    private Point2D newPointInLine(Point2D p1, Point2D p2, double stepSize){
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        
        double mag = Math.sqrt( Math.pow(dx, 2) + Math.pow(dy, 2));
        
        double x = p1.getX() + dx * stepSize / mag;
        double y = p1.getY() + dy * stepSize / mag;
        
        return new Point2D(x, y);
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
        // the less significance the fuzzier...
        f = 1 - f;  
        f *= 10;
        blur.setHeight(f);
        blur.setWidth(f);
    }
    
    public void setBrightness (double b) {
        // the less significance the brighter...
        colorAdjust.setBrightness(1.0 - b);
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
        this.grain = (1 - grain) * 40;
    }
}
