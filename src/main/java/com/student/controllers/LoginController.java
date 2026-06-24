package com.student.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.student.database.DatabaseConnection;

public class LoginController {

    @FXML private AnchorPane rootPane;
    @FXML private Pane orb1;
    @FXML private Pane orb2;
    @FXML private VBox glassCard;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        // Sync text between the password fields
        passwordField.textProperty().bindBidirectional(passwordVisibleField.textProperty());

        // Parallax effect for orbs and glass card
        rootPane.setOnMouseMoved(event -> {
            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();
            double width  = rootPane.getWidth();
            double height = rootPane.getHeight();

            double normalizedX = (mouseX - width  / 2) / (width  / 2);
            double normalizedY = (mouseY - height / 2) / (height / 2);

            orb1.setTranslateX(-normalizedX * 60);
            orb1.setTranslateY(-normalizedY * 60);
            orb2.setTranslateX(normalizedX  * 40);
            orb2.setTranslateY(normalizedY  * 40);
            glassCard.setTranslateX(-normalizedX * 10);
            glassCard.setTranslateY(-normalizedY * 10);
        });

        // Subtle glow on click
        glassCard.setOnMousePressed(event ->
            glassCard.setStyle("-fx-border-color: rgba(59,130,246,0.5); " +
                               "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 30, 0, 0, 0);"));
        glassCard.setOnMouseReleased(event -> glassCard.setStyle(""));

        // Allow login on Enter key in password field
        passwordField.setOnAction(e -> handleLogin());
        passwordVisibleField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");

        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // ── Basic empty-field check ────────────────────────────────────────────
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            resetButton();
            return;
        }

        // ── Simple email format check ──────────────────────────────────────────
        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Please enter a valid email address.");
            resetButton();
            return;
        }

        // ── Hash the entered password before comparing ─────────────────────────
        String hashedPassword = DatabaseConnection.hashPassword(password);

        // ── Database validation ───────────────────────────────────────────────
        String query = "SELECT id FROM users WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                showAlert("Connection Error",
                    "Cannot connect to the database.\nPlease ensure the application has write access to its directory.");
                resetButton();
                return;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, email);
                pstmt.setString(2, hashedPassword);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // ── Login successful ───────────────────────────────────
                        System.out.println("Login successful for: " + email);
                        loginButton.setText("Success! ✓");
                        loginButton.setStyle("-fx-background-color: #10B981; -fx-text-fill: white;");

                        loadDashboard();
                    } else {
                        showError("Invalid email or password.");
                        resetButton();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error",
                "An unexpected database error occurred.\n\nDetails: " + e.getMessage());
            resetButton();
        }
    }

    // ── Load Dashboard ────────────────────────────────────────────────────────
    private void loadDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/student/dashboard.fxml"));
            Parent dashboardRoot = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("Student Management System — Dashboard");

            Scene scene = new Scene(dashboardRoot);
            stage.setScene(scene);

            // Responsive: set preferred size, allow full resize
            stage.setWidth(1200);
            stage.setHeight(780);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("UI Error",
                "Failed to load the dashboard.\n\nDetails: " + e.getMessage());
            resetButton();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showError(String message) {
        errorLabel.setText("⚠  " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void resetButton() {
        loginButton.setDisable(false);
        loginButton.setText("Login");
        loginButton.setStyle("");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordButton.setText("✖");
        } else {
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            togglePasswordButton.setText("👁");
        }
    }
}
