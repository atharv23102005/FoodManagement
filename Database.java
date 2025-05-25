package FoodManagement;

import java.sql.*;

public class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/food_management";
    private static final String USER = "root";      // change if needed
    private static final String PASSWORD = "atharva@2005%";  // your MySQL password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
