package FoodManagement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

public class CartFrame extends JFrame {
    String username;
    Map<String, Integer> cart;
    MenuFrame menuFrame;
    JTextArea cartArea;
    JLabel totalLabel;

    public CartFrame(String username, Map<String, Integer> cart, MenuFrame menuFrame) {
        this.username = username;
        this.cart = cart;
        this.menuFrame = menuFrame;

        setTitle("Your Cart");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cartArea = new JTextArea();
        cartArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(cartArea);

        totalLabel = new JLabel();

        JButton payBtn = new JButton("Proceed to Payment");
        JButton backBtn = new JButton("Back to Menu");

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(totalLabel);
        bottomPanel.add(payBtn);
        bottomPanel.add(backBtn);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        payBtn.addActionListener(e -> openPayment());
        backBtn.addActionListener(e -> backToMenu());

        updateCartDisplay();
    }

    void updateCartDisplay() {
        StringBuilder sb = new StringBuilder();
        double total = 0;

        try (Connection con = Database.getConnection()) {
            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                String foodName = entry.getKey();
                int qty = entry.getValue();

                PreparedStatement pst = con.prepareStatement("SELECT price FROM food_items WHERE name = ?");
                pst.setString(1, foodName);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    double price = rs.getDouble("price");
                    sb.append(foodName).append(" x ").append(qty).append(" = $").append(price * qty).append("\n");
                    total += price * qty;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        double discount = OfferDiscount.calculateDiscount(cart);
        sb.append("\nDiscount: -$").append(String.format("%.2f", discount));
        sb.append("\n----------------------\nTotal: $").append(String.format("%.2f", total - discount));

        totalLabel.setText("Total: $" + String.format("%.2f", total - discount));
        cartArea.setText(sb.toString());
    }

    void openPayment() {
        new PaymentFrame(username, cart, this).setVisible(true);
        this.setVisible(false);
    }

    void backToMenu() {
        this.dispose();
        menuFrame.setVisible(true);
    }
}
