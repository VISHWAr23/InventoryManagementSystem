package com.inventory;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class InventoryManagementSystem extends JFrame {
    private JTabbedPane tabbedPane;
    private ProductPanel productPanel;
    private CategoryPanel categoryPanel;
    private PurchasePanel purchasePanel;
    private ReportPanel reportPanel;

    public InventoryManagementSystem() {
        setTitle("Inventory Management System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane();
        productPanel = new ProductPanel();
        categoryPanel = new CategoryPanel();
        purchasePanel = new PurchasePanel();
        reportPanel = new ReportPanel();

        tabbedPane.addTab("Products", productPanel);
        tabbedPane.addTab("Categories", categoryPanel);
        tabbedPane.addTab("Purchase", purchasePanel);
        tabbedPane.addTab("Reports", reportPanel);

        add(tabbedPane);
    }
}