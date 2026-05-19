package view;

import model.ProductRepository;
import model.ProductRepository.InventoryProductRow;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InventoryWindow extends JFrame {
    private static final Color COLOR_HEADER = Color.decode("#F0C808");
    private static final Color COLOR_BACKGROUND = Color.decode("#F8F9FA");
    private static final Color COLOR_PANEL = Color.decode("#FFFFFF");
    private static final Color COLOR_BORDER = Color.decode("#E0E0E0");
    private static final Color COLOR_TEXT = Color.decode("#1A1A1A");
    private static final Color COLOR_MUTED = Color.decode("#5E5E5E");
    private static final Color COLOR_HIGHLIGHT = Color.decode("#F0C808");
    private static final Color COLOR_SUCCESS = Color.decode("#3A7E00");
    private static final Color COLOR_DANGER = Color.decode("#D81E1E");

    private final ProductRepository productRepository;
    private final DefaultTableModel tableModel;
    private final JTable inventoryTable;
    private final JLabel statusLabel;
    private final JLabel timeLabel;

    public InventoryWindow(ProductRepository productRepository) {
        this.productRepository = productRepository;

        this.setTitle("Gestion de inventario");
        this.setSize(1280, 720);
        this.setMinimumSize(new Dimension(1100, 680));
        this.setResizable(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLayout(new BorderLayout(8, 8));
        this.getContentPane().setBackground(COLOR_BACKGROUND);

        this.add(createHeaderPanel(), BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[][] {}, new String[] { "ID", "PRODUCTO", "STOCK" }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 2) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        inventoryTable = new JTable(tableModel);
        inventoryTable.setRowHeight(22);
        inventoryTable.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        inventoryTable.setFillsViewportHeight(true);
        inventoryTable.setBackground(COLOR_PANEL);
        inventoryTable.setForeground(COLOR_TEXT);
        inventoryTable.setSelectionBackground(COLOR_HIGHLIGHT);
        inventoryTable.setSelectionForeground(COLOR_TEXT);
        inventoryTable.setGridColor(COLOR_BORDER);
        inventoryTable.getTableHeader().setBackground(COLOR_PANEL);
        inventoryTable.getTableHeader().setForeground(COLOR_TEXT);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBackground(COLOR_BACKGROUND);
        center.setBorder(new EmptyBorder(8, 8, 8, 8));
        center.add(createTablePanel(), BorderLayout.CENTER);
        center.add(createActionPanel(), BorderLayout.EAST);
        this.add(center, BorderLayout.CENTER);

        JPanel footer = createFooterPanel();
        statusLabel = (JLabel) footer.getClientProperty("statusLabel");
        timeLabel = (JLabel) footer.getClientProperty("timeLabel");
        this.add(footer, BorderLayout.SOUTH);

        reloadInventoryFromDatabase();
        startClock();

        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_HEADER);
        header.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("Monitor de inventario");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(COLOR_TEXT);
        header.add(title, BorderLayout.WEST);

        return header;
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(COLOR_PANEL);
        tablePanel.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 2));

        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        scrollPane.getViewport().setBackground(COLOR_PANEL);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createActionPanel() {
        JPanel actions = new JPanel(new GridLayout(4, 1, 8, 8));
        actions.setBackground(COLOR_BACKGROUND);
        actions.setBorder(new EmptyBorder(8, 8, 8, 8));

        JButton increaseButton = createActionButton("AUMENTAR");
        increaseButton.addActionListener(e -> adjustSelectedStock(1));
        increaseButton.setBackground(COLOR_SUCCESS);
        increaseButton.setForeground(COLOR_BACKGROUND);

        JButton decreaseButton = createActionButton("DISMINUIR");
        decreaseButton.addActionListener(e -> adjustSelectedStock(-1));
        decreaseButton.setBackground(COLOR_DANGER);
        decreaseButton.setForeground(COLOR_BACKGROUND);

        JButton refreshButton = createActionButton("ACTUALIZAR");
        refreshButton.addActionListener(e -> reloadInventoryFromDatabase());
        refreshButton.setBackground(COLOR_HIGHLIGHT);
        refreshButton.setForeground(COLOR_TEXT);

        JButton closeButton = createActionButton("CERRAR");
        closeButton.addActionListener(e -> dispose());
        closeButton.setBackground(COLOR_DANGER);
        closeButton.setForeground(COLOR_BACKGROUND);

        actions.add(increaseButton);
        actions.add(decreaseButton);
        actions.add(refreshButton);
        actions.add(closeButton);

        return actions;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(COLOR_HEADER);
        footer.setBorder(new EmptyBorder(6, 12, 6, 12));

        JLabel status = new JLabel("Items: 0");
        status.setForeground(COLOR_TEXT);
        status.setFont(new Font("SansSerif", Font.BOLD, 12));
        footer.add(status, BorderLayout.WEST);

        JLabel hint = new JLabel("Seleccione un producto y use AUMENTAR / DISMINUIR");
        hint.setForeground(COLOR_MUTED);
        footer.add(hint, BorderLayout.CENTER);

        JLabel time = new JLabel("--:--");
        time.setForeground(COLOR_MUTED);
        time.setFont(new Font("SansSerif", Font.BOLD, 12));
        time.setHorizontalAlignment(SwingConstants.RIGHT);
        footer.add(time, BorderLayout.EAST);

        footer.putClientProperty("statusLabel", status);
        footer.putClientProperty("timeLabel", time);
        return footer;
    }

    private JButton createActionButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(COLOR_PANEL);
        button.setForeground(COLOR_TEXT);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        return button;
    }

    private void reloadInventoryFromDatabase() {
        if (productRepository == null) {
            JOptionPane.showMessageDialog(this, "No hay repositorio de inventario disponible.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            List<InventoryProductRow> items = productRepository.getInventoryProducts();
            tableModel.setRowCount(0);
            for (InventoryProductRow item : items) {
                tableModel.addRow(new Object[] { item.getProductId(), item.getProductName(), item.getStock() });
            }
            updateStatus();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error cargando inventario",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void adjustSelectedStock(int delta) {
        int row = inventoryTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto para ajustar su stock.", "Sin seleccion",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object idValue = tableModel.getValueAt(row, 0);
        if (!(idValue instanceof Integer)) {
            JOptionPane.showMessageDialog(this, "ID de producto invalido en la tabla.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int productId = (Integer) idValue;
        try {
            int newStock = productRepository.adjustProductStock(productId, delta);
            tableModel.setValueAt(newStock, row, 2);
            updateStatus();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Stock invalido", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error actualizando stock", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus() {
        if (statusLabel != null) {
            statusLabel.setText("Items: " + tableModel.getRowCount());
        }
    }

    private void startClock() {
        if (timeLabel == null) {
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
        Timer timer = new Timer(1000, e -> timeLabel.setText(LocalDateTime.now().format(formatter)));
        timer.setInitialDelay(0);
        timer.start();
    }
}
