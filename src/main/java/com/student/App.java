package com.student;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import com.student.database.DatabaseConnection;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize the database and tables
        DatabaseConnection.initializeDatabase();
        
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
        Parent root = loader.load();
        
        // Create a Scene
        Scene scene = new Scene(root, 1080, 720);
        
        // Set up the Stage (Window)
        stage.setTitle("Student Management System - Phase 2");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
