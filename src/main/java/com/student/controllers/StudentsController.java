package com.student.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.Optional;
import java.io.PrintWriter;
import java.io.File;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.FileChooser;

import com.student.database.DatabaseConnection;
import com.student.models.Student;

public class StudentsController {

    // ── Table & toolbar ───────────────────────────────────────────────────────
    @FXML private TextField   searchField;
    @FXML private Button      editButton;
    @FXML private Button      deleteButton;
    @FXML private Label       recordCountLabel;

    @FXML private TableView<Student>              studentsTable;
    @FXML private TableColumn<Student, Integer>   colId;
    @FXML private TableColumn<Student, String>    colFirstName;
    @FXML private TableColumn<Student, String>    colLastName;
    @FXML private TableColumn<Student, Integer>   colAge;
    @FXML private TableColumn<Student, String>    colCourse;
    @FXML private TableColumn<Student, String>    colStatus;
    @FXML private TableColumn<Student, String>    colDate;
    @FXML private TableColumn<Student, String>    colAttendance; // New attendance column

    // ── Pagination controls ───────────────────────────────────────────────────
    @FXML private Label            pageInfoLabel;
    @FXML private Button           firstPageBtn;
    @FXML private Button           prevPageBtn;
    @FXML private Button           nextPageBtn;
    @FXML private Button           lastPageBtn;
    @FXML private ComboBox<Integer> pageSizeCombo;

    // ── Profile panel ─────────────────────────────────────────────────────────
    @FXML private VBox  profilePanel;
    @FXML private Label profileName;
    @FXML private Label profileStatusBadge;
    @FXML private Label profileId;
    @FXML private Label profileCourse;
    @FXML private Label profileAge;
    @FXML private Label profileEnrollDate;

    // ── Data state ────────────────────────────────────────────────────────────
    private final ObservableList<Student> studentList = FXCollections.observableArrayList();
    private final ObservableList<Student> pagedList   = FXCollections.observableArrayList();
    private FilteredList<Student>         filteredData;
    private SortedList<Student>           sortedData;

    private int currentPage = 1;
    private int pageSize    = 10;
    private int totalPages  = 1;

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupTableColumns();
        setupPaginationControls();
        loadStudentData();
        setupSearchFilter();

        studentsTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldSel, newSel) -> {
                boolean selected = newSel != null;
                editButton.setDisable(!selected);
                deleteButton.setDisable(!selected);
                if (selected) showProfilePanel(newSel);
                else          hideProfilePanel();
            });

        Label placeholder = new Label("No students found. Click '+ Add Student' to begin.");
        placeholder.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 16px;");
        studentsTable.setPlaceholder(placeholder);
    }

    // ── Table column setup ────────────────────────────────────────────────────
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setVisible(false);

        colId.setMinWidth(0);
        colId.setPrefWidth(0);
        colId.setMaxWidth(0);
        studentsTable.getColumns().remove(colId);
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("name")); // shows full name
        colLastName.setCellValueFactory(new PropertyValueFactory<>("rollNumber")); // shows roll number
        colAge.setCellValueFactory(new PropertyValueFactory<>("branch")); // shows branch
        colAge.setVisible(false); // hide age/branch column as requested
        // Completely remove the column from the table to avoid empty header
        studentsTable.getColumns().remove(colAge);
        colCourse.setCellValueFactory(new PropertyValueFactory<>("section")); // shows section
        colCourse.setVisible(false); // hide section column as requested
        studentsTable.getColumns().remove(colCourse);
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setVisible(false); // hide status column as requested
        studentsTable.getColumns().remove(colStatus);
        colDate.setCellValueFactory(new PropertyValueFactory<>("enrollmentDate"));
        // Attendance column – shows latest attendance status badge
        colAttendance.setCellValueFactory(cellData -> {
            String status = com.student.database.AttendanceDAO.getLatestAttendanceStatus(cellData.getValue().getId());
            return new javafx.beans.property.SimpleStringProperty(status != null ? status : "");
        });
        colAttendance.setCellFactory(col -> new javafx.scene.control.TableCell<Student, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null || status.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String emoji;
                    switch (status) {
                        case "Present": emoji = "✅"; break;
                        case "Absent":  emoji = "❌"; break;
                        case "Late":    emoji = "⏰"; break;
                        default:          emoji = status;
                    }
                    setText(emoji);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
        // Add the attendance column to the table (it is already defined in FXML)
        // Note: we do not remove it; it will be visible.


        // Color-coded status badge
        colStatus.setCellFactory(col -> new TableCell<Student, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null); setGraphic(null); setStyle(""); return;
                }
                Label badge = new Label(status);
                String base = "-fx-padding: 3 12 3 12; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-radius: 20; -fx-border-width: 1;";
                String color = switch (status) {
                    case "Active"    -> "-fx-background-color: rgba(16,185,129,0.2);  -fx-text-fill: #10B981; -fx-border-color: #10B981;";
                    case "Inactive"  -> "-fx-background-color: rgba(239,68,68,0.2);   -fx-text-fill: #EF4444; -fx-border-color: #EF4444;";
                    case "Graduated" -> "-fx-background-color: rgba(139,92,246,0.2);  -fx-text-fill: #8B5CF6; -fx-border-color: #8B5CF6;";
                    default          -> "-fx-background-color: rgba(156,163,175,0.2); -fx-text-fill: #9CA3AF; -fx-border-color: #9CA3AF;";
                };
                badge.setStyle(base + color);
                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });
    }

    // ── Pagination controls setup ─────────────────────────────────────────────
    private void setupPaginationControls() {
        pageSizeCombo.setItems(FXCollections.observableArrayList(5, 10, 25, 50));
        pageSizeCombo.setValue(pageSize);
        pageSizeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                pageSize    = newVal;
                currentPage = 1;
                updatePagedData();
            }
        });
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    public void loadStudentData() {
        studentList.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT * FROM students ORDER BY id DESC")) {

            while (rs.next()) {
                studentList.add(new Student(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("rollNumber"),
                    rs.getString("branch"),
                    rs.getString("section"),
                    rs.getString("status"),
                    rs.getString("enrollmentDate")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Refresh page if filter/sorted pipeline is already live
        if (sortedData != null) {
            currentPage = 1;
            updatePagedData();
        }
    }

    // ── Search & filter ───────────────────────────────────────────────────────
    private void setupSearchFilter() {
        filteredData = new FilteredList<>(studentList, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(s -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String f = newVal.toLowerCase();
                if (s.getName().toLowerCase().contains(f))        return true;
                if (s.getRollNumber().toLowerCase().contains(f))   return true;
                if (String.valueOf(s.getId()).contains(f))          return true;
                if (s.getBranch().toLowerCase().contains(f))       return true;
                if (s.getSection().toLowerCase().contains(f))      return true;
                if (s.getStatus().toLowerCase().contains(f))       return true;
                return false;
            });
            currentPage = 1;
            updatePagedData();
        });

        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(studentsTable.comparatorProperty());

        // Re-page when user clicks a column header to sort
        studentsTable.comparatorProperty().addListener((obs, o, n) -> updatePagedData());

        studentsTable.setItems(pagedList);
        updatePagedData();
    }

    // ── Pagination logic ──────────────────────────────────────────────────────
    private void updatePagedData() {
        int total  = sortedData != null ? sortedData.size() : 0;
        totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * pageSize;
        int to   = Math.min(from + pageSize, total);

        pagedList.setAll(sortedData != null ? sortedData.subList(from, to) : Collections.emptyList());

        pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);
        recordCountLabel.setText("Showing " + pagedList.size() + " of " + total + " records");

        firstPageBtn.setDisable(currentPage == 1);
        prevPageBtn.setDisable(currentPage == 1);
        nextPageBtn.setDisable(currentPage >= totalPages);
        lastPageBtn.setDisable(currentPage >= totalPages);
    }

    @FXML private void handleFirstPage() { currentPage = 1;          updatePagedData(); }
    @FXML private void handlePrevPage()  { if (currentPage > 1)          { currentPage--; updatePagedData(); } }
    @FXML private void handleNextPage()  { if (currentPage < totalPages)  { currentPage++; updatePagedData(); } }
    @FXML private void handleLastPage()  { currentPage = totalPages;  updatePagedData(); }

    // ── CSV Export ────────────────────────────────────────────────────────────
    @FXML
    private void handleExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Student Data as CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("students_export.csv");

        File file = fileChooser.showSaveDialog(studentsTable.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("ID,Name,RollNumber,Branch,Section,Status,Enrollment Date");
                // Export ALL filtered records (not just the current page)
                for (Student s : (sortedData != null ? sortedData : studentList)) {
                    writer.println(s.getId() + "," + s.getName() + "," + s.getRollNumber() + ","
                        + s.getBranch() + "," + s.getSection() + "," + s.getStatus() + "," + s.getEnrollmentDate());
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Data exported to " + file.getName());
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    // ── Modal open/close ──────────────────────────────────────────────────────
    @FXML private void showAddModal()  { openModal(null); }

    @FXML
    private void showEditModal() {
        Student selected = studentsTable.getSelectionModel().getSelectedItem();
        if (selected != null) openModal(selected);
    }

    @FXML
    private void handleMarkAttendance() {
        Student selected = studentsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/student/attendance_modal.fxml"));
                javafx.scene.Parent root = loader.load();
                com.student.controllers.AttendanceModalController controller = loader.getController();
                controller.setStudent(selected);
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
                stage.setScene(new javafx.scene.Scene(root));
                stage.showAndWait();
                // Refresh attendance column after returning
                studentsTable.refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openModal(Student student) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/student/student_modal.fxml"));
            Parent root = loader.load();

            StudentModalController controller = loader.getController();
            controller.setParentController(this);
            controller.setStudentToEdit(student);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    @FXML
    private void handleDeleteStudent() {
        Student selected = studentsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Confirmation");
            confirm.setHeaderText("Delete " + selected.getName() + "?");
            confirm.setContentText("This action cannot be undone.");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM students WHERE id = ?")) {
                    pstmt.setInt(1, selected.getId());
                    pstmt.executeUpdate();
                    hideProfilePanel();
                    loadStudentData();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ── Profile Panel ─────────────────────────────────────────────────────────
    private void showProfilePanel(Student s) {
        profileName.setText(s.getName());
        profileId.setText("#" + s.getId());
        // Reusing existing labels to show branch and section
        profileCourse.setText(s.getBranch());
        // Hide the age/section label as per request
        profileAge.setVisible(false);
        profileAge.setManaged(false);
        profileEnrollDate.setText(s.getEnrollmentDate());

        profileStatusBadge.setText(s.getStatus());
        String base = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; "
                    + "-fx-padding: 3 14 3 14; -fx-border-radius: 20; -fx-border-width: 1;";
        String color = switch (s.getStatus()) {
            case "Active"    -> "-fx-text-fill: #10B981; -fx-background-color: rgba(16,185,129,0.2);  -fx-border-color: #10B981;";
            case "Inactive"  -> "-fx-text-fill: #EF4444; -fx-background-color: rgba(239,68,68,0.2);   -fx-border-color: #EF4444;";
            case "Graduated" -> "-fx-text-fill: #8B5CF6; -fx-background-color: rgba(139,92,246,0.2);  -fx-border-color: #8B5CF6;";
            default          -> "-fx-text-fill: #9CA3AF; -fx-background-color: rgba(156,163,175,0.2); -fx-border-color: #9CA3AF;";
        };
        profileStatusBadge.setStyle(base + color);

        profilePanel.setVisible(true);
        profilePanel.setManaged(true);
    }

    private void hideProfilePanel() {
        profilePanel.setVisible(false);
        profilePanel.setManaged(false);
    }
}
