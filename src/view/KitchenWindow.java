package view;

import model.ProductRepository;
import model.ProductRepository.KitchenOrderRow;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KitchenWindow extends JFrame {
    private static final List<String> AREA_KEYS = List.of("1", "2", "3", "4", "5", "6");
    private static final Color COLOR_BACKGROUND = Color.decode("#F8F9FA");
    private static final Color COLOR_HEADER = Color.decode("#F0C808");
    private static final Color COLOR_TICKET_HEADER = Color.decode("#F0C808");
    private static final Color COLOR_TICKET_BORDER = Color.decode("#E0E0E0");
    private static final Color COLOR_PANEL = Color.decode("#FFFFFF");
    private static final Color COLOR_TEXT = Color.decode("#1A1A1A");
    private static final Color COLOR_HIGHLIGHT = Color.decode("#F0C808");
    private static final Color COLOR_SUCCESS = Color.decode("#3A7E00");
    private static final Color COLOR_DANGER = Color.decode("#D81E1E");
    private static final int FOOTER_BUTTON_HEIGHT = 34;

    private final ProductRepository productRepository;
    private final Map<String, DefaultListModel<QueueLine>> areaModels = new LinkedHashMap<>();
    private final Map<String, JList<QueueLine>> areaLists = new LinkedHashMap<>();
    private JButton markDoneButton;
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JLabel errorLabel;

    public KitchenWindow(ProductRepository productRepository) {
        this.productRepository = productRepository;

        this.setTitle("Monitor de produccion");
        this.setSize(1280, 720);
        this.setMinimumSize(new Dimension(1150, 680));
        this.setResizable(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLayout(new BorderLayout(8, 8));
        this.getContentPane().setBackground(COLOR_BACKGROUND);

        this.add(createHeaderPanel(), BorderLayout.NORTH);
        this.add(createAreasPanel(), BorderLayout.CENTER);
        this.add(createFooterPanel(), BorderLayout.SOUTH);

        loadPendingOrdersFromDatabase();
        startClock();

        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_HEADER);
        header.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("Monitor de produccion para SISTEMA TAQUERIA");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(COLOR_TEXT);
        header.add(title, BorderLayout.WEST);

        timeLabel = new JLabel(currentTimeText());
        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        timeLabel.setForeground(COLOR_TEXT);
        header.add(timeLabel, BorderLayout.EAST);

        return header;
    }

    private JPanel createAreasPanel() {
        JPanel areaGrid = new JPanel(new GridLayout(2, 3, 8, 8));
        areaGrid.setBackground(COLOR_BACKGROUND);
        areaGrid.setBorder(new EmptyBorder(6, 6, 6, 6));

        for (String areaKey : AREA_KEYS) {
            areaGrid.add(createAreaPanel(areaKey));
        }

        return areaGrid;
    }

    private JPanel createAreaPanel(String areaKey) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_PANEL);
        panel.setBorder(BorderFactory.createLineBorder(COLOR_TICKET_BORDER, 2));

        JLabel header = new JLabel("AREA " + formatArea(areaKey));
        header.setOpaque(true);
        header.setBackground(COLOR_TICKET_HEADER);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBorder(new EmptyBorder(6, 8, 6, 8));
        header.setForeground(COLOR_TEXT);
        panel.add(header, BorderLayout.NORTH);

        DefaultListModel<QueueLine> model = new DefaultListModel<>();
        model.addElement(QueueLine.placeholder("(Sin ordenes pendientes)"));
        areaModels.put(areaKey, model);

        JList<QueueLine> list = new JList<>(model);
        list.setBackground(COLOR_PANEL);
        list.setFont(new Font("SansSerif", Font.BOLD, 12));
        list.setForeground(COLOR_TEXT);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new QueueLineRenderer());
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            if (list.getSelectedIndex() >= 0) {
                clearSelectionFromOtherLists(list);
            }
            updateMarkDoneButtonState();
        });
        areaLists.put(areaKey, list);

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.getViewport().setBackground(COLOR_PANEL);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(COLOR_HEADER);
        footer.setBorder(new EmptyBorder(8, 12, 8, 12));

        statusLabel = new JLabel("Ordenes pendientes: 0");
        statusLabel.setForeground(COLOR_TEXT);
        footer.add(statusLabel, BorderLayout.WEST);

        errorLabel = new JLabel(" ");
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        errorLabel.setForeground(COLOR_DANGER);
        footer.add(errorLabel, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setBackground(COLOR_HEADER);
        markDoneButton = createFooterButton("Marcar hecho");
        markDoneButton.setEnabled(false);
        markDoneButton.addActionListener(e -> markSelectedAsDone());
        markDoneButton.setBackground(COLOR_SUCCESS);
        markDoneButton.setForeground(COLOR_BACKGROUND);

        JButton refreshButton = createFooterButton("Actualizar");
        refreshButton.addActionListener(e -> loadPendingOrdersFromDatabase());
        refreshButton.setBackground(COLOR_HIGHLIGHT);
        refreshButton.setForeground(COLOR_TEXT);
        JButton closeButton = createFooterButton("Cerrar");
        closeButton.addActionListener(e -> dispose());
        closeButton.setBackground(COLOR_DANGER);
        closeButton.setForeground(COLOR_BACKGROUND);

        right.add(markDoneButton);
        right.add(refreshButton);
        right.add(closeButton);
        footer.add(right, BorderLayout.EAST);

        return footer;
    }

    private void loadPendingOrdersFromDatabase() {
        clearAreaModels();
        clearAllSelections();

        if (productRepository == null) {
            setError("Repositorio no disponible");
            return;
        }

        try {
            List<KitchenOrderRow> rows = productRepository.getPendingKitchenRows();
            Map<String, LinkedHashMap<Integer, KitchenOrder>> queueByArea = buildQueueByArea(rows);
            renderQueueByArea(queueByArea);
            int totalOrders = countOrders(queueByArea);
            statusLabel.setText("Ordenes pendientes: " + totalOrders);
            setError(" ");
            updateMarkDoneButtonState();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            setError("Error cargando ordenes: " + ex.getMessage());
        }
    }

    private Map<String, LinkedHashMap<Integer, KitchenOrder>> buildQueueByArea(List<KitchenOrderRow> rows) {
        Map<String, LinkedHashMap<Integer, KitchenOrder>> queueByArea = new LinkedHashMap<>();
        for (String key : AREA_KEYS) {
            queueByArea.put(key, new LinkedHashMap<>());
        }

        for (KitchenOrderRow row : rows) {
            String areaKey = normalizeArea(row.getArea());
            if (!queueByArea.containsKey(areaKey)) {
                continue;
            }

            LinkedHashMap<Integer, KitchenOrder> areaQueue = queueByArea.get(areaKey);
            KitchenOrder order = areaQueue.get(row.getOrderId());
            if (order == null) {
                order = new KitchenOrder(row.getOrderId(), row.getEmployeeId(), row.getCreatedAt());
                areaQueue.put(row.getOrderId(), order);
            }
            order.items.add(new KitchenItem(row.getDetailId(), row.getQuantity(), row.getProductName()));
        }

        return queueByArea;
    }

    private void renderQueueByArea(Map<String, LinkedHashMap<Integer, KitchenOrder>> queueByArea) {
        for (String areaKey : AREA_KEYS) {
            DefaultListModel<QueueLine> model = areaModels.get(areaKey);
            if (model == null) {
                continue;
            }

            model.clear();
            LinkedHashMap<Integer, KitchenOrder> areaQueue = queueByArea.get(areaKey);
            if (areaQueue == null || areaQueue.isEmpty()) {
                model.addElement(QueueLine.placeholder("(Sin ordenes pendientes)"));
                continue;
            }

            for (KitchenOrder order : areaQueue.values()) {
                String headerText = "Pedido #" + order.orderId + " | " + formatOrderTime(order.createdAt) + " | Mesero "
                        + order.employeeId;
                model.addElement(QueueLine.orderHeader(headerText, order.orderId));
                for (KitchenItem item : order.items) {
                    String lineText = "  " + item.quantity + " x " + item.name;
                    model.addElement(QueueLine.orderItem(lineText, order.orderId, item.detailId));
                }
                model.addElement(QueueLine.separator());
            }
        }
    }

    private int countOrders(Map<String, LinkedHashMap<Integer, KitchenOrder>> queueByArea) {
        int count = 0;
        for (LinkedHashMap<Integer, KitchenOrder> areaQueue : queueByArea.values()) {
            count += areaQueue.size();
        }
        return count;
    }

    private void clearAreaModels() {
        for (DefaultListModel<QueueLine> model : areaModels.values()) {
            model.clear();
            model.addElement(QueueLine.placeholder("(Sin ordenes pendientes)"));
        }
    }

    private void clearAllSelections() {
        for (JList<QueueLine> list : areaLists.values()) {
            list.clearSelection();
        }
    }

    private void clearSelectionFromOtherLists(JList<QueueLine> selectedList) {
        for (JList<QueueLine> list : areaLists.values()) {
            if (list != selectedList) {
                list.clearSelection();
            }
        }
    }

    private void markSelectedAsDone() {
        if (productRepository == null) {
            setError("Repositorio no disponible");
            return;
        }

        QueueLine selectedLine = getSelectedLine();
        if (selectedLine == null || !selectedLine.isActionable()) {
            JOptionPane.showMessageDialog(this, "Selecciona un pedido o un item para marcar como hecho.",
                    "Sin seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean updated;
            if (selectedLine.isOrderHeader()) {
                updated = productRepository.markOrderAsDone(selectedLine.orderId);
            } else {
                updated = productRepository.markSingleDetailItemAsDone(selectedLine.detailId);
            }

            if (!updated) {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar el estado (puede que ya haya cambiado).",
                        "Sin cambios", JOptionPane.WARNING_MESSAGE);
            }
            loadPendingOrdersFromDatabase();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al actualizar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private QueueLine getSelectedLine() {
        for (JList<QueueLine> list : areaLists.values()) {
            QueueLine selected = list.getSelectedValue();
            if (selected != null) {
                return selected;
            }
        }
        return null;
    }

    private void updateMarkDoneButtonState() {
        if (markDoneButton == null) {
            return;
        }
        QueueLine selected = getSelectedLine();
        markDoneButton.setEnabled(selected != null && selected.isActionable());
    }

    private void setError(String text) {
        if (errorLabel != null) {
            errorLabel.setText(text == null || text.isBlank() ? " " : text);
        }
    }

    private JButton createFooterButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(COLOR_TEXT);
        button.setBackground(COLOR_PANEL);
        Dimension size = button.getPreferredSize();
        button.setPreferredSize(new Dimension(size.width, FOOTER_BUTTON_HEIGHT));
        return button;
    }

    private String normalizeArea(String areaRaw) {
        if (areaRaw == null || areaRaw.isBlank()) {
            return "";
        }
        String trimmed = areaRaw.trim();
        if (!trimmed.matches("\\d+")) {
            return trimmed;
        }
        return String.valueOf(Integer.parseInt(trimmed));
    }

    private String formatArea(String areaKey) {
        try {
            return String.format("%02d", Integer.parseInt(areaKey));
        } catch (NumberFormatException e) {
            return areaKey;
        }
    }

    private String formatOrderTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "--:--";
        }
        return timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String currentTimeText() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
        return LocalDateTime.now().format(formatter);
    }

    private void startClock() {
        if (timeLabel == null) {
            return;
        }
        Timer timer = new Timer(1000, e -> timeLabel.setText(currentTimeText()));
        timer.setInitialDelay(0);
        timer.start();
    }

    private static class KitchenOrder {
        private final int orderId;
        private final int employeeId;
        private final Timestamp createdAt;
        private final List<KitchenItem> items = new ArrayList<>();

        private KitchenOrder(int orderId, int employeeId, Timestamp createdAt) {
            this.orderId = orderId;
            this.employeeId = employeeId;
            this.createdAt = createdAt;
        }
    }

    private static class KitchenItem {
        private final int detailId;
        private final int quantity;
        private final String name;

        private KitchenItem(int detailId, int quantity, String name) {
            this.detailId = detailId;
            this.quantity = quantity;
            this.name = name;
        }
    }

    private static class QueueLine {
        private enum Type {
            ORDER_HEADER,
            ORDER_ITEM,
            PLACEHOLDER,
            SEPARATOR
        }

        private final Type type;
        private final String text;
        private final int orderId;
        private final int detailId;

        private QueueLine(Type type, String text, int orderId, int detailId) {
            this.type = type;
            this.text = text;
            this.orderId = orderId;
            this.detailId = detailId;
        }

        private static QueueLine orderHeader(String text, int orderId) {
            return new QueueLine(Type.ORDER_HEADER, text, orderId, -1);
        }

        private static QueueLine orderItem(String text, int orderId, int detailId) {
            return new QueueLine(Type.ORDER_ITEM, text, orderId, detailId);
        }

        private static QueueLine placeholder(String text) {
            return new QueueLine(Type.PLACEHOLDER, text, -1, -1);
        }

        private static QueueLine separator() {
            return new QueueLine(Type.SEPARATOR, " ", -1, -1);
        }

        private boolean isOrderHeader() {
            return type == Type.ORDER_HEADER;
        }

        private boolean isActionable() {
            return type == Type.ORDER_HEADER || type == Type.ORDER_ITEM;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private static class QueueLineRenderer extends JLabel implements javax.swing.ListCellRenderer<QueueLine> {
        private QueueLineRenderer() {
            setOpaque(true);
            setFont(new Font("SansSerif", Font.BOLD, 12));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends QueueLine> list,
                QueueLine value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            setText(value == null ? "" : value.toString());

            if (value != null && value.type == QueueLine.Type.ORDER_HEADER) {
                setFont(new Font("SansSerif", Font.BOLD, 12));
            } else {
                setFont(new Font("SansSerif", Font.PLAIN, 12));
            }

            if (isSelected) {
                setBackground(COLOR_HIGHLIGHT);
                setForeground(COLOR_TEXT);
            } else {
                setBackground(COLOR_PANEL);
                setForeground(COLOR_TEXT);
            }
            return this;
        }
    }
}
