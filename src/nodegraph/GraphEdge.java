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
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

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
    
    private static final double 
            MINIMUM_ARROWED_WIDTH = 2.0,
            MAXIMUM_ARROWED_WIDTH = GraphNode.RADIUS * 1.5,
            
            MINIMUM_TAPERED_WIDTH = GraphNode.RADIUS * 0.2,
            MAXIMUM_TAPERED_WIDTH = GraphNode.RADIUS * 2,
            
            MINIMUN_HUE = 0.333,              // = ~170
            MAXIMUM_HUE = 0.694,              // = ~216
            
            MAXIMUM_BRIGHTNESS = 0.9,
            
            HUE_MULTIPLIER = MAXIMUM_HUE - MINIMUN_HUE,
            TAPERED_WIDTH_MULTIPLIER = MAXIMUM_TAPERED_WIDTH - MINIMUM_TAPERED_WIDTH,
            ARROWED_WIDTH_MULTIPLIER = MAXIMUM_ARROWED_WIDTH - MINIMUM_ARROWED_WIDTH,
            BRIGHTNESS_MULTIPLIER = MAXIMUM_BRIGHTNESS,
            GRAIN_MULTIPLIER = 40,
            FUZZINESS_MULTIPLIER = 25,
            
            ARROW_HEAD_OPENNESS = 10,
            ARROW_HEAD_SIZE = 30;
    
    public static final double 
            DEFAULT_ARROW_WIDTH = 0.1,
            DEFAULT_TAPERED_WIDTH = 0.5;
    
    private GraphNode source;
    private GraphNode target;
    
    /**
     * Edge direction.
     * E.g. none, one way, both ways.
     */
    private byte direction;
    
    /**
     * Edge type.
     * e.g. Arrowed or tapered.
     */
    private byte edgeType;
    
    /**
     * Edge width. 
     * Use getWidth() since its actual value depends on the edgeType.
     */
    private double width;
    
    /**
     * Edge grain.
     * It's similar to a line stroke dash.
     */
    private double grain;
    
    /**
     * Represents the edge drawable elements.
     * It usually contains a single path element which represents the edge but
     * it could have others such as labels.
     */
    private final Group edgeGroup;
    
    /**
     * Represents the edge.
     * So far this element is all that it's needed to represent an edge.
     */
    private final Path path;
    
    /**
     * Represents a helper object with which we create the grain effect.
     */
    private final Group grainGroup;
    
    /**
     * Edge color.
     */
    private final ObjectProperty<Color> color = new SimpleObjectProperty();
    
    /**
     * Edge color adjustments.
     * With it we handle color effects such as brightness, hue, opacity, etc.
     */
    private final ColorAdjust colorAdjust;
    
    /**
     * Blur or fuzziness edge effect.
     */
    private final BoxBlur blur;
    
    public GraphEdge (GraphNode fromNode, GraphNode toNode, byte edgeType, byte direction) {
        this.source = fromNode;
        this.target = toNode;
        this.edgeType = edgeType;
        this.direction = direction;
        
        width = 0.0;
        grain = 0.0;
        edgeGroup = new Group();
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
        edgeGroup.setEffect(blur);
        
        edgeGroup.getChildren().add(path);
    }
    
    /**
     * Update the edge with the established features and the nodes current position.
     */
    public void update () {
        double targetOffSet = GraphNode.RADIUS;
        
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
    
    /**
     * Create an arrowed edge from sourcePos to targetPost.
     */
    private void createArrow (Point2D sourcePos, Point2D targetPos) {
        // Reference position used for creating the head of the arrow...
        Point2D refPos = new Point2D(targetPos.getX() + 10, targetPos.getY());
        double arrowWidth = getWidth() / 2;
        double angle = targetPos.angle(sourcePos, refPos) * Math.PI / 180;
        
        angle = targetPos.getY() <= sourcePos.getY() ? (Math.PI / 2) + angle : (Math.PI / 2) - angle;
        refPos = newPointInLine(targetPos, sourcePos, ARROW_HEAD_SIZE);
        
        // Stick points at the source...
        Point2D sourceA = new Point2D(
                sourcePos.getX() + arrowWidth * Math.cos(angle), 
                sourcePos.getY() + arrowWidth * Math.sin(angle));
        Point2D sourceB = new Point2D(
                sourcePos.getX() + arrowWidth * Math.cos(Math.PI + angle), 
                sourcePos.getY() + arrowWidth * Math.sin(Math.PI + angle));

        // Stick points at the beginning of the arrow head...
        Point2D targetA = new Point2D(
                refPos.getX() + arrowWidth * Math.cos(angle), 
                refPos.getY() + arrowWidth * Math.sin(angle));
        Point2D targetB = new Point2D(
                refPos.getX() + arrowWidth * Math.cos(Math.PI + angle), 
                refPos.getY() + arrowWidth * Math.sin(Math.PI + angle));

        // Arrow head side points...
        Point2D arrowA = new Point2D(
                refPos.getX() + (ARROW_HEAD_OPENNESS + arrowWidth) * Math.cos(angle), 
                refPos.getY() + (ARROW_HEAD_OPENNESS + arrowWidth) * Math.sin(angle));

        Point2D arrowB = new Point2D(
                refPos.getX() + (ARROW_HEAD_OPENNESS + arrowWidth)  * Math.cos(Math.PI + angle), 
                refPos.getY() + (ARROW_HEAD_OPENNESS + arrowWidth) * Math.sin(Math.PI + angle));
        
        // Draw arrow...
        path.getElements().addAll(
                new MoveTo(sourceA.getX(), sourceA.getY()),
                new LineTo(targetA.getX(), targetA.getY()),
                
                new LineTo(arrowA.getX(), arrowA.getY()),
                new LineTo(targetPos.getX(), targetPos.getY()),
                new LineTo(arrowB.getX(), arrowB.getY()),
                
                new LineTo(targetB.getX(), targetB.getY()),
                new LineTo(sourceB.getX(), sourceB.getY()),

                new ClosePath()
        );
        
        // Create grain effect when there is one...
        if (grain > 0.0) {
            grainGroup.getChildren().clear();
            // Spaces between each dash...
            double dashPadding = grain * 2;
            
            // Here we place white lines that cut the edge perpendicularlly.
            while (sourceA.distance(arrowA) >= grain) {
                sourceA = newPointInLine(sourceA, arrowA, dashPadding);
                sourceB = newPointInLine(sourceB, arrowB, dashPadding);

                Line l = new Line(sourceA.getX(), sourceA.getY(), sourceB.getX(), sourceB.getY());
                l.setStroke(Color.WHITE);
                l.setStrokeWidth(grain);
                grainGroup.getChildren().add(l);
            }
            
            // A replica of the head of the arrow is added so that the actual head
            // won't dissapear when using "clip"
            Path arrowHead = new Path(
                    new MoveTo(targetPos.getX(), targetPos.getY()),
                    new LineTo(arrowA.getX(), arrowA.getY()),
                    new LineTo(arrowB.getX(), arrowB.getY()),
                    new ClosePath()
            );
            arrowHead.setFill(Color.WHITE);
            arrowHead.setStroke(Color.WHITE);
            
            grainGroup.getChildren().add(arrowHead);
            path.setClip(grainGroup);
        }
    }
    
    private void createTapered (Point2D sourcePos, Point2D targetPos) {
        // Reference position used for establishing the source points of the triangle.
        Point2D refPos = new Point2D(targetPos.getX() + 10, targetPos.getY());
        // Triangle openness based on the given with.
        double openness = getWidth() / 2;
        double angle = targetPos.angle(sourcePos, refPos) * Math.PI / 180;
        angle = targetPos.getY() <= sourcePos.getY() ? (Math.PI / 2) + angle : (Math.PI / 2) - angle;

        Point2D sourceA = new Point2D(
                sourcePos.getX() + openness * Math.cos(Math.PI + angle), 
                sourcePos.getY() + openness * Math.sin(Math.PI + angle));
        Point2D sourceB = new Point2D(
                sourcePos.getX() + openness * Math.cos(angle), 
                sourcePos.getY() + openness * Math.sin(angle));
        
        path.getElements().addAll(
                new MoveTo(sourceA.getX(), sourceA.getY()),
                new LineTo(sourceB.getX(), sourceB.getY()),
                new LineTo(targetPos.getX(), targetPos.getY()),
                new ClosePath()
        );
        
        path.setStrokeWidth(1.0);
        
        if (grain > 0.0) {
            double grainPadding = grain * 2;
            grainGroup.getChildren().clear();
            
            // Here we place white lines that cut the edge perpendicularlly.
            while (sourceA.distance(targetPos) >= grain) {
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
    
    private double getWidth () {
        double w = this.width;
        
        if (w < 0.0 || w > 1)
            if (edgeType == TYPE_ARROWED)
                w = DEFAULT_ARROW_WIDTH;
            else
                w = DEFAULT_TAPERED_WIDTH;
        
        if (edgeType == TYPE_ARROWED)
            return w * ARROWED_WIDTH_MULTIPLIER + MINIMUM_ARROWED_WIDTH;
        
        return w * TAPERED_WIDTH_MULTIPLIER + MINIMUM_TAPERED_WIDTH;
    }
    
    public void setWidth (double width) {
        this.width = width;
    }
    
    public void setHue (double h) {
        colorAdjust.setHue(h * HUE_MULTIPLIER);
    }
    
    public void setOpacity (double o) {
        path.setOpacity(o);
    }
    
    public void setFuzziness (double f) {
        f *= FUZZINESS_MULTIPLIER;
        blur.setHeight(f);
        blur.setWidth(f);
    }
    
    public void setBrightness (double b) {
        // the less significance the brighter...
        colorAdjust.setBrightness(b * BRIGHTNESS_MULTIPLIER);
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
     * @return the edgeGroup
     */
    public Group getEdgeGroup() {
        return edgeGroup;
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
        this.grain = grain * GRAIN_MULTIPLIER;
    }
}
