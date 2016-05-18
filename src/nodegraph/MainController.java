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
    
    private double area;
    private double gravity;
    private double speed;
    
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
        
        speed = 1;
        area = 10000;
        gravity = 10;
    }
    
    @FXML
    void handlePlaceAction(ActionEvent event) {
        rootGroup.getChildren().clear();
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        
        if (!textAreaNodes.getText().trim().isEmpty()) {
            String[] nodePairs = textAreaNodes.getText().replace(" ", "").split(",");
            
            for (String pair : nodePairs) {
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
                    edge.getWidthProperty().bind(edgeWidth.valueProperty());
                    edge.getColorProperty().bind(edgeColor.valueProperty());
                    edge.getOpacityProperty().bind(edgeOpacity.valueProperty());
                    edge.getBlur().heightProperty().bind(edgeFuzziness.valueProperty());
                    edge.getBlur().widthProperty().bind(edgeFuzziness.valueProperty());
                    edge.getColorAdjust().hueProperty().bind(edgeHue.valueProperty());
                    edge.getColorAdjust().brightnessProperty().bind(edgeBrightness.valueProperty());
                    edge.getDashProperty().bind(edgeDash.valueProperty());
                    
                    edges.add(edge);
                    rootGroup.getChildren().add(0, edge.getBody());
                }
            }
        }
        
        fruchtermanReingold();
        for (GraphEdge edge : edges)
            edge.update();
    }
    
    void fruchtermanReingold () {
        double maxDisplace = (Math.sqrt(AREA_MULTIPLICATOR * area) / 10);
        double k = Math.sqrt((AREA_MULTIPLICATOR * area) / (1 + nodes.size()));
        
        for (int i = 0; i < 200; i++) {
            for (GraphNode n1 : nodes) {
                for (GraphNode n2 : nodes) {
                    if (n1.getId() != n2.getId()) {
                        Point2D posn1 = n1.getPosition();
                        Point2D posn2 = n2.getPosition();
                        
                        double xDist = posn1.getX() - posn2.getX();
                        double yDist = posn1.getY() - posn2.getY();
                        double dist = Math.sqrt(xDist * xDist + yDist * yDist);
                        
                        if (dist > 0) {
                            double repulsiveF = k * k / dist;
                            ForceLayoutData layoutData = n1.getLayoutData();
                            layoutData.dx += xDist / dist * repulsiveF;
                            layoutData.dy += yDist / dist * repulsiveF;
                        }
                    }
                }
            }
            
            for (GraphEdge edge : edges) {
                GraphNode nf = edge.getSource();
                GraphNode nt = edge.getTarget();
                Point2D posnf = nf.getPosition();
                Point2D posnt = nt.getPosition();

                double xDist = posnf.getX() - posnt.getX();
                double yDist = posnf.getY() - posnt.getY();
                double dist = Math.sqrt(xDist * xDist + yDist * yDist);

                double attractiveF = dist * dist / k;
                
                if (dist > 0) {
                    ForceLayoutData sourceLayoutData = nf.getLayoutData();
                    ForceLayoutData targetLayoutData = nt.getLayoutData();
                    sourceLayoutData.dx -= xDist / dist * attractiveF;
                    sourceLayoutData.dy -= yDist / dist * attractiveF;
                    targetLayoutData.dx += xDist / dist * attractiveF;
                    targetLayoutData.dy += yDist / dist * attractiveF;
                }
            }
            
            for (GraphNode n : nodes) {
                ForceLayoutData layoutData = n.getLayoutData();
                Point2D pos = n.getPosition();
                
                double d = Math.sqrt(pos.getX() * pos.getX() + pos.getY() * pos.getY());
                double gf = 0.01 * k * gravity * d;
                layoutData.dx -= gf * pos.getX() / d;
                layoutData.dy -= gf * pos.getY() / d;
            }
            
            for (GraphNode n : nodes) {
                ForceLayoutData layoutData = n.getLayoutData();
                layoutData.dx *= speed / SPEED_DIVISOR;
                layoutData.dy *= speed / SPEED_DIVISOR;
            }
            
            for (GraphNode n : nodes) {
                Point2D pos = n.getPosition();
                ForceLayoutData layoutData = n.getLayoutData();
                double xDist = layoutData.dx;
                double yDist = layoutData.dy;
                float dist = (float) Math.sqrt(layoutData.dx * layoutData.dx + layoutData.dy * layoutData.dy);
//                if (dist > 0 && !n.isFixed()) {
                if (dist > 0) {
                    double limitedDist = Math.min(maxDisplace * (speed / SPEED_DIVISOR), dist);
                    double x = pos.getX() + xDist / dist * limitedDist;
                    double y = pos.getY() + yDist / dist * limitedDist;
                    n.setPosition(x, y);
                }
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
