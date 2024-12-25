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
                System.out.println("4. View All Orders");
                System.out.println("5. Exit");
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
                        viewOrders(conn);
                        break;
                    case 5:
                        System.out.println("Thank you for using Cafe Management System!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } while (choice != 5);

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
        System.out.print("Enter the number of items in the order: ");
        int itemCount = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        String insertOrder = "INSERT INTO Orders () VALUES ()";
        String insertOrderDetails = "INSERT INTO OrderDetails (order_id, menu_item_id, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement orderStmt = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement orderDetailsStmt = conn.prepareStatement(insertOrderDetails)) {

            orderStmt.executeUpdate();
            ResultSet generatedKeys = orderStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int orderId = generatedKeys.getInt(1);

                for (int i = 0; i < itemCount; i++) {
                    System.out.print("Enter menu item ID: ");
                    int itemId = scanner.nextInt();
                    System.out.print("Enter quantity: ");
                    int quantity = scanner.nextInt();
                    scanner.nextLine(); // Consume newline

                    orderDetailsStmt.setInt(1, orderId);
                    orderDetailsStmt.setInt(2, itemId);
                    orderDetailsStmt.setInt(3, quantity);
                    orderDetailsStmt.executeUpdate();
                }

                System.out.println("Order placed successfully!");
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
