import java.sql.*;
import java.util.Scanner;

public class CafeManagement {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/CafeManagement";
    private static final String USER = "root"; // Replace with your MySQL username
    private static final String PASSWORD = "Valdepielagos13*"; // Replace with your MySQL password

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            System.out.println("Welcome to Cute Cafe in Madrid!");

            do {
                System.out.println("\nMain Menu:");
                System.out.println("1. Add Menu Item");
                System.out.println("2. View Menu");
                System.out.println("3. Place Order");
                System.out.println("4. Update Order Status");
                System.out.println("5. View All Orders");
                System.out.println("6. View Inventory");
                System.out.println("7. Redeem Loyalty Points");
                System.out.println("8. Exit");
                System.out.print("Enter your choice: ");
                choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        addMenuItem(conn, scanner);
                        break;
                    case 2:
                        viewMenu(conn);
                        break;
                    case 3:
                        placeOrder(conn, scanner);
                        break;
                    case 4:
                        updateOrderStatus(conn, scanner);
                        break;
                    case 5:
                        viewOrders(conn);
                        break;
                    case 6:
                        viewInventory(conn);
                        break;
                    case 7:
                        redeemLoyaltyPoints(conn, scanner);
                        break;
                    case 8:
                        System.out.println("Thank you for using Cafe Management System!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } while (choice != 8);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addMenuItem(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter item name: ");
        String name = scanner.nextLine();
        System.out.print("Enter item price: ");
        double price = scanner.nextDouble();
        scanner.nextLine(); // Consume newline
        System.out.print("Enter item description: ");
        String description = scanner.nextLine();

        String query = "INSERT INTO MenuItems (name, price, description) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setDouble(2, price);
            pstmt.setString(3, description);
            pstmt.executeUpdate();
            System.out.println("Menu item added successfully!");
        }
    }

    private static void viewMenu(Connection conn) throws SQLException {
        String query = "SELECT * FROM MenuItems";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("\nMenu:");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + ". " +
                        rs.getString("name") + " - €" +
                        rs.getDouble("price") + " (" +
                        rs.getString("description") + ")");
            }
        }
    }

    private static void placeOrder(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter customer ID (or 0 for new customer): ");
        int customerId = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (customerId == 0) {
            System.out.print("Enter new customer name: ");
            String customerName = scanner.nextLine();
            customerId = addCustomer(conn, customerName);
        }

        String insertOrder = "INSERT INTO Orders (customer_id, status) VALUES (?, 'Pending')";
        String insertOrderDetails = "INSERT INTO OrderDetails (order_id, menu_item_id, quantity, customization) VALUES (?, ?, ?, ?)";
        try (PreparedStatement orderStmt = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement orderDetailsStmt = conn.prepareStatement(insertOrderDetails)) {

            orderStmt.setInt(1, customerId);
            orderStmt.executeUpdate();
            ResultSet generatedKeys = orderStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int orderId = generatedKeys.getInt(1);
                System.out.print("Enter the number of items in the order: ");
                int itemCount = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                for (int i = 0; i < itemCount; i++) {
                    System.out.print("Enter menu item ID: ");
                    int itemId = scanner.nextInt();
                    System.out.print("Enter quantity: ");
                    int quantity = scanner.nextInt();
                    scanner.nextLine(); // Consume newline
                    System.out.print("Enter customization (e.g., 'extra milk, no sugar'): ");
                    String customization = scanner.nextLine();

                    orderDetailsStmt.setInt(1, orderId);
                    orderDetailsStmt.setInt(2, itemId);
                    orderDetailsStmt.setInt(3, quantity);
                    orderDetailsStmt.setString(4, customization);
                    orderDetailsStmt.executeUpdate();
                }

                System.out.println("Order placed successfully!");
            }
        }
    }

    private static int addCustomer(Connection conn, String name) throws SQLException {
        String query = "INSERT INTO Customers (name, loyalty_points) VALUES (?, 0)";
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        }
        return -1; // Indicates failure
    }

    private static void updateOrderStatus(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter order ID: ");
        int orderId = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        System.out.print("Enter new status (Pending, Prepared, Served): ");
        String status = scanner.nextLine();

        String query = "UPDATE Orders SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
            System.out.println("Order status updated successfully!");
        }
    }

    private static void viewInventory(Connection conn) throws SQLException {
        String query = "SELECT * FROM Inventory";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("\nInventory:");
            while (rs.next()) {
                System.out.println(rs.getString("ingredient_name") + " - " +
                        rs.getInt("quantity_in_stock") + " units");
                if (rs.getInt("quantity_in_stock") < rs.getInt("low_stock_threshold")) {
                    System.out.println("⚠️ Low stock alert for " + rs.getString("ingredient_name"));
                }
            }
        }
    }

    private static void redeemLoyaltyPoints(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter customer ID: ");
        int customerId = scanner.nextInt();

        String query = "SELECT points FROM LoyaltyPoints WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int points = rs.getInt("points");
                    System.out.println("You have " + points + " points. 10 points = €1 discount.");
                    System.out.print("Enter points to redeem: ");
                    int redeemPoints = scanner.nextInt();

                    if (redeemPoints <= points) {
                        String updateQuery = "UPDATE LoyaltyPoints SET points = points - ? WHERE id = ?";
                        try (PreparedStatement updatePstmt = conn.prepareStatement(updateQuery)) {
                            updatePstmt.setInt(1, redeemPoints);
                            updatePstmt.setInt(2, customerId);
                            updatePstmt.executeUpdate();
                            System.out.println("Points redeemed! €" + (redeemPoints / 10.0) + " discount applied.");
                        }
                    } else {
                        System.out.println("Insufficient points.");
                    }
                } else {
                    System.out.println("Customer not found.");
                }
            }
        }
    }
    private static void viewOrders(Connection conn) throws SQLException {
        String query = "SELECT o.id AS order_id, o.order_date, m.name, d.quantity " +
                "FROM Orders o " +
                "JOIN OrderDetails d ON o.id = d.order_id " +
                "JOIN MenuItems m ON d.menu_item_id = m.id " +
                "ORDER BY o.id";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("\nAll Orders:");
            while (rs.next()) {
                System.out.println("Order ID: " + rs.getInt("order_id") +
                        ", Date: " + rs.getTimestamp("order_date") +
                        ", Item: " + rs.getString("name") +
                        ", Quantity: " + rs.getInt("quantity"));
            }
        }
    }
}
