package com.student.database;

import com.student.models.Attendance;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AttendanceDAO {

    // Insert a new attendance record
    public static void insertAttendance(int studentId, String date, String status) {
        String sql = "INSERT INTO attendance (student_id, date, status) VALUES (?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setString(2, date);
            pstmt.setString(3, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get the latest attendance status for a student (most recent date)
    public static String getLatestAttendanceStatus(int studentId) {
        String sql = "SELECT status FROM attendance WHERE student_id = ? ORDER BY date DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Optional: fetch recent attendance list (e.g., last 5) – not used directly now
    public static java.util.List<Attendance> getRecentAttendance(int studentId, int limit) {
        java.util.List<Attendance> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM attendance WHERE student_id = ? ORDER BY date DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Attendance a = new Attendance(
                    rs.getInt("id"),
                    rs.getInt("student_id"),
                    rs.getString("date"),
                    rs.getString("status")
                );
                list.add(a);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
