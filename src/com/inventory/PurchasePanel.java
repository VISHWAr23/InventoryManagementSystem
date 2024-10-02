package com.inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PurchasePanel extends JPanel {
    private JComboBox<String> productComboBox;
    private JTextField quantityField;
    private JButton purchaseButton;
    private JTable purchaseHistoryTable;
    private DefaultTableModel tableModel;

    public PurchasePanel() {
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        productComboBox = new JComboBox<>();
        quantityField = new JTextField();

        inputPanel.add(new JLabel("Product:"));
        inputPanel.add(productComboBox);
        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(quantityField);

        purchaseButton = new JButton("Purchase");

        tableModel = new DefaultTableModel(new Object[]{"ID", "Product", "Quantity", "Total Price", "Date"}, 0);
        purchaseHistoryTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(purchaseHistoryTable);

        add(inputPanel, BorderLayout.NORTH);
        add(purchaseButton, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        loadProducts();
        loadPurchaseHistory();
        setupListeners();
    }

    private void loadProducts() {
        productComboBox.removeAllItems();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM products WHERE quantity > 0")) {

            while (rs.next()) {
                productComboBox.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage());
        }
    }

    private void loadPurchaseHistory() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT ph.id, p.name, ph.quantity, ph.total_price, ph.purchase_date " +
                 "FROM purchase_history ph " +
                 "JOIN products p ON ph.product_id = p.id " +
                 "ORDER BY ph.purchase_date DESC")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getDouble("total_price"),
                    rs.getTimestamp("purchase_date")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading purchase history: " + e.getMessage());
        }
    }

    private void setupListeners() {
        purchaseButton.addActionListener(e -> makePurchase());
    }

    private void makePurchase() {
        String productName = (String) productComboBox.getSelectedItem();
        String quantityStr = quantityField.getText();

        if (productName == null || quantityStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a product and enter a quantity");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive integer for quantity");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT id, price, quantity FROM products WHERE name = ?")) {

            pstmt.setString(1, productName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int productId = rs.getInt("id");
                double price = rs.getDouble("price");
                int availableQuantity = rs.getInt("quantity");

                if (quantity > availableQuantity) {
                    JOptionPane.showMessageDialog(this, "Not enough stock. Available: " + availableQuantity);
                    return;
                }

                double totalPrice = price * quantity;

                // Update product quantity
                try (PreparedStatement updatePstmt = conn.prepareStatement(
                    "UPDATE products SET quantity = quantity - ? WHERE id = ?")) {
                    updatePstmt.setInt(1, quantity);
                    updatePstmt.setInt(2, productId);
                    updatePstmt.executeUpdate();
                }

                // Record purchase
                try (PreparedStatement insertPstmt = conn.prepareStatement(
                    "INSERT INTO purchase_history (product_id, quantity, total_price, purchase_date) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                    insertPstmt.setInt(1, productId);
                    insertPstmt.setInt(2, quantity);
                    insertPstmt.setDouble(3, totalPrice);
                    insertPstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

                    int affectedRows = insertPstmt.executeUpdate();

                    if (affectedRows > 0) {
                        try (ResultSet generatedKeys = insertPstmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int purchaseId = generatedKeys.getInt(1);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                tableModel.insertRow(0, new Object[]{
                                    purchaseId,
                                    productName,
                                    quantity,
                                    totalPrice,
                                    sdf.format(new Date())
                                });
                            }
                        }
                    }
                }

                JOptionPane.showMessageDialog(this, "Purchase successful. Total price: $" + totalPrice);
                quantityField.setText("");
                loadProducts(); // Refresh product list
            } else {
                JOptionPane.showMessageDialog(this, "Product not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error making purchase: " + e.getMessage());
        }
    }
}