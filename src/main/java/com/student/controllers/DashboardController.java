package com.student.controllers;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.application.Platform;


import java.io.IOException;




import com.student.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
public class DashboardController {

    // ── Metric labels ─────────────────────────────────────────────────────────
    @FXML private Label totalStudentsLabel;
    @FXML private Label recentStudentsLabel;
    @FXML private Label activeCoursesLabel;
    @FXML private HBox  loadingBox;
    @FXML private HBox  metricsBox;
    @FXML private ListView<String> activityList;
    @FXML private Button logoutButton;

    // ── Charts ────────────────────────────────────────────────────────────────
    @FXML private PieChart                    courseChart;
    @FXML private BarChart<String, Number>    statusChart;

    // ── Nav ───────────────────────────────────────────────────────────────────
    // homeScrollPane wraps homeView — we toggle the ScrollPane for visibility
    @FXML private ScrollPane                  homeScrollPane;
    @FXML private javafx.scene.layout.VBox   homeView;
    @FXML private javafx.scene.layout.VBox   studentsView;
    @FXML private Button navDashboardBtn;
    @FXML private Button navStudentsBtn;

    @FXML
    public void initialize() {
        loadMetricData();
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML
    private void showHomeView() {
        homeScrollPane.setVisible(true);
        homeScrollPane.setManaged(true);
        studentsView.setVisible(false);
        studentsView.setManaged(false);
        navDashboardBtn.getStyleClass().add("sidebar-button-active");
        navStudentsBtn.getStyleClass().remove("sidebar-button-active");
        loadMetricData();
    }

    @FXML
    private void showStudentsView() {
        homeScrollPane.setVisible(false);
        homeScrollPane.setManaged(false);
        studentsView.setVisible(true);
        studentsView.setManaged(true);
        navStudentsBtn.getStyleClass().add("sidebar-button-active");
        navDashboardBtn.getStyleClass().remove("sidebar-button-active");
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    @FXML
    private void loadMetricData() {
        loadingBox.setVisible(true);
        loadingBox.setManaged(true);
        metricsBox.setOpacity(0.3);

        PauseTransition pause = new PauseTransition(Duration.seconds(0.6));
        pause.setOnFinished(e -> {
            fetchDataFromDatabase();
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
            metricsBox.setOpacity(1.0);
        });
        pause.play();
    }

    private void fetchDataFromDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            if (conn == null) {
                showAlert("Database Error", "Could not connect to the database. Please restart the application.");
                return;
            }

            // ── Metric cards ─────────────────────────────────────────────────
            ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM students");
            if (rsCount.next()) totalStudentsLabel.setText(String.valueOf(rsCount.getInt(1)));

            ResultSet rsActive = stmt.executeQuery("SELECT COUNT(*) FROM students WHERE status='Active'");
            if (rsActive.next()) recentStudentsLabel.setText(String.valueOf(rsActive.getInt(1)));

            ResultSet rsBranchCount = stmt.executeQuery("SELECT COUNT(DISTINCT branch) FROM students");
            if (rsBranchCount.next() && activeCoursesLabel != null)
                activeCoursesLabel.setText(String.valueOf(rsBranchCount.getInt(1)));

            // ── Pie chart: branch distribution ────────────────────────────────
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            ResultSet rsPie = stmt.executeQuery(
                "SELECT branch, COUNT(*) as cnt FROM students GROUP BY branch ORDER BY cnt DESC");
            while (rsPie.next()) {
                pieData.add(new PieChart.Data(rsPie.getString("branch"), rsPie.getInt("cnt")));
            }
            courseChart.setData(pieData);
            courseChart.setLabelsVisible(true);

            // ── Bar chart: status breakdown ───────────────────────────────────
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Students");
            ResultSet rsBar = stmt.executeQuery(
                "SELECT status, COUNT(*) as cnt FROM students GROUP BY status ORDER BY cnt DESC");
            while (rsBar.next()) {
                series.getData().add(new XYChart.Data<>(rsBar.getString("status"), rsBar.getInt("cnt")));
            }
            statusChart.getData().clear();
            statusChart.getData().add(series);

            // ── Activity log (last 7 days) ────────────────────────────────────
            activityList.getItems().clear();
            ResultSet rsRecent = stmt.executeQuery(
                "SELECT name, status, created_at FROM students " +
                "WHERE DATE(created_at) >= DATE('now','-7 days') " +
                "ORDER BY created_at DESC LIMIT 10");
            while (rsRecent.next()) {
                String name   = rsRecent.getString("name");
                String date   = rsRecent.getString("created_at");
                String status = rsRecent.getString("status");
                activityList.getItems().add("➕ " + name + "  [" + status + "]  — added on " + date);
            }
            if (activityList.getItems().isEmpty())
                activityList.getItems().add("No students added this week. System is ready! 🎉");

        } catch (Exception e) {
            e.printStackTrace();
            totalStudentsLabel.setText("Err");
            showAlert("Dashboard Error",
                "Failed to load dashboard data.\n\nDetails: " + e.getMessage());
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/student/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setTitle("Student Management System");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/student/css/styles.css").toExternalForm());
            // Restore responsive scene
            stage.setScene(scene);
            stage.setWidth(1100);
            stage.setHeight(720);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not return to login screen.\n\nDetails: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

}
