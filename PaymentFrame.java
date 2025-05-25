package FoodManagement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Map;

public class PaymentFrame extends JFrame {
    String username;
    Map<String, Integer> cart;
    CartFrame cartFrame;

    JComboBox<String> paymentMethodCombo;

    public PaymentFrame(String username, Map<String, Integer> cart, CartFrame cartFrame) {
        this.username = username;
        this.cart = cart;
        this.cartFrame = cartFrame;

        setTitle("Payment");
        setSize(350, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 2, 10, 10));

        add(new JLabel("Select Payment Method:"));
        paymentMethodCombo = new JComboBox<>(new String[]{"Credit Card", "Cash", "Mobile Payment"});
        add(paymentMethodCombo);

        JButton payBtn = new JButton("Pay Now");
        JButton cancelBtn = new JButton("Cancel");
        add(payBtn);
        add(cancelBtn);

        payBtn.addActionListener(e -> processPayment());
        cancelBtn.addActionListener(e -> cancelPayment());
    }

    void processPayment() {
        String paymentMethod = (String) paymentMethodCombo.getSelectedItem();
        if (paymentMethod == null) {
            JOptionPane.showMessageDialog(this, "Please select payment method.");
            return;
        }

        Connection con = null;
        try {
            con = Database.getConnection();
            con.setAutoCommit(false); // Start transaction

            // Get user id
            PreparedStatement pstUser = con.prepareStatement("SELECT id FROM users WHERE username = ?");
            pstUser.setString(1, username);
            ResultSet rsUser = pstUser.executeQuery();
            if (!rsUser.next()) {
                JOptionPane.showMessageDialog(this, "User not found.");
                return;
            }
            int userId = rsUser.getInt("id");

            // Calculate total & discount
            double total = 0;
            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                String foodName = entry.getKey();
                int qty = entry.getValue();

                PreparedStatement pstPrice = con.prepareStatement("SELECT price, stock, id FROM food_items WHERE name = ?");
                pstPrice.setString(1, foodName);
                ResultSet rsPrice = pstPrice.executeQuery();

                if (rsPrice.next()) {
                    double price = rsPrice.getDouble("price");
                    int stock = rsPrice.getInt("stock");
                    int foodId = rsPrice.getInt("id");

                    if (stock < qty) {
                        JOptionPane.showMessageDialog(this, foodName + " does not have enough stock.");
                        con.rollback();
                        return;
                    }

                    total += price * qty;
                } else {
                    JOptionPane.showMessageDialog(this, "Food item " + foodName + " not found.");
                    con.rollback();
                    return;
                }
            }

            double discount = OfferDiscount.calculateDiscount(cart);
            double finalTotal = total - discount;

            // Insert into orders table
            PreparedStatement pstOrder = con.prepareStatement(
                    "INSERT INTO orders(user_id, total, payment_method) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            pstOrder.setInt(1, userId);
            pstOrder.setDouble(2, finalTotal);
            pstOrder.setString(3, paymentMethod);
            pstOrder.executeUpdate();

            ResultSet generatedKeys = pstOrder.getGeneratedKeys();
            int orderId = -1;
            if (generatedKeys.next()) {
                orderId = generatedKeys.getInt(1);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create order.");
                con.rollback();
                return;
            }

            // Insert each order item and update stock
            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                String foodName = entry.getKey();
                int qty = entry.getValue();

                // Get food item id and current stock again
                PreparedStatement pstFood = con.prepareStatement("SELECT id, stock FROM food_items WHERE name = ?");
                pstFood.setString(1, foodName);
                ResultSet rsFood = pstFood.executeQuery();
                if (rsFood.next()) {
                    int foodId = rsFood.getInt("id");
                    int stock = rsFood.getInt("stock");

                    if (stock < qty) {
                        JOptionPane.showMessageDialog(this, foodName + " does not have enough stock.");
                        con.rollback();
                        return;
                    }

                    // Insert order item
                    PreparedStatement pstOrderItem = con.prepareStatement(
                            "INSERT INTO order_items(order_id, food_item_id, quantity) VALUES (?, ?, ?)");
                    pstOrderItem.setInt(1, orderId);
                    pstOrderItem.setInt(2, foodId);
                    pstOrderItem.setInt(3, qty);
                    pstOrderItem.executeUpdate();

                    // Update stock
                    PreparedStatement pstUpdateStock = con.prepareStatement(
                            "UPDATE food_items SET stock = stock - ? WHERE id = ?");
                    pstUpdateStock.setInt(1, qty);
                    pstUpdateStock.setInt(2, foodId);
                    pstUpdateStock.executeUpdate();
                } else {
                    JOptionPane.showMessageDialog(this, "Food item " + foodName + " not found during order processing.");
                    con.rollback();
                    return;
                }
            }

            con.commit();

            JOptionPane.showMessageDialog(this, "Payment successful! Order placed and sent to kitchen.");
            cart.clear();
            this.dispose();
            cartFrame.dispose();
            new MenuFrame(username).setVisible(true);

        } catch (SQLException ex) {
            try {
                if (con != null) con.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Payment failed due to database error.");
        } finally {
            try {
                if (con != null) con.setAutoCommit(true);
                if (con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    void cancelPayment() {
        this.dispose();
        cartFrame.setVisible(true);
    }
}
