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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

/**
 *
 * @author elio
 */
public class MainController implements Initializable {
    
    private static final float SPEED_DIVISOR = 800;
    private static final float AREA_MULTIPLICATOR = 10000;
    
    @FXML
    private AnchorPane canvas;

    @FXML
    private Slider edgeWidth;
    
    @FXML
    private Slider edgeHue;

    @FXML
    private Slider edgeFuzziness;
    
    @FXML
    private ChoiceBox<String> edgeTypes;
    
    @FXML
    private Slider edgeBrightness;

    @FXML
    private Slider edgeOpacity;
    
    @FXML
    private Slider edgeDash;

    @FXML
    private Label labelOpacity;

    @FXML
    private Label labelWidth;

    @FXML
    private Label labelFuzziness;

    @FXML
    private Label labelBrightness;

    @FXML
    private Label labelHue;
    
    @FXML
    private Label labelDash;

    @FXML
    private TextArea textAreaNodes;
    
    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;
    
    @FXML
    private ColorPicker edgeColor;
    
    private Group rootGroup;
    
    ArrayList<GraphNode> nodes;
    
    ArrayList<GraphEdge> edges;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        resources = rb;
        
        edgeTypes.setItems(FXCollections.observableArrayList(GraphEdge.TYPES));
        edgeTypes.getSelectionModel().selectFirst();
        
        edgeColor.setValue(Color.BLACK);
        
        labelWidth.textProperty().bind(edgeWidth.valueProperty().asString());
        labelBrightness.textProperty().bind(edgeBrightness.valueProperty().asString());
        labelFuzziness.textProperty().bind(edgeFuzziness.valueProperty().asString());
        labelHue.textProperty().bind(edgeHue.valueProperty().asString());
        labelOpacity.textProperty().bind(edgeOpacity.valueProperty().asString());
        labelDash.textProperty().bind(edgeDash.valueProperty().asString());
        rootGroup = new Group();
        canvas.getChildren().add(rootGroup);
        
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
        
        if (!textAreaNodes.getText().trim().isEmpty()) {
            String[] nodePairs = textAreaNodes.getText().trim().split(",");
            
            for (String nodePair : nodePairs) {
                String [] pairAndAttributes = nodePair.trim().split(" ");
                String pair = pairAndAttributes[0];
//                String [] pairAndAttributes = nodePair.split("\\|");
//                String pair = pairAndAttributes[0].replace(" ", "");
                attributes = new HashMap();
                
                // Retrieve attributes if any...
                if (pairAndAttributes.length > 1) {
                    for (int i = 1; i < pairAndAttributes.length; i++) {
                        String attr = pairAndAttributes[i];
                        String[] nameValue = attr.split("=");
                        attributes.put(nameValue[0].trim(), nameValue[1].trim());
                    }
                    
//                    String attrs = pairAndAttributes[1];
//                    for (String attr : attrs.trim().split(" ")) {
//                        String[] nameValue = attr.split("=");
//                        attributes.put(nameValue[0].trim(), nameValue[1].trim());
//                    }
                }
                
                // Default attributes...
                String label = attributes.getOrDefault("l", "");
                double width = Double.parseDouble(attributes.getOrDefault("w", "1.0"));
                double hue = Double.parseDouble(attributes.getOrDefault("h", "0.0"));
                double opacity = Double.parseDouble(attributes.getOrDefault("o", "1.0"));
                double fuzziness = Double.parseDouble(attributes.getOrDefault("f", "0.0"));
                double brightness = Double.parseDouble(attributes.getOrDefault("b", "0.0"));
                
                String[] nodesArr = null;
                byte direction = 0;
                
                if (pair.contains("-")) {
                    nodesArr = pair.split("-");
                    direction = GraphEdge.DIRECTION_NONE;
                } else if (pair.contains(">")) {
                    nodesArr = pair.split(">");
                    direction = GraphEdge.DIRECTION_ONEWAY;
                } else  if (pair.contains("<>")) {
                    nodesArr = pair.split("<>");
                    direction = GraphEdge.DIRECTION_BOTHWAYS;
                }
                
                if (nodesArr.length == 1) {
                    createOrGetNode(nodesArr[0]);
                } else if (nodesArr.length == 2) {
                    GraphNode fromNode = createOrGetNode(nodesArr[0]);
                    GraphNode toNode = createOrGetNode(nodesArr[1]);
                    byte edgeType = (byte)edgeTypes.getSelectionModel().getSelectedIndex();
                    
                    GraphEdge edge = new GraphEdge(fromNode, toNode, edgeType, direction);
                    edge.setLabel(label);
                    edge.setWidth(width);
                    edge.setHue(hue);
                    edge.setOpacity(opacity);
                    edge.setFuzziness(fuzziness);
                    edge.setBrightness(brightness);
                    edge.getColorProperty().bind(edgeColor.valueProperty());
                    
                    edges.add(edge);
                    rootGroup.getChildren().add(0, edge.getBody());
                    rootGroup.getChildren().add(edge.getLabelNode());
                }
            }
        }
        
//        new Thread(new FruchtermanReingold()).start();
        double area = canvas.getWidth() * canvas.getHeight() * 2;
        double k = Math.sqrt(area / nodes.size());
        double temperature = canvas.getWidth() / 10;
        double speed = 1;
        
        for (int i = 0; i < 100; i++) {
            fruchtermanReingold(area, k, temperature, speed);
            temperature *= (1.0 - i / (double) 100);
        }
        
        for (GraphEdge edge : edges)
            edge.update();
    }
    
    /**
     * Animated version of the algorithm.
     */
    private class FruchtermanReingold extends Task<Object> {

        @Override
        protected Object call() throws Exception {
            double area = canvas.getWidth() * canvas.getHeight();
            double k = Math.sqrt(area / nodes.size());
            double temperature = canvas.getWidth() / 10;
            double speed = 1;
            
            for (int i = 0; i < 100; i++) {
                fruchtermanReingold(area, k, temperature, speed);
                // Cooling...
                temperature *= (1.0 - i / (double) 100);
                // Update edges...
                for (GraphEdge edge : edges)
                    edge.update();
                
                Thread.sleep(100);
            }
            
            return null;
        }
        
    }
    
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
                sNode.dx -= xdiff / dist * force;
                sNode.dy -= ydiff / dist * force;
                tNode.dx += xdiff / dist * force;
                tNode.dy += ydiff / dist * force;
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
    
    private GraphNode createOrGetNode (String nodeLabel) {
        GraphNode node = null;
        
        for (GraphNode n : nodes)
            if (n.getLabel().equalsIgnoreCase(nodeLabel))
                node = n;
        
        if (node == null) {
            node = new GraphNode(nodes.size(), nodeLabel);
            nodes.add(node);
            rootGroup.getChildren().add(node.getBody());
            
            node.getBody().setTranslateX(randomBetween(400, 410));
            node.getBody().setTranslateY(randomBetween(400, 410));
        }
            
        return node;
    }
    
    public int nodeIndex (String nodeLabel) {
        for (GraphNode n : nodes) {
            if (n.getLabel().equalsIgnoreCase(nodeLabel))
                return n.getId();
        }
        
        return -1;
    }
    
    public static double randomBetween(int lowest, int highest){
        return new Random().nextInt(highest-lowest) + lowest;
    }
}
