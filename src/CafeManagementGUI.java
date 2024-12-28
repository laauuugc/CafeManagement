import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Locale;
import java.util.ResourceBundle;

public class CafeManagementGUI {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/CafeManagement";
    private static final String USER = "root"; // Replace with your MySQL username
    private static final String PASSWORD = "Valdepielagos13*"; // Replace with your MySQL password

    private JFrame frame;
    private DefaultTableModel menuTableModel;
    private ResourceBundle bundle;

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

        // Menu Table
        String[] columnNames = {bundle.getString("id"), bundle.getString("name"), bundle.getString("price"), bundle.getString("description")};
        menuTableModel = new DefaultTableModel(columnNames, 0);
        JTable menuTable = new JTable(menuTableModel);
        JScrollPane scrollPane = new JScrollPane(menuTable);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // Footer with actions
        JPanel footerPanel = new JPanel();
        JButton addButton = new JButton(bundle.getString("add_item"));
        JButton searchButton = new JButton(bundle.getString("search"));
        JButton switchLangButton = new JButton(bundle.getString("switch_language"));
        JButton resetButton = new JButton(bundle.getString("reset"));
        footerPanel.add(addButton);
        footerPanel.add(searchButton);
        footerPanel.add(switchLangButton);
        footerPanel.add(resetButton);
        frame.getContentPane().add(footerPanel, BorderLayout.SOUTH);

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CafeManagementGUI::new);
    }
}
