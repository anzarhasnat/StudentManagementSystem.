module com.student {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.student to javafx.fxml;
    opens com.student.controllers to javafx.fxml;
opens com.student.models to javafx.base;
    exports com.student;
}
