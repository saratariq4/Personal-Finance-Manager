package oop2_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class LoginScreen extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginScreen() {
        setTitle("User Login");
        setSize(500, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(245, 245, 245));

        JLabel title = new JLabel("Login to Your Account", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        mainPanel.add(title);
        mainPanel.add(createFieldPanel("Email Address:", emailField = new JTextField(20)));
        mainPanel.add(createFieldPanel("Password:", passwordField = new JPasswordField(20)));

        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        // Replace lambda with an anonymous inner class
        loginButton.addActionListener(new ActionListener() {
            @Override
             public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
        buttonPanel.add(loginButton);

        JButton backBtn = new JButton("Home Page");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        // Replace lambda with an anonymous inner class
        backBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new WelcomeScreen();
                dispose(); // Close the login screen
            }
        });
        buttonPanel.add(backBtn);

        // Add button panel to the main panel
        mainPanel.add(buttonPanel);

        add(mainPanel);
        setVisible(true);
    }

    private JPanel createFieldPanel(String labelText, JTextField field) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBackground(new Color(245, 245, 245));
        JLabel label = new JLabel(labelText);
        panel.add(label);
        panel.add(field);
        return panel;
    }

   private void performLogin() {
    String email = emailField.getText().trim();
    String password = String.valueOf(passwordField.getPassword()).trim();

    if (email.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Please fill in all fields.");
        return;
    }

    try (Connection conn = DBConnection.connect()) {
        String sql = "SELECT user_id, name, email, password, role FROM User WHERE email = ? AND password = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, email);
        stmt.setString(2, password);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            int userId = rs.getInt("user_id");
            String name = rs.getString("name");
            String role = rs.getString("role");

            User user;
            switch (role.toLowerCase()) {
                case "admin":
                    user = new Admin(userId, name, email, password);
                    break;
                case "instructor":
                    user = new Instructor(userId, name, email, password);
                    break;
                case "student":
                    user = new Student(userId, name, email, password);
                    break;
                default:
                    JOptionPane.showMessageDialog(this, "Unknown role.");
                    return;
            }

            user.showDashboard();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid email or password.");
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
    }
}
}