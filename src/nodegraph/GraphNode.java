/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodegraph;

import java.util.ArrayList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 *
 * @author elio
 */
public class GraphNode {
    public static final double RADIUS = 30;
    private static final double X_OFFSET = 200;
    
    private int id;
    private String name;
    private Group body;
    private final Circle circle;
    private final Label label;
    private ArrayList<GraphEdge> inboundEdges;
    private ArrayList<GraphEdge> outboundEdges;
    
    // Auxiliar variables for fruchtermanReingold algorithm...
    public double dx;
    public double dy;
    
    public GraphNode (int id, String name) {
        this.id = id;
        this.name = name;
        dx = 0.0;
        dy = 0.0;
        
        inboundEdges = new ArrayList<>();
        outboundEdges = new ArrayList<>();
        
        circle = new Circle(RADIUS);
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.BLACK);
        label = new Label(name);
        label.setTranslateX(-name.length()*12);
        label.setTranslateY(-22);
        label.setFont(Font.font("Verdana", FontWeight.BOLD, 36));
        
        body = new Group(circle, label);
        
        body.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                body.setTranslateX(event.getSceneX() - X_OFFSET);
                body.setTranslateY(event.getSceneY());
                
                for (GraphEdge e : inboundEdges)
                    e.update();
                for (GraphEdge e : outboundEdges)
                    e.update();
            }
        });
    }
    
    public void setPosition (double x, double y) {
        dx = x;
        dy = y;
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
     * @return the inboundEdges
     */
    public ArrayList<GraphEdge> getInboundEdges() {
        return inboundEdges;
    }

    /**
     * @param edges the inboundEdges to set
     */
    public void setInboundEdges(ArrayList<GraphEdge> edges) {
        this.inboundEdges = edges;
    }

    /**
     * @return the outboundEdges
     */
    public ArrayList<GraphEdge> getOutboundEdges() {
        return outboundEdges;
    }

    /**
     * @param outboundEdges the outboundEdges to set
     */
    public void setOutboundEdges(ArrayList<GraphEdge> outboundEdges) {
        this.outboundEdges = outboundEdges;
    }
}
