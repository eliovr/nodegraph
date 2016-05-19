/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodegraph;

import java.util.ArrayList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 *
 * @author elio
 */
public class GraphNode {
    public static final double RADIUS = 15;
    
    private int id;
    private String name;
    private Group body;
    private Circle circle;
    private Label label;
    private ArrayList<GraphEdge> edges;
    
    // Auxiliar variables for fruchtermanReingold algorithm...
    public double dx;
    public double dy;
    
    public GraphNode (int id, String name) {
        this.id = id;
        this.name = name;
        dx = 0.0;
        dy = 0.0;
        
        edges = new ArrayList<>();
        
        circle = new Circle(RADIUS);
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.BLACK);
//        circle.setFill(Color.web("#0489B1"));
        
        label = new Label(name);
//        label.setTextFill(Color.WHITE);
        label.setTranslateX(-name.length()*4);
        label.setTranslateY(-7);
        label.setScaleX(1.2);
        label.setScaleY(1.2);
        
        body = new Group(circle, label);
    }
    
    public void setPosition (double x, double y) {
        body.setTranslateX(x);
        body.setTranslateY(y);
    }
    
    public Point2D getPosition () {
        return new Point2D(body.getTranslateX(), body.getTranslateY());
    }
    
    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getLabel() {
        return name;
    }

    /**
     * @param label the name to set
     */
    public void setLabel(String label) {
        this.name = label;
    }

    /**
     * @return the body
     */
    public Group getBody() {
        return body;
    }

    /**
     * @param body the body to set
     */
    public void setBody(Group body) {
        this.body = body;
    }

    /**
     * @return the edges
     */
    public ArrayList<GraphEdge> getEdges() {
        return edges;
    }

    /**
     * @param edges the edges to set
     */
    public void setEdges(ArrayList<GraphEdge> edges) {
        this.edges = edges;
    }
}
