import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Locale;
import java.util.ResourceBundle;

public class CafeManagementGUI {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/CafeManagement";
    private static final String USER = "root"; // Replace with your MySQL username
    private static final String PASSWORD = "Valdepielagos13*"; // Replace with your MySQL password

    private JFrame frame;
    private DefaultTableModel menuTableModel;
    private DefaultTableModel customerTableModel;
    private JTable customerTable;
    private ResourceBundle bundle;
    private JTable menuTable;

    public CafeManagementGUI() {
        setLocale(Locale.ENGLISH); // Default to English
        initialize();
    }

    private void setLocale(Locale locale) {
        Locale.setDefault(locale);
        bundle = ResourceBundle.getBundle("MessagesBundle_en_EN", locale);
    }

    private void initialize() {
        frame = new JFrame(bundle.getString("title"));
        frame.setBounds(100, 100, 800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel();
        JLabel titleLabel = new JLabel(bundle.getString("welcome_message"));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(titleLabel);
        frame.getContentPane().add(headerPanel, BorderLayout.NORTH);

        // **Tabbed Pane for Menu & Customers**
        JTabbedPane tabbedPane = new JTabbedPane();
        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Menu Table
        JPanel menuPanel = new JPanel(new BorderLayout());
        String[] columnNames = {bundle.getString("id"), bundle.getString("name"), bundle.getString("price"), bundle.getString("description")};
        menuTableModel = new DefaultTableModel(columnNames, 0);
        menuTable = new JTable(menuTableModel);
        JScrollPane scrollPane = new JScrollPane(menuTable);
        menuPanel.add(new JScrollPane(menuTable), BorderLayout.CENTER);
        //frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("Menu", menuPanel);

        // **Customers Panel**
        JPanel customerPanel = new JPanel(new BorderLayout());
        String[] customerColumnNames = {"Customer Name", "Loyalty Points"};
        customerTableModel = new DefaultTableModel(customerColumnNames, 0);
        customerTable = new JTable(customerTableModel);
        customerPanel.add(new JScrollPane(customerTable), BorderLayout.CENTER);
        tabbedPane.addTab("Customers", customerPanel);

        // **ADD CLICK LISTENER TO TABLE**
        menuTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = menuTable.getSelectedRow();
                if (row != -1) {
                    int menuItemId = (int) menuTableModel.getValueAt(row, 0);
                    showMenuIngredients(menuItemId);
                }
            }
        });

        // Footer with actions
        JPanel footerPanel = new JPanel();
        JButton loadCustomersButton = new JButton("Load Customers & Points");
        JButton addButton = new JButton(bundle.getString("add_item"));
        JButton searchButton = new JButton(bundle.getString("search"));
        JButton switchLangButton = new JButton(bundle.getString("switch_language"));
        JButton resetButton = new JButton(bundle.getString("reset"));
        footerPanel.add(loadCustomersButton);
        footerPanel.add(addButton);
        footerPanel.add(searchButton);
        footerPanel.add(switchLangButton);
        footerPanel.add(resetButton);
        frame.getContentPane().add(footerPanel, BorderLayout.SOUTH);

        loadCustomersButton.addActionListener(e -> loadCustomerLoyaltyPoints());

        // Add button action
        addButton.addActionListener(e -> addMenuItem());

        // Search button action
        searchButton.addActionListener(e -> searchMenuItems());

        // Reset button action
        resetButton.addActionListener(e -> loadMenuItems());

        // Switch Language Button
        switchLangButton.addActionListener(e -> switchLanguage());

        frame.setVisible(true);
        loadMenuItems();
    }

    private void loadMenuItems() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            String query = "SELECT * FROM MenuItems";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                menuTableModel.setRowCount(0); // Clear table
                while (rs.next()) {
                    menuTableModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getString("description")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, bundle.getString("error_loading_menu"), bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    // **NEW FUNCTION TO SHOW INGREDIENTS**
    private void showMenuIngredients(int menuItemId) {
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html"); // Enable HTML formatting
        textPane.setEditable(false);

        StringBuilder ingredientsList = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            String query = "SELECT inv.id, inv.ingredient_name, inv.quantity_in_stock, inv.low_stock_threshold " +
                    "FROM MenuIngredients mi " +
                    "JOIN Inventory inv ON mi.ingredient_id = inv.id " +
                    "WHERE mi.menu_item_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, menuItemId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String ingredientName = rs.getString("ingredient_name");
                        int quantityInStock = rs.getInt("quantity_in_stock");
                        int lowStockThreshold = rs.getInt("low_stock_threshold");

                        // If quantity is below or equal to threshold, make it red
                        String color = (quantityInStock <= lowStockThreshold) ? "red" : "black";

                        ingredientsList.append("<p>")
                                .append("<b>").append(ingredientName).append("</b>")
                                .append(" - <span style='color:").append(color).append(";'>")
                                .append(quantityInStock)
                                .append("</span></p>");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, bundle.getString("error_loading_ingredients"), bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (ingredientsList.length() == 0) {
            ingredientsList.append("<p>").append(bundle.getString("no_ingredients_found")).append("</p>");
        }

        ingredientsList.append("</body></html>");
        textPane.setText(ingredientsList.toString());

        // Wrap in a scrollable dialog
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        JOptionPane.showMessageDialog(frame, scrollPane, bundle.getString("ingredients_list"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void addMenuItem() {
        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField descField = new JTextField();
        Object[] fields = {
                bundle.getString("name"), nameField,
                bundle.getString("price"), priceField,
                bundle.getString("description"), descField
        };

        int option = JOptionPane.showConfirmDialog(frame, fields, bundle.getString("add_item"), JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                String query = "INSERT INTO MenuItems (name, price, description) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, nameField.getText());
                    pstmt.setDouble(2, Double.parseDouble(priceField.getText()));
                    pstmt.setString(3, descField.getText());
                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(frame, bundle.getString("item_added_success"));
                    loadMenuItems();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, bundle.getString("error_adding_item"), bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void searchMenuItems() {
        String keyword = JOptionPane.showInputDialog(frame, bundle.getString("search_prompt"));
        if (keyword != null && !keyword.isEmpty()) {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                String query = "SELECT * FROM MenuItems WHERE name LIKE ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, "%" + keyword + "%");
                    try (ResultSet rs = pstmt.executeQuery()) {
                        menuTableModel.setRowCount(0); // Clear table
                        while (rs.next()) {
                            menuTableModel.addRow(new Object[]{
                                    rs.getInt("id"),
                                    rs.getString("name"),
                                    rs.getDouble("price"),
                                    rs.getString("description")
                            });
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, bundle.getString("error_searching"), bundle.getString("error"), JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(frame, bundle.getString("no_results"));
        }
    }

    private void switchLanguage() {
        Locale newLocale = Locale.getDefault().equals(Locale.ENGLISH) ? new Locale("es", "ES") : Locale.ENGLISH;
        setLocale(newLocale);
        JOptionPane.showMessageDialog(frame, bundle.getString("lang_switched"));
        frame.dispose();
        initialize(); // Restart GUI with new language
    }

    // **New Method to Load Customer Names and Loyalty Points**
    private void loadCustomerLoyaltyPoints() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            String query = "SELECT c.name, lp.points FROM Customers c " +
                    "LEFT JOIN LoyaltyPoints lp ON c.id = lp.customer_id";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                customerTableModel.setRowCount(0); // Clear table
                while (rs.next()) {
                    customerTableModel.addRow(new Object[]{
                            rs.getString("name"),
                            rs.getInt("points")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error loading customers and loyalty points", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CafeManagementGUI::new);
    }
}
