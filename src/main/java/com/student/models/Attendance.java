package com.student.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Attendance {
    private final IntegerProperty id;
    private final IntegerProperty studentId;
    private final StringProperty date; // ISO string (yyyy-MM-dd)
    private final StringProperty status; // Present, Absent, Late

    public Attendance(int id, int studentId, String date, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.studentId = new SimpleIntegerProperty(studentId);
        this.date = new SimpleStringProperty(date);
        this.status = new SimpleStringProperty(status);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }
    public void setId(int id) { this.id.set(id); }

    public int getStudentId() { return studentId.get(); }
    public IntegerProperty studentIdProperty() { return studentId; }
    public void setStudentId(int studentId) { this.studentId.set(studentId); }

    public String getDate() { return date.get(); }
    public StringProperty dateProperty() { return date; }
    public void setDate(String date) { this.date.set(date); }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public void setStatus(String status) { this.status.set(status); }
}
