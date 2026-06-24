package com.student.controllers;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

import com.student.database.DatabaseConnection;
import com.student.models.Student;

public class StudentModalController {

    @FXML private Label modalTitleLabel;
    @FXML private TextField nameField;
    @FXML private TextField rollNumberField;
    @FXML private TextField branchField;
    // Section and status fields removed from UI
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private Student studentToEdit = null;
    private StudentsController parentController;

    public void setParentController(StudentsController parentController) {
        this.parentController = parentController;
    }

    public void setStudentToEdit(Student student) {
        this.studentToEdit = student;
        if (student != null) {
            modalTitleLabel.setText("Edit Student");
            nameField.setText(student.getName());
            rollNumberField.setText(student.getRollNumber());
            branchField.setText(student.getBranch());
            // section and status fields removed; defaults will be used
            saveButton.setText("Update Student");
        }
    }

    @FXML
    private void handleSave() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String name = nameField.getText().trim();
        String rollNumber = rollNumberField.getText().trim();
        String branch = branchField.getText().trim();
        String section = "A"; // default section
        String status = "Active"; // default status

        // Validation
        if (name.isEmpty() || rollNumber.isEmpty() || branch.isEmpty()) {
            showError("All fields are required.");
            return;
        }



        // Database Operation
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (studentToEdit == null) {
                // Insert New Student
                String query = "INSERT INTO students (name, rollNumber, branch, section, status, enrollmentDate) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, rollNumber);
                    pstmt.setString(3, branch);
                    pstmt.setString(4, section);
                    pstmt.setString(5, status);
                    pstmt.setString(6, LocalDate.now().toString());
                    pstmt.executeUpdate();
                }
            } else {
                // Update Existing Student
                String query = "UPDATE students SET name = ?, rollNumber = ?, branch = ?, section = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, rollNumber);
                    pstmt.setString(3, branch);
                    pstmt.setString(4, section);
                    pstmt.setString(5, status);
                    pstmt.setInt(6, studentToEdit.getId());
                    pstmt.executeUpdate();
                }
            }

            // Refresh table and close modal
            if (parentController != null) {
                parentController.loadStudentData();
            }
            closeModal();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Database error occurred.");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @FXML
    private void handleCancel() {
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
