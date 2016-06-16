/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodegraph;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

/**
 *
 * @author elio
 */
public class MainController implements Initializable {
    
    private static final float SPEED_DIVISOR = 1200;
    
    @FXML
    private AnchorPane canvas;

    @FXML
    private ChoiceBox<String> edgeTypes;
    
    @FXML
    private ColorPicker edgeColor;
    
    @FXML
    private TextArea textAreaNodes;
    
    @FXML
    private Button buttonPlace;
    
    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;
    
    // parent group where all nodes, edges and labeles should be.
    private Group rootGroup;
    
    ArrayList<GraphNode> nodes;
    
    ArrayList<GraphEdge> edges;
    
    private FruchtermanReingold frAlgorithm;
    private Thread thread;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        resources = rb;
        edgeTypes.setItems(FXCollections.observableArrayList(GraphEdge.TYPES));
        edgeTypes.getSelectionModel().selectFirst();
        edgeColor.setValue(Color.BLACK);
        rootGroup = new Group();
        canvas.getChildren().add(rootGroup);
        frAlgorithm = null;
        
        // Update edges when changing edge type...
        edgeTypes.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (edges != null) {
                    for (GraphEdge edge : edges) {
                        edge.setEdgeType(newValue.byteValue());
                        edge.update();
                    }
                }
            }
        });
    }
    
    @FXML
    void handlePlaceAction(ActionEvent event) {
        rootGroup.getChildren().clear();
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        HashMap<String, String> attributes;
        
        // Parse user input...
        if (!textAreaNodes.getText().trim().isEmpty()) {
            // Get every line in an array...
            String[] lines = textAreaNodes.getText().trim().split("\n");
            
            for (String line : lines) {
                String [] pairAndAttributes = line.trim().split(" ");
                String nodePair = pairAndAttributes[0];
                attributes = new HashMap();
                
                // Retrieve attributes if any...
                if (pairAndAttributes.length > 1) {
                    for (int i = 1; i < pairAndAttributes.length; i++) {
                        String attr = pairAndAttributes[i];
                        String[] nameValue = attr.split("=");
                        attributes.put(nameValue[0].trim(), nameValue[1].trim());
                    }
                }
                
                // Default values are given here. All values should be between 0 and 1.
                // Default width is an exception because it's different for tapered and arrow edges.
                double width = Double.parseDouble(attributes.getOrDefault("w", "-1"));
                double hue = Double.parseDouble(attributes.getOrDefault("h", "0.0"));
                double opacity = Double.parseDouble(attributes.getOrDefault("o", "1.0"));
                double fuzziness = Double.parseDouble(attributes.getOrDefault("f", "1.0"));
                double brightness = Double.parseDouble(attributes.getOrDefault("b", "1.0"));
                double grain = Double.parseDouble(attributes.getOrDefault("g", "1.0"));
                
                String[] nodesArr;
                byte direction;
                
                if (nodePair.contains("-")) {
                    nodesArr = nodePair.split("-");
                    direction = GraphEdge.DIRECTION_NONE;
                } 
                else if (nodePair.contains(">")) {
                    nodesArr = nodePair.split(">");
                    direction = GraphEdge.DIRECTION_ONEWAY;
                } 
                else  if (nodePair.contains("<>")) {
                    nodesArr = nodePair.split("<>");
                    direction = GraphEdge.DIRECTION_BOTHWAYS;
                } 
                else { // is a single node...
                    getOrCreate(nodePair);
                    continue;
                }
                
                GraphNode fromNode = getOrCreate(nodesArr[0]);
                GraphNode toNode = getOrCreate(nodesArr[1]);
                byte edgeType = (byte)edgeTypes.getSelectionModel().getSelectedIndex();

                GraphEdge edge = new GraphEdge(fromNode, toNode, edgeType, direction);
                fromNode.getOutboundEdges().add(edge);
                toNode.getInboundEdges().add(edge);
                
                edge.setWidth(width);
                edge.setHue(hue);
                edge.setOpacity(opacity);
                edge.setFuzziness(1 - fuzziness);
                edge.setBrightness(1 - brightness);
                edge.setGrain(1 - grain);
                edge.getColorProperty().bind(edgeColor.valueProperty());

                edges.add(edge);
                rootGroup.getChildren().add(0, edge.getEdgeGroup());
            }
        }
        
        double minX = canvas.getWidth() / 2;
        double minY = canvas.getHeight() / 2;
        
        
        double radius = (nodes.size() * GraphNode.RADIUS * 1.5) / Math.PI;
        double x, y;
        double angle = 2 * Math.PI / nodes.size();
        int j = 1;
        // Place nodes initially according to the amount of incoming edges...
        for (GraphNode node : nodes) {
            x = minX + radius * Math.cos(j * angle);
            y = minY + radius * Math.sin(j * angle);
            node.getBody().setTranslateX(x);
            node.getBody().setTranslateY(y);
            j++;
            
//            node.getBody().setTranslateX(minX + randomBetween(-50, 50));
//            node.getBody().setTranslateY(minY + (node.getInboundEdges().size() * 25) + Math.random());
        }
        
        // Initialize variables for the Fruchterman Reingold algorithm...
        double area = canvas.getWidth() * canvas.getHeight();
        double k = Math.sqrt(area / nodes.size());
        double temperature = canvas.getWidth() / 10;
        double speed = 1;
        
        buttonPlace.setDisable(true);
        frAlgorithm = new FruchtermanReingold(area, k, temperature, speed);
        frAlgorithm.setOnSucceeded((WorkerStateEvent e) -> {
            buttonPlace.setDisable(false);
        });

        thread = new Thread(frAlgorithm);
        thread.start();
        
        // Run Fruchterman Reingold algorithm...
//        for (int i = 0; i < 100; i++) {
//            fruchtermanReingold(area, k, temperature, speed);
//            // Cooling...
//            temperature *= (1.0 - i / 10000);
//        }

        // Update edges...
//        for (GraphEdge edge : edges)
//            edge.update();
    }
    
    /**
     * Animated version of the algorithm.
     */
    private class FruchtermanReingold extends Task<Object> {

        double area;
        double k;
        double temperature;
        double speed;
        
        public FruchtermanReingold (double area, double k, double temperature, double speed) {
            this.area = area;
            this.k = k;
            this.temperature = temperature;
            this.speed = speed;
        }
        
        @Override
        protected Object call() throws Exception {
            for (int i = 0; i < 100; i++) {
                try {
                    // run algorithm...
                    fruchtermanReingold(area, k, temperature, speed);
                    // Cooling...
                    temperature *= (1.0 - i / 10000);
                    // Update edges...
                    for (GraphEdge edge : edges)
                        edge.update();

                    Thread.sleep(30);
                } catch (Exception e) {
                    
                }
            }
            
            return null;
        }
    }
    
    /**
     * See: Graph Drawing by Force directed Placement (Fruchterman et al, 1991)
     */
    void fruchtermanReingold (double area, double k, double temp, double speed) {
        double force;
        
        // Calculate repulsive forces...
        for (GraphNode n1 : nodes) {
            for (GraphNode n2 : nodes) {
                if (n1.getId() != n2.getId()) {
                    Point2D posn1 = n1.getPosition();
                    Point2D posn2 = n2.getPosition();

                    double xdiff = posn1.getX() - posn2.getX();
                    double ydiff = posn1.getY() - posn2.getY();

                    double dist = Math.sqrt(xdiff * xdiff + ydiff * ydiff);

                    if (dist > 0) {
                        force = k * k / dist;
                        n1.dx += xdiff / dist * force;
                        n1.dy += ydiff / dist * force;
                    }
                }
            }
        }

        // Calculate attractive forces...
        for (GraphEdge edge : edges) {
            GraphNode sNode = edge.getSource();
            GraphNode tNode = edge.getTarget();
            Point2D sPos = sNode.getPosition();
            Point2D tPos = tNode.getPosition();

            double xdiff = sPos.getX() - tPos.getX();
            double ydiff = sPos.getY() - tPos.getY();
            double dist = Math.sqrt(xdiff * xdiff + ydiff * ydiff);

            force = dist * dist / k;

            if (dist > 0) {
                sNode.dx -= (xdiff / dist) * force;
                sNode.dy -= (ydiff / dist) * force;
                tNode.dx += (xdiff / dist) * force;
                tNode.dy += (ydiff / dist) * force;
            }
        }
        
        for (GraphNode n : nodes) {
            n.dx *= speed / SPEED_DIVISOR;
            n.dy *= speed / SPEED_DIVISOR;
        }

        for (GraphNode n : nodes) {
            Point2D pos = n.getPosition();

            double deltaLength = Math.sqrt(n.dx * n.dx + n.dy * n.dy);

            if (deltaLength > 0) {
                double min = Math.min(deltaLength, temp);
                double x = pos.getX() + (n.dx / deltaLength) * min;
                double y = pos.getY() + (n.dy / deltaLength) * min;

                n.setPosition(x, y);
            }
        }
    }
    
    
    /**
     * Get or create node with the given label.
     * Created nodes are added to the rootGroup for displaying.
     * 
     * @param nodeLabel Node label id which is unique for every node.
     * @return The existing or newly created node.
     */
    private GraphNode getOrCreate (String nodeLabel) {
        GraphNode node = null;
        
        for (GraphNode n : nodes)
            if (n.getLabel().equalsIgnoreCase(nodeLabel))
                node = n;
        
        if (node == null) {
            node = new GraphNode(nodes.size(), nodeLabel);
            nodes.add(node);
            rootGroup.getChildren().add(node.getBody());
        }
            
        return node;
    }
    
    /**
     * Helper method for generating a random number between the given parameters.
     * @param lowest lower boundary.
     * @param highest upper boundary.
     */
    public static double randomBetween(int lowest, int highest){
        return new Random().nextInt(highest-lowest) + lowest;
    }
}
