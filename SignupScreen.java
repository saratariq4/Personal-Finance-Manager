package oop2_project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class InstructorDashboard extends JFrame {
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private int instructorId;

    public InstructorDashboard(int instructorId) {
        this.instructorId = instructorId;

        setTitle("Instructor Dashboard");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 245, 245));

        JLabel title = new JLabel("Instructor Dashboard - Manage Grades", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"User ID", "Name", "Course", "Score", "Letter"}, 0);
        studentTable = new JTable(tableModel);
        add(new JScrollPane(studentTable), BorderLayout.CENTER);

        JButton updateGradeBtn = new JButton("Update Grade");
        JButton addGradeBtn = new JButton("Add Grade");
        JButton viewStudentsBtn = new JButton("View Students in Course");
        JButton manageAssignmentsBtn = new JButton("Manage Assignments");
        JButton viewSubmissionsBtn = new JButton("View Submissions");

        updateGradeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGrade();
            }
        });

        addGradeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAddGradeDialog();
            }
        });

        viewStudentsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openViewStudentsDialog();
            }
        });

        manageAssignmentsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openManageAssignmentsDialog();
            }
        });

        viewSubmissionsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openViewSubmissionsDialog();
            }
        });

         JPanel bottom = new JPanel();
         bottom.add(updateGradeBtn);
         bottom.add(addGradeBtn);
         bottom.add(viewStudentsBtn);
         bottom.add(manageAssignmentsBtn);
         bottom.add(viewSubmissionsBtn);

         JButton backButton = new JButton("Back");
         backButton.addActionListener(e -> {
                new WelcomeScreen().setVisible(true);
                dispose();
            });
         bottom.add(backButton);

        add(bottom, BorderLayout.SOUTH);

        loadStudentGrades();
        setVisible(true);
    }

    private void loadStudentGrades() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT u.user_id, u.name, c.title, g.score, g.letter " +
                         "FROM grade g " +
                         "JOIN registration r ON g.registration_id = r.registration_id " +
                         "JOIN user u ON r.student_id = u.user_id " +
                         "JOIN course c ON r.course_id = c.course_id " +
                         "WHERE c.instructor_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, instructorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("title"),
                        rs.getInt("score"),
                        rs.getString("letter")
                });
            }
        } catch (SQLException e) {
            showError("Error loading grades: " + e.getMessage());
        }
    }

    private void updateGrade() {
        int row = studentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student row to update the grade.");
            return;
        }

        int userId = (int) tableModel.getValueAt(row, 0);
        String courseName = (String) tableModel.getValueAt(row, 2);

        String newScoreStr = JOptionPane.showInputDialog(this, "Enter new score:");
        String newLetter = JOptionPane.showInputDialog(this, "Enter letter grade:");

        if (newScoreStr != null && newLetter != null && !newScoreStr.trim().isEmpty() && !newLetter.trim().isEmpty()) {
            try {
                int newScore = Integer.parseInt(newScoreStr);

                try (Connection conn = DBConnection.connect()) {
                    String sql = "UPDATE grade g " +
                                 "JOIN registration r ON g.registration_id = r.registration_id " +
                                 "JOIN user u ON r.student_id = u.user_id " +
                                 "JOIN course c ON r.course_id = c.course_id " +
                                 "SET g.score = ?, g.letter = ? " +
                                 "WHERE u.user_id = ? AND c.title = ? AND c.instructor_id = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, newScore);
                    stmt.setString(2, newLetter);
                    stmt.setInt(3, userId);
                    stmt.setString(4, courseName);
                    stmt.setInt(5, instructorId);
                    stmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Grade updated successfully.");
                    loadStudentGrades();
                }
            } catch (NumberFormatException e) {
                showError("Invalid score format.");
            } catch (SQLException e) {
                showError("Update failed: " + e.getMessage());
            }
        }
    }

    private void openAddGradeDialog() {
        JComboBox<String> registrationBox = new JComboBox<>();
        JTextField scoreField = new JTextField();
        JTextField letterField = new JTextField();

        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT r.registration_id, u.name, c.title FROM registration r " +
                         "JOIN user u ON r.student_id = u.user_id " +
                         "JOIN course c ON r.course_id = c.course_id " +
                         "LEFT JOIN grade g ON r.registration_id = g.registration_id " +
                         "WHERE g.registration_id IS NULL AND c.instructor_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, instructorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int regId = rs.getInt("registration_id");
                String studentName = rs.getString("name");
                String courseTitle = rs.getString("title");
                registrationBox.addItem(regId + " - " + studentName + " | " + courseTitle);
            }

            if (registrationBox.getItemCount() == 0) {
                JOptionPane.showMessageDialog(this, "All registered students already have grades.");
                return;
            }
        } catch (SQLException e) {
            showError("Failed to load registrations: " + e.getMessage());
            return;
        }

        JPanel inputPanel = new JPanel(new GridLayout(0, 2));
        inputPanel.add(new JLabel("Select Student-Course:"));
        inputPanel.add(registrationBox);
        inputPanel.add(new JLabel("Score:"));
        inputPanel.add(scoreField);
        inputPanel.add(new JLabel("Letter Grade:"));
        inputPanel.add(letterField);

        int option = JOptionPane.showConfirmDialog(this, inputPanel, "Add Grade", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                int registrationId = Integer.parseInt(registrationBox.getSelectedItem().toString().split(" - ")[0]);
                int score = Integer.parseInt(scoreField.getText().trim());
                String letter = letterField.getText().trim();

                try (Connection conn = DBConnection.connect()) {
                    String insert = "INSERT INTO grade (registration_id, score, letter) VALUES (?, ?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(insert);
                    stmt.setInt(1, registrationId);
                    stmt.setInt(2, score);
                    stmt.setString(3, letter);
                    stmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Grade added successfully.");
                    loadStudentGrades();
                }
            } catch (NumberFormatException ex) {
                showError("Score must be a number.");
            } catch (SQLException ex) {
                showError("Error saving grade: " + ex.getMessage());
            }
        }
    }

    private void openViewStudentsDialog() {
        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT course_id, title FROM course WHERE instructor_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, instructorId);
            ResultSet rs = stmt.executeQuery();

            JComboBox<String> courseBox = new JComboBox<>();
            while (rs.next()) {
                int courseId = rs.getInt("course_id");
                String title = rs.getString("title");
                courseBox.addItem(courseId + " - " + title);
            }

            if (courseBox.getItemCount() == 0) {
                JOptionPane.showMessageDialog(this, "No courses found for this instructor.");
                return;
            }

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Select Course:"));
            panel.add(courseBox);

            int option = JOptionPane.showConfirmDialog(this, panel, "View Students", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String selectedCourse = courseBox.getSelectedItem().toString();
                int courseId = Integer.parseInt(selectedCourse.split(" - ")[0]);
                loadStudentsInCourse(courseId);
            }
        } catch (SQLException e) {
            showError("Failed to load courses: " + e.getMessage());
        }
    }


    private void loadStudentsInCourse(int courseId) {
        JDialog studentDialog = new JDialog(this, "Students in Course", true);
        studentDialog.setSize(400, 300);
        DefaultTableModel studentTableModel = new DefaultTableModel(new String[]{"User ID", "Name"}, 0);
        JTable studentsTable = new JTable(studentTableModel);
        studentDialog.add(new JScrollPane(studentsTable), BorderLayout.CENTER);

        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT u.user_id, u.name FROM registration r " +
                         "JOIN user u ON r.student_id = u.user_id " +
                         "WHERE r.course_id = ? LIMIT 30";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                studentTableModel.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("name")
                });
            }
        } catch (SQLException e) {
            showError("Error loading students: " + e.getMessage());
        }

        studentDialog.setVisible(true);
    }

    private void openManageAssignmentsDialog() {
        JDialog assignmentDialog = new JDialog(this, "Manage Assignments", true);
        assignmentDialog.setSize(400, 300);
        DefaultTableModel assignmentModel = new DefaultTableModel(new String[]{"Course", "Assignment", "Due Date"}, 0);
        JTable assignmentTable = new JTable(assignmentModel);
        assignmentDialog.add(new JScrollPane(assignmentTable), BorderLayout.CENTER);

        // Load assignments initially
        loadAssignments(assignmentModel);

        // Button to add assignment
        JButton addButton = new JButton("Add Assignment");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAddAssignmentDialog(assignmentModel);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        assignmentDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        assignmentDialog.setVisible(true);
    }

    private void openAddAssignmentDialog(DefaultTableModel assignmentModel) {
        JDialog addAssignmentDialog = new JDialog(this, "Add Assignment", true);
        addAssignmentDialog.setSize(350, 300);

        // ComboBox for course selection
        JComboBox<String> courseComboBox = new JComboBox<>();
        loadCourses(courseComboBox); // Load available courses

        JTextField titleField = new JTextField();
        JTextArea descriptionArea = new JTextArea(5, 20);
        JTextField dueDateField = new JTextField();

        JPanel inputPanel = new JPanel(new GridLayout(5, 2));
        inputPanel.add(new JLabel("Course:"));
        inputPanel.add(courseComboBox);
        inputPanel.add(new JLabel("Title:"));
        inputPanel.add(titleField);
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(new JScrollPane(descriptionArea));
        inputPanel.add(new JLabel("Due Date (YYYY-MM-DD):"));
        inputPanel.add(dueDateField);

        JButton submitButton = new JButton("Submit");
submitButton.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        String selectedCourse = courseComboBox.getSelectedItem().toString();
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        String dueDate = dueDateField.getText().trim();

        if (!title.isEmpty() && !description.isEmpty() && !dueDate.isEmpty()) {
            try (Connection conn = DBConnection.connect()) {
                String insert = "INSERT INTO Assignment (course_id, title, description, due_date) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(insert);
                stmt.setInt(1, getCourseIdFromSelection(selectedCourse)); // Get course ID from selection
                stmt.setString(2, title);
                stmt.setString(3, description);
                stmt.setDate(4, Date.valueOf(dueDate));
                stmt.executeUpdate();

                // Use InstructorDashboard.this to refer to the outer class
                JOptionPane.showMessageDialog(InstructorDashboard.this, "Assignment added successfully.");
                assignmentModel.setRowCount(0); // Clear the current assignments
                loadAssignments(assignmentModel); // Reload assignments
                addAssignmentDialog.dispose(); // Close the add assignment dialog
            } catch (SQLException ex) {
                showError("Error adding assignment: " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                showError("Invalid date format.");
            }
        } else {
            showError("All fields must be filled.");
        }
    }
});
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitButton);

        addAssignmentDialog.add(inputPanel, BorderLayout.CENTER);
        addAssignmentDialog.add(buttonPanel, BorderLayout.SOUTH);

        addAssignmentDialog.setVisible(true);
    }

    private void loadCourses(JComboBox<String> courseComboBox) {
        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT course_id, title FROM course WHERE instructor_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, instructorId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int courseId = rs.getInt("course_id");
                String title = rs.getString("title");
                courseComboBox.addItem(courseId + " - " + title); // Format: "ID - Title"
            }
        } catch (SQLException e) {
            showError("Error loading courses: " + e.getMessage());
        }
    }

    private int getCourseIdFromSelection(String selectedCourse) {
        return Integer.parseInt(selectedCourse.split(" - ")[0]); // Extract course ID from selection
    }

    private void loadAssignments(DefaultTableModel assignmentModel) {
        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT c.title AS course_title, a.title AS assignment_title, a.due_date " +
                         "FROM Assignment a " +
                         "JOIN Course c ON a.course_id = c.course_id " +
                         "WHERE c.instructor_id = ?"; // Ensure we're only loading assignments for the instructor's courses
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, instructorId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                assignmentModel.addRow(new Object[]{
                    rs.getString("course_title"),
                    rs.getString("assignment_title"),
                    rs.getDate("due_date")
                });
            }
        } catch (SQLException e) {
            showError("Error loading assignments: " + e.getMessage());
        }
    }

    private void openViewSubmissionsDialog() {
        JDialog submissionDialog = new JDialog(this, "Submissions", true);
        submissionDialog.setSize(400, 300);
        DefaultTableModel submissionModel = new DefaultTableModel(new String[]{"Assignment", "Student", "Submission Date", "Status"}, 0);
        JTable submissionTable = new JTable(submissionModel);
        submissionDialog.add(new JScrollPane(submissionTable), BorderLayout.CENTER);

        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT a.title AS assignment_title, u.name AS student_name, s.submission_date, " +
                         "CASE WHEN s.submission_date IS NOT NULL THEN 'Submitted' ELSE 'Not Submitted' END AS status " +
                         "FROM Submission s " +
                         "JOIN Assignment a ON s.assignment_id = a.assignment_id " +
                         "JOIN Registration r ON a.course_id = r.course_id " +
                         "JOIN User u ON r.student_id = u.user_id " +
                         "WHERE a.course_id IN (SELECT course_id FROM Course WHERE instructor_id = ?)"; // Filter for this instructor's courses
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, instructorId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                submissionModel.addRow(new Object[]{
                    rs.getString("assignment_title"),
                    rs.getString("student_name"),
                    rs.getDate("submission_date"),
                    rs.getString("status")
                });
            }
        } catch (SQLException e) {
            showError("Error loading submissions: " + e.getMessage());
        }

        submissionDialog.setVisible(true);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}