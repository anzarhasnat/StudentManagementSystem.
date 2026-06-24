package com.student.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Student {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty rollNumber;
    private final StringProperty branch;
    private final StringProperty section;
    private final StringProperty status;
    private final StringProperty enrollmentDate;

    public Student(int id, String name, String rollNumber, String branch, String section, String status, String enrollmentDate) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.rollNumber = new SimpleStringProperty(rollNumber);
        this.branch = new SimpleStringProperty(branch);
        this.section = new SimpleStringProperty(section);
        this.status = new SimpleStringProperty(status);
        this.enrollmentDate = new SimpleStringProperty(enrollmentDate);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }
    public void setId(int id) { this.id.set(id); }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public void setName(String name) { this.name.set(name); }

    public String getRollNumber() { return rollNumber.get(); }
    public StringProperty rollNumberProperty() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber.set(rollNumber); }

    public String getBranch() { return branch.get(); }
    public StringProperty branchProperty() { return branch; }
    public void setBranch(String branch) { this.branch.set(branch); }

    public String getSection() { return section.get(); }
    public StringProperty sectionProperty() { return section; }
    public void setSection(String section) { this.section.set(section); }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public void setStatus(String status) { this.status.set(status); }

    public String getEnrollmentDate() { return enrollmentDate.get(); }
    public StringProperty enrollmentDateProperty() { return enrollmentDate; }
    public void setEnrollmentDate(String enrollmentDate) { this.enrollmentDate.set(enrollmentDate); }
}
