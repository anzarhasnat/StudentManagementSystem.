package com.student.database;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:student_management.db";

    // ── Connection ────────────────────────────────────────────────────────────
    public static Connection getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(URL);
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return null;
        }
    }

    // ── SHA-256 Password Hashing ──────────────────────────────────────────────
    /**
     * Hashes a plain-text password using SHA-256.
     * @param plain the raw password string
     * @return lowercase hex-encoded SHA-256 digest
     */
    public static String hashPassword(String plain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java SE — should never happen
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // ── Initialise DB ─────────────────────────────────────────────────────────
    public static void initializeDatabase() {
        System.out.println("Initializing Database...");

        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    email    TEXT    NOT NULL UNIQUE,
                    password TEXT    NOT NULL
                );
                """;

        String createStudentsTable = """
                CREATE TABLE IF NOT EXISTS students (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    rollNumber TEXT NOT NULL,
                    branch TEXT NOT NULL,
                    section TEXT NOT NULL,
                    status TEXT DEFAULT 'Active',
                    enrollmentDate TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createAttendanceTable = """
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    status TEXT NOT NULL,
                    FOREIGN KEY(student_id) REFERENCES students(id)
                );
                """;

        String createTeacherNotesTable = """
                CREATE TABLE IF NOT EXISTS teacher_notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id INTEGER NOT NULL,
                    note TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(student_id) REFERENCES students(id)
                );
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createUsersTable);
            stmt.execute(createStudentsTable);
            stmt.execute(createAttendanceTable);
            stmt.execute(createTeacherNotesTable);
            System.out.println("Database initialized successfully.");

            addDefaultAdminIfMissing(stmt);
            seedSampleData(stmt);

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    // ── Default admin (password stored as SHA-256 hash) ───────────────────────
    private static void addDefaultAdminIfMissing(Statement stmt) throws SQLException {
        var rs = stmt.executeQuery("SELECT count(*) FROM users WHERE email = 'admin@college.edu';");
        if (rs.next() && rs.getInt(1) == 0) {
            String hashedPassword = hashPassword("admin123");
            stmt.executeUpdate(
                "INSERT INTO users (email, password) VALUES ('admin@college.edu', '" + hashedPassword + "');"
            );
            System.out.println("Default admin created: admin@college.edu / admin123");
        }
    }

    // ── Sample data seed ──────────────────────────────────────────────────────
    private static void seedSampleData(Statement stmt) throws SQLException {
        var rs = stmt.executeQuery("SELECT COUNT(*) FROM students");
        if (rs.next() && rs.getInt(1) > 0) return; // Already has data

        String[][] students = {
            {"Aarav",     "Sharma",    "20", "Computer Science",       "Active",    "2023-08-01"},
            {"Emma",      "Johnson",   "22", "Mathematics",            "Active",    "2023-08-01"},
            {"Liam",      "Williams",  "21", "Physics",                "Active",    "2023-09-01"},
            {"Olivia",    "Brown",     "23", "Business Administration","Active",    "2022-08-01"},
            {"Noah",      "Jones",     "19", "Computer Science",       "Active",    "2024-01-15"},
            {"Sophia",    "Garcia",    "24", "Medicine",               "Graduated", "2021-08-01"},
            {"Ethan",     "Martinez",  "22", "Engineering",            "Active",    "2023-08-01"},
            {"Mia",       "Davis",     "20", "Arts & Design",          "Active",    "2023-09-01"},
            {"Lucas",     "Rodriguez", "21", "Law",                    "Active",    "2022-09-01"},
            {"Isabella",  "Wilson",    "23", "Economics",              "Graduated", "2021-09-01"},
            {"James",     "Anderson",  "25", "Medicine",               "Active",    "2022-08-01"},
            {"Amelia",    "Taylor",    "19", "Psychology",             "Active",    "2024-01-20"},
            {"Benjamin",  "Thomas",    "22", "Computer Science",       "Inactive",  "2022-08-01"},
            {"Charlotte", "Jackson",   "21", "Mathematics",            "Active",    "2023-08-01"},
            {"Henry",     "White",     "20", "Engineering",            "Active",    "2023-09-01"},
            {"Harper",    "Harris",    "24", "Business Administration","Graduated", "2021-08-01"},
            {"Alexander", "Martin",    "22", "Physics",                "Active",    "2023-08-01"},
            {"Evelyn",    "Thompson",  "19", "Arts & Design",          "Active",    "2024-02-01"},
            {"Michael",   "Lee",       "23", "Law",                    "Inactive",  "2022-09-01"},
            {"Abigail",   "Perez",     "21", "Economics",              "Active",    "2023-08-01"},
            {"Daniel",    "Clark",     "20", "Computer Science",       "Active",    "2023-09-01"},
            {"Ella",      "Lewis",     "22", "Psychology",             "Active",    "2023-08-01"},
            {"Matthew",   "Robinson",  "25", "Medicine",               "Graduated", "2020-08-01"},
            {"Scarlett",  "Walker",    "19", "Engineering",            "Active",    "2024-01-10"},
            {"Jack",      "Hall",      "21", "Mathematics",            "Active",    "2023-09-01"}
        };

        String sql = "INSERT INTO students (name, rollNumber, branch, section, status, enrollmentDate) VALUES (?,?,?,?,?,?)";
        try (var pstmt = stmt.getConnection().prepareStatement(sql)) {
            for (String[] s : students) {
                // s[0]=first name, s[1]=last name, s[2]=age (unused), s[3]=branch, s[4]=status, s[5]=enrollmentDate
                String fullName = s[0] + " " + s[1];
                String rollNum = "RN" + s[2]; // generate roll number from age placeholder
                String branch = s[3];
                String section = "A"; // default section placeholder
                String status = s[4];
                String enrollmentDate = s[5];
                pstmt.setString(1, fullName);
                pstmt.setString(2, rollNum);
                pstmt.setString(3, branch);
                pstmt.setString(4, section);
                pstmt.setString(5, status);
                pstmt.setString(6, enrollmentDate);
                pstmt.executeUpdate();
            }
        }
        System.out.println("Seeded 25 sample students.");
    }
}
