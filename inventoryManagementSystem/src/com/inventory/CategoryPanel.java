package com.inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CategoryPanel extends JPanel {
    private JTextField nameField;
    private JButton addButton, updateButton, deleteButton;
    private JTable categoryTable;
    private DefaultTableModel tableModel;

    public CategoryPanel() {
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(1, 2));
        nameField = new JTextField();

        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);

        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Name"}, 0);
        categoryTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(categoryTable);

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        loadCategories();
        setupListeners();
    }

    private void loadCategories() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM categories ORDER BY name")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{rs.getInt("id"), rs.getString("name")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading categories: " + e.getMessage());
        }
    }

    private void setupListeners() {
        addButton.addActionListener(e -> addCategory());
        updateButton.addActionListener(e -> updateCategory());
        deleteButton.addActionListener(e -> deleteCategory());

        categoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = categoryTable.getSelectedRow();
                if (selectedRow != -1) {
                    nameField.setText((String) tableModel.getValueAt(selectedRow, 1));
                }
            }
        });
    }

    private void addCategory() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a category name");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO categories (name) VALUES (?)",
                 Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        tableModel.addRow(new Object[]{id, name});
                        nameField.setText("");
                        JOptionPane.showMessageDialog(this, "Category added successfully");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding category: " + e.getMessage());
        }
    }

    private void updateCategory() {
        int selectedRow = categoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a category to update");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a category name");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE categories SET name = ? WHERE id = ?")) {

            pstmt.setString(1, name);
            pstmt.setInt(2, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                tableModel.setValueAt(name, selectedRow, 1);
                nameField.setText("");
                JOptionPane.showMessageDialog(this, "Category updated successfully");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating category: " + e.getMessage());
        }
    }

    private void deleteCategory() {
        int selectedRow = categoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a category to delete");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String name = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete the category '" + name + "'?\n" +
            "This will also delete all products in this category.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // First, delete all products in this category
                try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM products WHERE category_id = ?")) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }

                // Then, delete the category
                try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM categories WHERE id = ?")) {
                    pstmt.setInt(1, id);
                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) {
                        tableModel.removeRow(selectedRow);
                        nameField.setText("");
                        JOptionPane.showMessageDialog(this, "Category and associated products deleted successfully");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error deleting category: " + e.getMessage());
            }
        }
    }
}