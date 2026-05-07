package oop2_project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Formatter;
import java.sql.*;

public class AdminDashboard extends JFrame {

    private JTable instructorTable, courseTable;
    private DefaultTableModel instructorModel, courseModel;

    public AdminDashboard() {
        setTitle("Admin Dashboard");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(245, 245, 245));
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Admin Dashboard - Manage Instructors & Courses", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Instructors", buildInstructorPanel());
        tabbedPane.addTab("Courses", buildCoursePanel());
        add(tabbedPane, BorderLayout.CENTER);

        // Back button in a separate bottom panel
        JPanel bottomPanel = new JPanel();
        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.addActionListener(e -> {
            new WelcomeScreen().setVisible(true);
            dispose();
        });
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
        loadInstructors();
        loadCourses();
    }

    private JPanel buildInstructorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        instructorModel = new DefaultTableModel(new String[]{"ID", "Name", "Email"}, 0);
        instructorTable = new JTable(instructorModel);
        panel.add(new JScrollPane(instructorTable), BorderLayout.CENTER);
        panel.add(buildInstructorButtonPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildInstructorButtonPanel() {
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Instructor");
        addButton.addActionListener(e -> openAddInstructorDialog());
        buttonPanel.add(addButton);

        JButton updateButton = new JButton("Update Instructor");
        updateButton.addActionListener(e -> openUpdateInstructorDialog());
        buttonPanel.add(updateButton);

        JButton deleteButton = new JButton("Delete Instructor");
        deleteButton.addActionListener(e -> deleteInstructor());
        buttonPanel.add(deleteButton);

        JButton exportButton = new JButton("Export Data to TXT");
        exportButton.addActionListener(e -> exportToTextFile());
        buttonPanel.add(exportButton);

        return buttonPanel;
    }

    private JPanel buildCoursePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        courseModel = new DefaultTableModel(new String[]{"ID", "Title", "Instructor", "Department"}, 0);
        courseTable = new JTable(courseModel);
        panel.add(new JScrollPane(courseTable), BorderLayout.CENTER);
        panel.add(buildCourseButtonPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCourseButtonPanel() {
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Course");
        addButton.addActionListener(e -> openAddCourseDialog());
        buttonPanel.add(addButton);

        JButton updateButton = new JButton("Update Course");
        updateButton.addActionListener(e -> openUpdateCourseDialog());
        buttonPanel.add(updateButton);

        JButton deleteButton = new JButton("Delete Course");
        deleteButton.addActionListener(e -> deleteCourse());
        buttonPanel.add(deleteButton);

        return buttonPanel;
    }

    private void loadInstructors() {
        instructorModel.setRowCount(0);
        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT user_id, name, email FROM User WHERE role = 'instructor'")) {
            while (rs.next()) {
                instructorModel.addRow(new Object[]{rs.getInt("user_id"), rs.getString("name"), rs.getString("email")});
            }
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void loadCourses() {
        courseModel.setRowCount(0);
        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.course_id, c.title, u.name as instructor, d.name as department " +
                     "FROM Course c LEFT JOIN User u ON c.instructor_id = u.user_id " +
                     "LEFT JOIN Department d ON c.department_id = d.department_id")) {
            while (rs.next()) {
                courseModel.addRow(new Object[]{rs.getInt("course_id"), rs.getString("title"), rs.getString("instructor"), rs.getString("department")});
            }
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void openAddInstructorDialog() {
    JTextField nameField = new JTextField();
    JTextField emailField = new JTextField();
    JTextField passwordField = new JTextField();

    JPanel panel = new JPanel(new GridLayout(0, 2));
    panel.add(new JLabel("Name:"));
    panel.add(nameField);
    panel.add(new JLabel("Email:"));
    panel.add(emailField);
    panel.add(new JLabel("Password:"));
    panel.add(passwordField);

    if (JOptionPane.showConfirmDialog(this, panel, "Add Instructor", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        
        if (nameField.getText().trim().isEmpty() ||
            emailField.getText().trim().isEmpty() ||
            passwordField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.", "Missing Data", JOptionPane.ERROR_MESSAGE);
            return; // don't continue if any field is empty
        }

        try (Connection conn = DBConnection.connect()) {
            String insertUser = "INSERT INTO User (name, email, password, role) VALUES (?, ?, ?, 'instructor')";
            try (PreparedStatement stmtUser = conn.prepareStatement(insertUser)) {
                stmtUser.setString(1, nameField.getText());
                stmtUser.setString(2, emailField.getText());
                stmtUser.setString(3, passwordField.getText());
                stmtUser.executeUpdate();
                JOptionPane.showMessageDialog(this, "Instructor added.");
                loadInstructors();
            }
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }
}


    private void openUpdateInstructorDialog() {
        int row = instructorTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an instructor.");
            return;
        }
        int userId = (int) instructorModel.getValueAt(row, 0);
        String name = (String) instructorModel.getValueAt(row, 1);
        String email = (String) instructorModel.getValueAt(row, 2);

        JTextField nameField = new JTextField(name);
        JTextField emailField = new JTextField(email);
        JTextField passwordField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("New Password:"));
        panel.add(passwordField);

        if (JOptionPane.showConfirmDialog(this, panel, "Update Instructor", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.connect()) {
                String updateUser = passwordField.getText().isEmpty() ?
                        "UPDATE User SET name=?, email=? WHERE user_id=?" :
                        "UPDATE User SET name=?, email=?, password=? WHERE user_id=?";
                try (PreparedStatement stmtUser = conn.prepareStatement(updateUser)) {
                    stmtUser.setString(1, nameField.getText());
                    stmtUser.setString(2, emailField.getText());
                    if (passwordField.getText().isEmpty()) {
                        stmtUser.setInt(3, userId);
                    } else {
                        stmtUser.setString(3, passwordField.getText());
                        stmtUser.setInt(4, userId);
                    }
                    stmtUser.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Instructor updated.");
                    loadInstructors();
                }
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        }
    }

    private void deleteInstructor() {
        int row = instructorTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an instructor.");
            return;
        }
        int userId = (int) instructorModel.getValueAt(row, 0);

        if (JOptionPane.showConfirmDialog(this, "Delete this instructor?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.connect()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM User WHERE user_id = ?")) {
                    stmt.setInt(1, userId);
                    stmt.executeUpdate();
                    loadInstructors();
                    JOptionPane.showMessageDialog(this, "Instructor deleted.");
                }
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        }
    }

    private void openAddCourseDialog() {
        JTextField titleField = new JTextField();
        
        JComboBox<String> instructorBox = new JComboBox<>();
        JComboBox<String> departmentBox = new JComboBox<>();
      
        
        try (Connection conn = DBConnection.connect()) {
            ResultSet rs1 = conn.prepareStatement("SELECT user_id, name FROM User WHERE role='instructor'").executeQuery();
            while (rs1.next()) instructorBox.addItem(rs1.getInt("user_id") + " - " + rs1.getString("name"));

            ResultSet rs2 = conn.prepareStatement("SELECT department_id, name FROM Department").executeQuery();
            while (rs2.next()) departmentBox.addItem(rs2.getInt("department_id") + " - " + rs2.getString("name"));
        } catch (SQLException e) {
            showError(e.getMessage());
            return;
        }

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Instructor:"));
        panel.add(instructorBox);
        panel.add(new JLabel("Department:"));
        panel.add(departmentBox);
        
        
        if (JOptionPane.showConfirmDialog(this, panel, "Add Course", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            
            if (titleField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.", "Missing Data", JOptionPane.ERROR_MESSAGE);
            return; // don't continue if any field is empty
           }
            try (Connection conn = DBConnection.connect()) {
                String selectedInstructor = (String) instructorBox.getSelectedItem();
                String selectedDepartment = (String) departmentBox.getSelectedItem();
                int instId = Integer.parseInt(selectedInstructor.split(" - ")[0]);
                int deptId = Integer.parseInt(selectedDepartment.split(" - ")[0]);

                String insertCourse = "INSERT INTO Course (title, instructor_id, department_id) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertCourse)) {
                    stmt.setString(1, titleField.getText());
                    stmt.setInt(2, instId);
                    stmt.setInt(3, deptId);
                    stmt.executeUpdate();
                    loadCourses();
                    JOptionPane.showMessageDialog(this, "Course added.");
                }
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        }
    }

    private void openUpdateCourseDialog() {
        int row = courseTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a course.");
            return;
        }
        int courseId = (int) courseModel.getValueAt(row, 0);
        String title = (String) courseModel.getValueAt(row, 1);

        JTextField titleField = new JTextField(title);
        JComboBox<String> instructorBox = new JComboBox<>();
        JComboBox<String> departmentBox = new JComboBox<>();

        try (Connection conn = DBConnection.connect()) {
            ResultSet rs1 = conn.prepareStatement("SELECT user_id, name FROM User WHERE role='instructor'").executeQuery();
            while (rs1.next()) instructorBox.addItem(rs1.getInt("user_id") + " - " + rs1.getString("name"));

            ResultSet rs2 = conn.prepareStatement("SELECT department_id, name FROM Department").executeQuery();
            while (rs2.next()) departmentBox.addItem(rs2.getInt("department_id") + " - " + rs2.getString("name"));
        } catch (SQLException e) {
            showError(e.getMessage());
            return;
        }

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Instructor:"));
        panel.add(instructorBox);
        panel.add(new JLabel("Department:"));
        panel.add(departmentBox);

        if (JOptionPane.showConfirmDialog(this, panel, "Update Course", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.connect()) {
                int instId = Integer.parseInt(((String) instructorBox.getSelectedItem()).split(" - ")[0]);
                int deptId = Integer.parseInt(((String) departmentBox.getSelectedItem()).split(" - ")[0]);

                String updateCourse = "UPDATE Course SET title=?, instructor_id=?, department_id=? WHERE course_id=?";
                try (PreparedStatement stmt = conn.prepareStatement(updateCourse)) {
                    stmt.setString(1, titleField.getText());
                    stmt.setInt(2, instId);
                    stmt.setInt(3, deptId);
                    stmt.setInt(4, courseId);
                    stmt.executeUpdate();
                    loadCourses();
                    JOptionPane.showMessageDialog(this, "Course updated.");
                }
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        }
    }

    private void deleteCourse() {
        int row = courseTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a course.");
            return;
        }
        int courseId = (int) courseModel.getValueAt(row, 0);

        if (JOptionPane.showConfirmDialog(this, "Delete this course?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.connect()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Course WHERE course_id=?")) {
                    stmt.setInt(1, courseId);
                    stmt.executeUpdate();
                    loadCourses();
                    JOptionPane.showMessageDialog(this, "Course deleted.");
                }
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        }
    }

    private void exportToTextFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (Formatter formatter = new Formatter(fileToSave)) {
                try (Connection conn = DBConnection.connect();
                     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM User")) {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        formatter.format("User ID: %d\nEmail: %s\nRole: %s\n\n",
                                rs.getInt("user_id"), rs.getString("email"), rs.getString("role"));
                    }
                    JOptionPane.showMessageDialog(this, "Data exported successfully to " + fileToSave.getAbsolutePath());
                }
            } catch (FileNotFoundException | SQLException e) {
                showError(e.getMessage());
            }
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, "ERROR: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
