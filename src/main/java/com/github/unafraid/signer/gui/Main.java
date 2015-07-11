package com.github.unafraid.signer.gui;/**
 * Created by UnAfraid on 11.7.2015 ã..
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        final Parent root = FXMLLoader.load(getClass().getResource("/views/Main.fxml"));
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Digital Signer");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
