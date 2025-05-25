package FoodManagement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

public class MenuFrame extends JFrame {
    String username;
    DefaultListModel<String> menuListModel = new DefaultListModel<>();
    JList<String> menuList;
    Map<String, Integer> cart = new HashMap<>(); // food name -> quantity

    public MenuFrame(String username) {
        this.username = username;
        setTitle("Menu - Welcome " + username);
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        menuList = new JList<>(menuListModel);
        JScrollPane scrollPane = new JScrollPane(menuList);

        JButton addToCartBtn = new JButton("Add to Cart");
        JButton viewCartBtn = new JButton("View Cart");
        JButton logoutBtn = new JButton("Logout");

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(addToCartBtn);
        buttonsPanel.add(viewCartBtn);
        buttonsPanel.add(logoutBtn);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        loadMenuFromDB();

        addToCartBtn.addActionListener(e -> addItemToCart());
        viewCartBtn.addActionListener(e -> openCart());
        logoutBtn.addActionListener(e -> logout());
    }

    void loadMenuFromDB() {
        menuListModel.clear();
        try (Connection con = Database.getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT name, price, stock FROM food_items WHERE stock > 0");
            while (rs.next()) {
                String item = rs.getString("name") + " - $" + rs.getDouble("price") + " (Stock: " + rs.getInt("stock") + ")";
                menuListModel.addElement(item);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load menu.");
        }
    }

    void addItemToCart() {
        String selected = menuList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select an item to add.");
            return;
        }
        String foodName = selected.split(" - ")[0];
        cart.put(foodName, cart.getOrDefault(foodName, 0) + 1);
        JOptionPane.showMessageDialog(this, foodName + " added to cart.");
    }

    void openCart() {
        new CartFrame(username, cart, this).setVisible(true);
        this.setVisible(false);
    }

    void logout() {
        dispose();
        new LoginFrame().setVisible(true);
    }
}

