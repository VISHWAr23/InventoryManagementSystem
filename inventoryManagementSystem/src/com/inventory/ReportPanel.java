package com.inventory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportPanel extends JPanel {
    private JTextArea reportArea;
    private JButton generateReportButton;

    public ReportPanel() {
        setLayout(new BorderLayout());

        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(reportArea);

        generateReportButton = new JButton("Generate Report");

        add(generateReportButton, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        setupListeners();
    }

    private void setupListeners() {
        generateReportButton.addActionListener(e -> generateReport());
    }

    private void generateReport() {
        StringBuilder report = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        report.append("Inventory Management System Report\n");
        report.append("Generated on: ").append(sdf.format(new Date())).append("\n\n");

        try (Connection conn = DatabaseConnection.getConnection()) {
            report.append(generateInventoryReport(conn));
            report.append("\n");
            report.append(generateSalesReport(conn));
            report.append("\n");
            report.append(generateLowStockReport(conn));

            reportArea.setText(report.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating report: " + e.getMessage());
        }
    }

    private String generateInventoryReport(Connection conn) throws SQLException {
        StringBuilder inventoryReport = new StringBuilder();
        inventoryReport.append("Inventory Summary:\n");
        inventoryReport.append(String.format("%-30s %-10s %-10s %-15s\n", "Product", "Quantity", "Price", "Total Value"));
        inventoryReport.append("------------------------------------------------------------\n");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT p.name, p.quantity, p.price, (p.quantity * p.price) as total_value " +
                 "FROM products p ORDER BY total_value DESC")) {

            double totalInventoryValue = 0;

            while (rs.next()) {
                String name = rs.getString("name");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                double totalValue = rs.getDouble("total_value");

                inventoryReport.append(String.format("%-30s %-10d $%-9.2f $%-14.2f\n", name, quantity, price, totalValue));
                totalInventoryValue += totalValue;
            }

            inventoryReport.append("------------------------------------------------------------\n");
            inventoryReport.append(String.format("%-30s %-10s %-10s $%-14.2f\n", "Total Inventory Value", "", "", totalInventoryValue));
        }

        return inventoryReport.toString();
    }

    private String generateSalesReport(Connection conn) throws SQLException {
        StringBuilder salesReport = new StringBuilder();
        salesReport.append("Sales Summary (Last 30 days):\n");
        salesReport.append(String.format("%-30s %-10s %-15s\n", "Product", "Quantity", "Total Sales"));
        salesReport.append("------------------------------------------------------------\n");

        try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT p.name, SUM(ph.quantity) as total_quantity, SUM(ph.total_price) as total_sales " +
            "FROM purchase_history ph " +
            "JOIN products p ON ph.product_id = p.id " +
            "WHERE ph.purchase_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY p.id " +
            "ORDER BY total_sales DESC")) {

            ResultSet rs = pstmt.executeQuery();
            double totalSales = 0;

            while (rs.next()) {
                String name = rs.getString("name");
                int totalQuantity = rs.getInt("total_quantity");
                double totalSalesProduct = rs.getDouble("total_sales");

                salesReport.append(String.format("%-30s %-10d $%-14.2f\n", name, totalQuantity, totalSalesProduct));
                totalSales += totalSalesProduct;
            }

            salesReport.append("------------------------------------------------------------\n");
            salesReport.append(String.format("%-30s %-10s $%-14.2f\n", "Total Sales", "", totalSales));
        }

        return salesReport.toString();
    }

    private String generateLowStockReport(Connection conn) throws SQLException {
        StringBuilder lowStockReport = new StringBuilder();
        lowStockReport.append("Low Stock Alert (Quantity <= 5):\n");
        lowStockReport.append(String.format("%-30s %-10s\n", "Product", "Quantity"));
        lowStockReport.append("------------------------------------------------------------\n");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name, quantity FROM products WHERE quantity <= 5 ORDER BY quantity ASC")) {

            while (rs.next()) {
                String name = rs.getString("name");
                int quantity = rs.getInt("quantity");

                lowStockReport.append(String.format("%-30s %-10d\n", name, quantity));
            }
        }

        return lowStockReport.toString();
    }
}