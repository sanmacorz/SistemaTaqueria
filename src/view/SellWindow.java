package view;

import model.ProductRepository;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SellWindow extends JFrame {
    private static final String IMAGE_DIR = "src/imgs";
    private static final int DEFAULT_CUSTOMER_ID = 1;
    private static final int DEFAULT_EMPLOYEE_ID = 1;
    private static final Color COLOR_HEADER = Color.decode("#F0C808");
    private static final Color COLOR_BACKGROUND = Color.decode("#F8F9FA");
    private static final Color COLOR_PANEL = Color.decode("#FFFFFF");
    private static final Color COLOR_BUTTON = Color.decode("#FFFFFF");
    private static final Color COLOR_ACCENT = Color.decode("#F0C808");
    private static final Color COLOR_TEXT = Color.decode("#1A1A1A");
    private static final Color COLOR_MUTED = Color.decode("#5E5E5E");
    private static final Color COLOR_SUCCESS = Color.decode("#3A7E00");
    private static final Color COLOR_DANGER = Color.decode("#D81E1E");
    private static final Color COLOR_BORDER = Color.decode("#E0E0E0");
    private static final int FOOTER_BUTTON_HEIGHT = 34;
    private static final int HEADER_BUTTON_HEIGHT = 40;
    private static final int HEADER_BUTTON_MIN_WIDTH = 110;
    private static final Dimension ITEM_BUTTON_SIZE = new Dimension(160, 120);
    private static final Dimension ITEM_ICON_SIZE = new Dimension(120, 80);
    private static final Map<String, String> ITEM_IMAGE_MAP = new HashMap<>();
    private static final Map<String, ImageIcon> ICON_CACHE = new HashMap<>();

    static {
        ITEM_IMAGE_MAP.put("cocacola", "cocacola.png");
        ITEM_IMAGE_MAP.put("manzana", "manzana.png");
        ITEM_IMAGE_MAP.put("durazno", "durazno.png");
        ITEM_IMAGE_MAP.put("ponchedefrutas", "ponchedefrutas.png");
        ITEM_IMAGE_MAP.put("tacodechicharron", "tacodechicharron.png");
        ITEM_IMAGE_MAP.put("tacodedeshebrada", "tacodedeshebrada.png");
        ITEM_IMAGE_MAP.put("tacodefrijol", "tacodefrijol.png");
        ITEM_IMAGE_MAP.put("tacodepapa", "tacodepapa.png");
        ITEM_IMAGE_MAP.put("taco", "taco.png");
    }

    private final List<String> itemNames;
    private final ProductRepository productRepository;
    private final Map<String, Integer> orderQuantities = new LinkedHashMap<>();
    private final Map<String, BigDecimal> unitPricesByName = new HashMap<>();
    private final Map<String, Integer> availableStockByName = new HashMap<>();
    private DefaultTableModel orderTableModel;
    private JTable orderTable;
    private JTextField areaField;
    private JTextField subtotalField;
    private JTextField discountField;
    private JTextField taxesField;
    private JTextField totalField;

    public SellWindow(List<String> itemNames, ProductRepository productRepository) {
        this.itemNames = itemNames == null ? new ArrayList<>() : new ArrayList<>(itemNames);
        this.productRepository = productRepository;
        if (this.productRepository != null && !this.itemNames.isEmpty()) {
            this.unitPricesByName.putAll(this.productRepository.getUnitPricesByNames(this.itemNames));
            this.availableStockByName.putAll(this.productRepository.getStocksByNames(this.itemNames));
        }
        this.setTitle("Venta");
        this.setSize(1280, 720);
        this.setMinimumSize(new Dimension(1200, 700));
        this.setResizable(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLayout(new BorderLayout(8, 8));
        this.getContentPane().setBackground(COLOR_BACKGROUND);
        this.setIconImage(loadWindowIcon());

        this.add(createHeaderPanel(), BorderLayout.NORTH);
        this.add(createContentPanel(), BorderLayout.CENTER);

        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private Image loadWindowIcon() {
        ImageIcon icon = loadScaledIcon(new File(IMAGE_DIR, "taco.png"), 32, 32);
        return icon == null ? null : icon.getImage();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBorder(new EmptyBorder(10, 12, 10, 12));
        header.setBackground(COLOR_HEADER);

        JLabel title = new JLabel("Servicio Rapido");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(COLOR_TEXT);
        header.add(title, BorderLayout.WEST);

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        quickActions.setBackground(COLOR_HEADER);
        quickActions.add(createHeaderButton("Enviar mensaje"));
        quickActions.add(createHeaderButton("Personas"));
        quickActions.add(createHeaderButton("Pagar cuenta"));
        quickActions.add(createHeaderButton("Tarjeta"));
        JButton closeHeader = createHeaderButton("Cerrar");
        closeHeader.setBackground(COLOR_DANGER);
        closeHeader.setForeground(COLOR_BACKGROUND);
        quickActions.add(closeHeader);
        header.add(quickActions, BorderLayout.EAST);

        return header;
    }

    private JPanel createContentPanel() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        content.setBackground(COLOR_BACKGROUND);

        content.add(createLeftPanel(), BorderLayout.WEST);
        content.add(createRightPanel(), BorderLayout.CENTER);

        return content;
    }

    private JPanel createLeftPanel() {
        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setBackground(COLOR_BACKGROUND);
        left.setPreferredSize(new Dimension(420, 0));

        left.add(createOrderPanel(), BorderLayout.CENTER);
        left.add(createTotalsPanel(), BorderLayout.SOUTH);

        return left;
    }

    private JPanel createOrderPanel() {
        JPanel orderPanel = new JPanel(new BorderLayout(6, 6));
        orderPanel.setBackground(COLOR_BACKGROUND);

        JPanel orderHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        orderHeader.setBackground(COLOR_BACKGROUND);
        JLabel areaLabel = new JLabel("Area:");
        areaLabel.setForeground(COLOR_TEXT);
        orderHeader.add(areaLabel);
        areaField = createSizedField("01", 40);
        orderHeader.add(areaField);
        orderPanel.add(orderHeader, BorderLayout.NORTH);

        orderTable = createOrderTable();
        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.getViewport().setBackground(COLOR_PANEL);
        orderPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel orderActions = new JPanel(new GridLayout(2, 1, 4, 4));
        orderActions.setBackground(COLOR_BACKGROUND);
        JButton removeOneButton = createSmallButton("-1");
        removeOneButton.setToolTipText("Quitar una unidad del producto seleccionado");
        removeOneButton.addActionListener(e -> removeOneSelectedItem());
        JButton clearOrderButton = createSmallButton("Limpiar");
        clearOrderButton.setToolTipText("Eliminar todos los productos de la orden");
        clearOrderButton.addActionListener(e -> clearOrderItems());
        orderActions.add(removeOneButton);
        orderActions.add(clearOrderButton);
        orderPanel.add(orderActions, BorderLayout.EAST);

        return orderPanel;
    }

    private JTable createOrderTable() {
        orderTableModel = new DefaultTableModel(new Object[][] {},
                new String[] { "CANT.", "DESCRIPCION", "DESC.", "IMPORTE" }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(orderTableModel);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.setBackground(COLOR_PANEL);
        table.setForeground(COLOR_TEXT);
        table.setSelectionBackground(COLOR_ACCENT);
        table.setSelectionForeground(COLOR_TEXT);
        table.setGridColor(COLOR_BORDER);
        table.getTableHeader().setBackground(COLOR_PANEL);
        table.getTableHeader().setForeground(COLOR_TEXT);
        return table;
    }

    private JPanel createTotalsPanel() {
        JPanel totals = new JPanel(new GridLayout(4, 2, 6, 6));
        totals.setBackground(COLOR_PANEL);
        TitledBorder totalsBorder = BorderFactory.createTitledBorder("Totales");
        totalsBorder.setTitleColor(COLOR_TEXT);
        totals.setBorder(totalsBorder);

        totals.add(createTotalsLabel("Subtotal:"));
        subtotalField = createTotalsField("0.00");
        totals.add(subtotalField);
        totals.add(createTotalsLabel("Descuento:"));
        discountField = createTotalsField("0.00");
        totals.add(discountField);
        totals.add(createTotalsLabel("Impuestos:"));
        taxesField = createTotalsField("0.00");
        totals.add(taxesField);
        totals.add(createTotalsLabel("TOTAL:"));
        totalField = createTotalsField("0.00");
        totals.add(totalField);

        return totals;
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBackground(COLOR_BACKGROUND);

        right.add(createItemsPanel(), BorderLayout.CENTER);
        right.add(createBottomPanel(), BorderLayout.SOUTH);

        return right;
    }

    private JPanel createItemsPanel() {
        JPanel itemsGrid = new JPanel(new GridLayout(0, 4, 8, 8));
        itemsGrid.setBackground(COLOR_BACKGROUND);

        if (itemNames.isEmpty()) {
            itemsGrid.add(createItemButton("Sin items"));
        } else {
            for (String itemName : itemNames) {
                itemsGrid.add(createItemButton(itemName));
            }
        }

        JScrollPane scrollPane = new JScrollPane(itemsGrid);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(COLOR_BACKGROUND);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(COLOR_BACKGROUND);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setBackground(COLOR_BACKGROUND);

        JButton commentButton = createFooterButton("Comentario escrito");
        commentButton.setBackground(COLOR_ACCENT);
        commentButton.setForeground(COLOR_TEXT);
        bottom.add(commentButton);
        JButton advanceButton = createFooterButton("Avanzar");
        advanceButton.setBackground(COLOR_SUCCESS);
        advanceButton.setForeground(COLOR_BACKGROUND);
        advanceButton.addActionListener(e -> submitCurrentOrder());
        bottom.add(advanceButton);

        return bottom;
    }

    private JButton createHeaderButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(COLOR_BUTTON);
        button.setForeground(COLOR_TEXT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        button.setMargin(new java.awt.Insets(6, 12, 6, 12));
        Dimension size = button.getPreferredSize();
        int width = Math.max(size.width, HEADER_BUTTON_MIN_WIDTH);
        button.setPreferredSize(new Dimension(width, HEADER_BUTTON_HEIGHT));
        return button;
    }

    private JButton createSmallButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBackground(COLOR_BUTTON);
        button.setForeground(COLOR_TEXT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        return button;
    }

    private JButton createFooterButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBackground(COLOR_PANEL);
        button.setForeground(COLOR_TEXT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        Dimension size = button.getPreferredSize();
        button.setPreferredSize(new Dimension(size.width, FOOTER_BUTTON_HEIGHT));
        return button;
    }

    private JButton createItemButton(String itemName) {
        JButton button = new JButton();
        button.setPreferredSize(ITEM_BUTTON_SIZE);
        button.setBackground(COLOR_PANEL);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(COLOR_ACCENT, 2));
        button.setForeground(COLOR_TEXT);

        ImageIcon icon = loadItemIcon(itemName);
        if (icon != null) {
            button.setIcon(icon);
        }

        button.setText("<html><div style='text-align:center;'>" + itemName + "</div></html>");
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setIconTextGap(6);
        button.setToolTipText("Clic izquierdo: +1 | Clic derecho: -1");

        button.addActionListener(e -> adjustItemQuantity(itemName, 1));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    adjustItemQuantity(itemName, -1);
                }
            }
        });
        return button;
    }

    private void adjustItemQuantity(String itemName, int change) {
        if (orderTableModel == null || itemName == null || itemName.isBlank()) {
            return;
        }

        if (change > 0 && !canAddOneMore(itemName)) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se puede agregar \"" + itemName + "\" porque no hay stock disponible.",
                    "Sin stock",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int nextQuantity = orderQuantities.getOrDefault(itemName, 0) + change;
        if (nextQuantity <= 0) {
            orderQuantities.remove(itemName);
        } else {
            orderQuantities.put(itemName, nextQuantity);
        }

        refreshOrderTable();
    }

    private boolean canAddOneMore(String itemName) {
        int availableStock = availableStockByName.getOrDefault(itemName, 0);
        int currentQuantity = orderQuantities.getOrDefault(itemName, 0);
        return currentQuantity < availableStock;
    }

    private void refreshOrderTable() {
        orderTableModel.setRowCount(0);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : orderQuantities.entrySet()) {
            BigDecimal unitPrice = unitPricesByName.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(entry.getValue()));
            subtotal = subtotal.add(lineTotal);
            orderTableModel.addRow(new Object[] { entry.getValue(), entry.getKey(), "0%", formatMoney(lineTotal) });
        }
        updateTotalsFields(subtotal);
    }

    private String formatMoney(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private void updateTotalsFields(BigDecimal subtotal) {
        if (subtotalField == null || discountField == null || taxesField == null || totalField == null) {
            return;
        }

        BigDecimal discount = parseMoney(discountField.getText());
        BigDecimal taxes = parseMoney(taxesField.getText());
        BigDecimal total = subtotal.subtract(discount).add(taxes);

        subtotalField.setText(formatMoney(subtotal));
        totalField.setText(formatMoney(total));
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void submitCurrentOrder() {
        if (productRepository == null) {
            JOptionPane.showMessageDialog(this, "No hay repositorio de ventas disponible.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (orderQuantities.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agrega al menos un producto antes de continuar.", "Orden vacia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String areaValue = normalizeAreaForDatabase();
            productRepository.registerSale(orderQuantities, DEFAULT_CUSTOMER_ID, DEFAULT_EMPLOYEE_ID, areaValue);
            if (areaField != null) {
                areaField.setText(String.format("%02d", Integer.parseInt(areaValue)));
            }
            orderQuantities.clear();
            refreshOrderTable();
            JOptionPane.showMessageDialog(this, "Venta registrada correctamente.", "Exito",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Area invalida", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    buildSaleErrorDebugMessage(ex),
                    "Error al registrar venta",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String normalizeAreaForDatabase() {
        String rawArea = areaField == null ? "" : areaField.getText();
        if (rawArea == null || rawArea.isBlank()) {
            throw new IllegalArgumentException("El campo Area no puede estar vacio.");
        }

        String normalized = rawArea.trim();
        if (!normalized.matches("\\d+")) {
            throw new IllegalArgumentException("El Area solo puede contener numeros.");
        }

        int areaNumber = Integer.parseInt(normalized);
        if (areaNumber <= 0) {
            throw new IllegalArgumentException("El Area debe ser mayor a 0.");
        }

        return String.valueOf(areaNumber);
    }

    private String buildSaleErrorDebugMessage(RuntimeException ex) {
        StringBuilder debug = new StringBuilder();
        debug.append("No se pudo registrar la venta.\n\n");
        debug.append("Detalle: ").append(ex.getMessage() == null ? "(sin mensaje)" : ex.getMessage());

        Throwable root = getRootCause(ex);
        if (root != null && root != ex) {
            debug.append("\n\nCausa raiz: ").append(root.getClass().getSimpleName())
                    .append(" - ").append(root.getMessage());
        }

        debug.append("\n\nRevisa la consola para ver el stack trace completo.");
        return debug.toString();
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void removeOneSelectedItem() {
        if (orderTable == null) {
            return;
        }
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto en la lista para eliminar una unidad.",
                    "Sin seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object nameValue = orderTableModel.getValueAt(selectedRow, 1);
        if (!(nameValue instanceof String)) {
            return;
        }

        adjustItemQuantity((String) nameValue, -1);
    }

    private void clearOrderItems() {
        if (orderQuantities.isEmpty()) {
            return;
        }
        orderQuantities.clear();
        refreshOrderTable();
    }

    private JLabel createTotalsLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(COLOR_TEXT);
        return label;
    }

    private JTextField createTotalsField(String value) {
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.setBackground(COLOR_PANEL);
        field.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        field.setForeground(COLOR_TEXT);
        return field;
    }

    private JTextField createSizedField(String value, int width) {
        JTextField field = new JTextField(value);
        field.setPreferredSize(new Dimension(width, 22));
        field.setBackground(COLOR_PANEL);
        field.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        field.setForeground(COLOR_TEXT);
        return field;
    }

    private ImageIcon loadItemIcon(String itemName) {
        String key = normalizeKey(itemName);
        String fileName = ITEM_IMAGE_MAP.get(key);

        if (fileName == null) {
            for (Map.Entry<String, String> entry : ITEM_IMAGE_MAP.entrySet()) {
                if (key.contains(entry.getKey())) {
                    fileName = entry.getValue();
                    break;
                }
            }
        }

        if (fileName == null) {
            fileName = "taco.png";
        }

        File file = new File(IMAGE_DIR, fileName);
        ImageIcon icon = loadScaledIcon(file, ITEM_ICON_SIZE.width, ITEM_ICON_SIZE.height);
        if (icon != null) {
            return icon;
        }

        return createPlaceholderIcon(itemName, ITEM_ICON_SIZE.width, ITEM_ICON_SIZE.height);
    }

    private ImageIcon loadScaledIcon(File file, int width, int height) {
        String cacheKey = file.getPath() + "#" + width + "x" + height;
        ImageIcon cached = ICON_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            Image image = ImageIO.read(file);
            if (image == null) {
                return null;
            }

            Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaled);
            ICON_CACHE.put(cacheKey, icon);
            return icon;
        } catch (IOException e) {
            return null;
        }
    }

    private ImageIcon createPlaceholderIcon(String text, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(COLOR_PANEL);
        g2.fillRect(0, 0, width, height);
        g2.setColor(COLOR_MUTED);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString(text.length() > 12 ? text.substring(0, 12) + "..." : text, 6, height / 2);
        g2.dispose();
        return new ImageIcon(image);
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized.toLowerCase();
        return normalized.replaceAll("[^a-z0-9]", "");
    }
}
