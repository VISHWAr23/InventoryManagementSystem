package com.inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ProductPanel extends JPanel {
    private JTextField nameField, priceField, quantityField;
    private JComboBox<String> categoryComboBox;
    private JButton addButton, updateButton, deleteButton;
    private JTable productTable;
    private DefaultTableModel tableModel;

    public ProductPanel() {
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        nameField = new JTextField();
        priceField = new JTextField();
        quantityField = new JTextField();
        categoryComboBox = new JComboBox<>();

        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Price:"));
        inputPanel.add(priceField);
        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(quantityField);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(categoryComboBox);

        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Price", "Quantity", "Category"}, 0);
        productTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(productTable);

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        loadCategories();
        loadProducts();
        setupListeners();
    }

    private void loadCategories() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM categories")) {

            categoryComboBox.removeAllItems();
            while (rs.next()) {
                categoryComboBox.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading categories: " + e.getMessage());
        }
    }

    private void loadProducts() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT p.id, p.name, p.price, p.quantity, c.name as category FROM products p JOIN categories c ON p.category_id = c.id")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("quantity"),
                    rs.getString("category")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage());
        }
    }

    private void setupListeners() {
        addButton.addActionListener(e -> addProduct());
        updateButton.addActionListener(e -> updateProduct());
        deleteButton.addActionListener(e -> deleteProduct());

        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = productTable.getSelectedRow();
                if (selectedRow != -1) {
                    nameField.setText((String) tableModel.getValueAt(selectedRow, 1));
                    priceField.setText(String.valueOf(tableModel.getValueAt(selectedRow, 2)));
                    quantityField.setText(String.valueOf(tableModel.getValueAt(selectedRow, 3)));
                    categoryComboBox.setSelectedItem(tableModel.getValueAt(selectedRow, 4));
                }
            }
        });
    }

    private void addProduct() {
        String name = nameField.getText();
        String price = priceField.getText();
        String quantity = quantityField.getText();
        String category = (String) categoryComboBox.getSelectedItem();

        if (name.isEmpty() || price.isEmpty() || quantity.isEmpty() || category == null) {
            JOptionPane.showMessageDialog(this, "Please fill all fields");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO products (name, price, quantity, category_id) VALUES (?, ?, ?, (SELECT id FROM categories WHERE name = ?))",
                 Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);
            pstmt.setDouble(2, Double.parseDouble(price));
            pstmt.setInt(3, Integer.parseInt(quantity));
            pstmt.setString(4, category);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        tableModel.addRow(new Object[]{
                            generatedKeys.getInt(1),
                            name,
                            Double.parseDouble(price),
                            Integer.parseInt(quantity),
                            category
                        });
                    }
                }
                clearFields();
                JOptionPane.showMessageDialog(this, "Product added successfully");
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding product: " + e.getMessage());
        }
    }

    private void updateProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to update");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String name = nameField.getText();
        String price = priceField.getText();
        String quantity = quantityField.getText();
        String category = (String) categoryComboBox.getSelectedItem();

        if (name.isEmpty() || price.isEmpty() || quantity.isEmpty() || category == null) {
            JOptionPane.showMessageDialog(this, "Please fill all fields");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE products SET name = ?, price = ?, quantity = ?, category_id = (SELECT id FROM categories WHERE name = ?) WHERE id = ?")) {

            pstmt.setString(1, name);
            pstmt.setDouble(2, Double.parseDouble(price));
            pstmt.setInt(3, Integer.parseInt(quantity));
            pstmt.setString(4, category);
            pstmt.setInt(5, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                tableModel.setValueAt(name, selectedRow, 1);
                tableModel.setValueAt(Double.parseDouble(price), selectedRow, 2);
                tableModel.setValueAt(Integer.parseInt(quantity), selectedRow, 3);
                tableModel.setValueAt(category, selectedRow, 4);
                clearFields();
                JOptionPane.showMessageDialog(this, "Product updated successfully");
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating product: " + e.getMessage());
        }
    }

    private void deleteProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this product?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {

                pstmt.setInt(1, id);

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    tableModel.removeRow(selectedRow);
                    clearFields();
                    JOptionPane.showMessageDialog(this, "Product deleted successfully");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error deleting product: " + e.getMessage());
            }
        }
    }

    private void clearFields() {
        nameField.setText("");
        priceField.setText("");
        quantityField.setText("");
        categoryComboBox.setSelectedIndex(0);
    }
}