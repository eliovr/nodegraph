/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodegraph;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
    
    private Group body;
    private Path path;
    private Line dashLine;
    private DoubleProperty width = new SimpleDoubleProperty();
    private DoubleProperty opacity = new SimpleDoubleProperty();
    private DoubleProperty dash = new SimpleDoubleProperty();
    private ObjectProperty<Color> color = new SimpleObjectProperty();
    private BoxBlur blur;
    private ColorAdjust colorAdjust;
    
    
    public GraphEdge (GraphNode fromNode, GraphNode toNode, byte edgeType, byte direction) {
        this.source = fromNode;
        this.target = toNode;
        this.edgeType = edgeType;
        this.direction = direction;
        
        body = new Group();
        blur = new BoxBlur();
        colorAdjust = new ColorAdjust();
        
        path = new Path();
        path.strokeWidthProperty().bind(width);
        path.strokeProperty().bind(color);
        path.fillProperty().bind(color);
        path.setEffect(colorAdjust);
        
        dashLine = new Line();
        dashLine.setStrokeWidth(GraphNode.RADIUS);
        dashLine.setStroke(Color.BLUE);
        
        body.getChildren().addAll(path, dashLine);
        
        blur.setIterations(3);
        body.setEffect(blur);
        body.opacityProperty().bind(opacity);
        
        dash.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (dashLine != null) {
                double value = newValue.doubleValue();
                if (value <= 0.0) {
                    dashLine.setVisible(false);
                } else {
                    dashLine.getStrokeDashArray().set(0, value);
                    dashLine.setVisible(true);
                }
            }
        });
    }
    
    public void update () {
        Point2D sourcePos = source.getPosition();
        Point2D targetPos = target.getPosition();
        path.getElements().clear();
        
        if (direction == DIRECTION_NONE) {
            path.strokeWidthProperty().bind(width);
            path.getElements().add(new MoveTo(sourcePos.getX(), sourcePos.getY()));
            path.getElements().add(new LineTo(targetPos.getX(), targetPos.getY()));
        } else {
            switch (edgeType) {
                case TYPE_ARROWED:
                    path.strokeWidthProperty().bind(width);
                    createArrow(sourcePos, targetPos);
                    break;
                case TYPE_TAPERED:
                    path.strokeWidthProperty().unbind();
                    createTapered(sourcePos, targetPos);
            }
        }
        
        dashLine.setStartX(sourcePos.getX());
        dashLine.setStartY(sourcePos.getY());
        dashLine.setEndX(targetPos.getX());
        dashLine.setEndY(targetPos.getY());
        dashLine.setVisible(dash.get() > 0);
    }
    
    private void createArrow (Point2D sourcePos, Point2D targetPos) {
        Point2D other = new Point2D(targetPos.getX() + 10, targetPos.getY());

        double radius = GraphNode.RADIUS * 8 / Math.PI;
        double openness = Math.PI / 15;
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
    }
    
    private void createTapered (Point2D sourcePos, Point2D targetPos) {
        Point2D other = new Point2D(targetPos.getX() + 10, targetPos.getY());

        double radius = GraphNode.RADIUS * 1.8 / Math.PI;
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
    }
    
    public ColorAdjust getColorAdjust () {
        return colorAdjust;
    }
    
    public BoxBlur getBlur () {
        return blur;
    }
    
    public ObjectProperty getColorProperty () {
        return color;
    }
    
    public DoubleProperty getWidthProperty () {
        return width;
    }
    
    public DoubleProperty getOpacityProperty () {
        return opacity;
    }
    
    public DoubleProperty getDashProperty () {
        return dash;
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
        return body;
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
}
