/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nodegraph;

import java.util.Locale;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author elio
 */
public class NodeGraph extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle("nodegraph.locale.Bundle", new Locale("en", "US"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"), bundle);
        Parent root = (Parent)loader.load();
        
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
