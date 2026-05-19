package view;

import model.ProductRepository;
import model.ProductRepository.StatsFilter;
import model.ProductRepository.StatsSaleRow;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StatsWindow extends JFrame {
    private static final Color COLOR_HEADER = Color.decode("#F0C808");
    private static final Color COLOR_BACKGROUND = Color.decode("#F8F9FA");
    private static final Color COLOR_PANEL = Color.decode("#FFFFFF");
    private static final Color COLOR_TEXT = Color.decode("#1A1A1A");
    private static final Color COLOR_MUTED = Color.decode("#5E5E5E");
    private static final Color COLOR_HIGHLIGHT = Color.decode("#F0C808");
    private static final Color COLOR_BORDER = Color.decode("#E0E0E0");
    private static final Color COLOR_DANGER = Color.decode("#D81E1E");

    private final ProductRepository productRepository;

    private final DefaultTableModel soldTableModel;
    private final JTable soldTable;
    private final JLabel statusLabel;
    private final JLabel timeLabel;

    private final JSpinner fromSpinner;
    private final JSpinner toSpinner;

    private JList<String> reportList;
    private JComboBox<String> groupCombo;
    private JTable groupTable;

    private JRadioButton turnoRadio;
    private JRadioButton periodoRadio;
    private JRadioButton anualRadio;

    private JComboBox<String> serieCombo;
    private JComboBox<String> imprimirCostoCombo;
    private JComboBox<String> servicioCombo;
    private JComboBox<String> areaCombo;
    private JComboBox<String> mesaCombo;
    private JComboBox<String> cuentasCombo;
    private JComboBox<String> productosCombo;
    private JComboBox<String> clasificacionCombo;
    private JComboBox<String> ordenCombo;

    private JCheckBox incluirImpuestosCheck;
    private JCheckBox precioPromedioCheck;
    private JCheckBox agruparProductosCheck;
    private JCheckBox imprimirResumenCheck;

    private final List<AggregatedRow> currentRows = new ArrayList<>();

    public StatsWindow(ProductRepository productRepository) {
        this.productRepository = productRepository;

        this.setTitle("Reporte de productos vendidos");
        this.setSize(1280, 720);
        this.setMinimumSize(new Dimension(1100, 680));
        this.setResizable(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setLayout(new BorderLayout(8, 8));
        this.getContentPane().setBackground(COLOR_BACKGROUND);

        this.add(createHeaderPanel(), BorderLayout.NORTH);

        soldTableModel = new DefaultTableModel(new Object[][] {}, new String[] { "PRODUCTO", "CANTIDAD" }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 1 ? Integer.class : String.class;
            }
        };
        soldTable = new JTable(soldTableModel);
        soldTable.setRowHeight(22);
        soldTable.setFillsViewportHeight(true);
        soldTable.setBackground(COLOR_PANEL);
        soldTable.setForeground(COLOR_TEXT);
        soldTable.setSelectionBackground(COLOR_HIGHLIGHT);
        soldTable.setSelectionForeground(COLOR_TEXT);
        soldTable.setGridColor(COLOR_BORDER);
        soldTable.getTableHeader().setBackground(COLOR_PANEL);
        soldTable.getTableHeader().setForeground(COLOR_TEXT);

        fromSpinner = createDateSpinner();
        toSpinner = createDateSpinner();
        setDefaultDateRange();

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBackground(COLOR_BACKGROUND);
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        content.add(createLeftPanel(), BorderLayout.WEST);
        content.add(createRightPanel(), BorderLayout.CENTER);
        this.add(content, BorderLayout.CENTER);

        JPanel footer = createFooterPanel();
        statusLabel = (JLabel) footer.getClientProperty("statusLabel");
        timeLabel = (JLabel) footer.getClientProperty("timeLabel");
        this.add(footer, BorderLayout.SOUTH);

        startClock();
        updateSoldTable();

        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_HEADER);
        header.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("Reporte de productos vendidos");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(COLOR_TEXT);
        header.add(title, BorderLayout.WEST);

        return header;
    }

    private JPanel createLeftPanel() {
        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setBackground(COLOR_BACKGROUND);
        left.setPreferredSize(new Dimension(360, 0));

        left.add(createReportListPanel(), BorderLayout.NORTH);
        left.add(createGroupPanel(), BorderLayout.CENTER);

        return left;
    }

    private JPanel createReportListPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(COLOR_PANEL);
        TitledBorder reportBorder = BorderFactory.createTitledBorder("Reportes");
        reportBorder.setTitleColor(COLOR_TEXT);
        panel.setBorder(reportBorder);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("Productos vendidos en general");
        listModel.addElement("Productos vendidos por mesero / repartidor");
        listModel.addElement("Maximos y promedios");
        reportList = new JList<>(listModel);
        reportList.setSelectedIndex(0);
        reportList.setBackground(COLOR_PANEL);
        reportList.setForeground(COLOR_TEXT);
        reportList.setSelectionBackground(COLOR_HIGHLIGHT);
        reportList.setSelectionForeground(COLOR_TEXT);
        reportList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSoldTable();
            }
        });
        JScrollPane reportScroll = new JScrollPane(reportList);
        reportScroll.getViewport().setBackground(COLOR_PANEL);
        panel.add(reportScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGroupPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(COLOR_PANEL);
        TitledBorder groupBorder = BorderFactory.createTitledBorder("Filtrar por");
        groupBorder.setTitleColor(COLOR_TEXT);
        panel.setBorder(groupBorder);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        top.setBackground(COLOR_PANEL);
        top.add(createLabel("Grupo:"));
        groupCombo = new JComboBox<>(new String[] { "Todos", "Tacos", "Bebidas", "Extras", "Otros" });
        groupCombo.setBackground(COLOR_PANEL);
        groupCombo.setForeground(COLOR_TEXT);
        groupCombo.addActionListener(e -> updateSoldTable());
        top.add(groupCombo);
        panel.add(top, BorderLayout.NORTH);

        groupTable = createGroupTable();
        JScrollPane groupScroll = new JScrollPane(groupTable);
        groupScroll.getViewport().setBackground(COLOR_PANEL);
        panel.add(groupScroll, BorderLayout.CENTER);

        return panel;
    }

    private JTable createGroupTable() {
        DefaultTableModel model = new DefaultTableModel(new Object[][] {
                { "01", "TACOS", true },
                { "02", "BEBIDAS", true },
                { "03", "EXTRAS", true },
                { "04", "OTROS", true }
        }, new String[] { "CLAVE", "DESCRIPCION", "INCLUIR" }) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) {
                    return Boolean.class;
                }
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setBackground(COLOR_PANEL);
        table.setForeground(COLOR_TEXT);
        table.setSelectionBackground(COLOR_HIGHLIGHT);
        table.setSelectionForeground(COLOR_TEXT);
        table.setGridColor(COLOR_BORDER);
        table.getTableHeader().setBackground(COLOR_PANEL);
        table.getTableHeader().setForeground(COLOR_TEXT);
        model.addTableModelListener(e -> updateSoldTable());
        return table;
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBackground(COLOR_BACKGROUND);

        right.add(createFilterPanel(), BorderLayout.NORTH);
        right.add(createSoldTablePanel(), BorderLayout.CENTER);
        right.add(createActionPanel(), BorderLayout.SOUTH);

        return right;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_PANEL);
        TitledBorder filterBorder = BorderFactory.createTitledBorder("Parametros");
        filterBorder.setTitleColor(COLOR_TEXT);
        panel.setBorder(filterBorder);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(createLabel("Modo:"), gbc);

        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        radios.setBackground(COLOR_PANEL);
        turnoRadio = new JRadioButton("Turno");
        periodoRadio = new JRadioButton("Periodo");
        anualRadio = new JRadioButton("Anual");
        turnoRadio.setBackground(COLOR_PANEL);
        periodoRadio.setBackground(COLOR_PANEL);
        anualRadio.setBackground(COLOR_PANEL);
        turnoRadio.setForeground(COLOR_TEXT);
        periodoRadio.setForeground(COLOR_TEXT);
        anualRadio.setForeground(COLOR_TEXT);
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(turnoRadio);
        modeGroup.add(periodoRadio);
        modeGroup.add(anualRadio);
        periodoRadio.setSelected(true);

        turnoRadio.addActionListener(e -> {
            setShiftDateRange();
            updateSoldTable();
        });
        periodoRadio.addActionListener(e -> updateSoldTable());
        anualRadio.addActionListener(e -> {
            setYearDateRange();
            updateSoldTable();
        });

        radios.add(turnoRadio);
        radios.add(periodoRadio);
        radios.add(anualRadio);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        panel.add(radios, gbc);

        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(createLabel("Del:"), gbc);
        gbc.gridx = 1;
        panel.add(fromSpinner, gbc);
        gbc.gridx = 2;
        panel.add(createLabel("Al:"), gbc);
        gbc.gridx = 3;
        panel.add(toSpinner, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(createLabel("Serie:"), gbc);
        gbc.gridx = 1;
        serieCombo = new JComboBox<>(new String[] { "(Todas)", "A", "B", "C" });
        serieCombo.setBackground(COLOR_PANEL);
        serieCombo.setForeground(COLOR_TEXT);
        panel.add(serieCombo, gbc);
        gbc.gridx = 2;
        panel.add(createLabel("Imprimir costo:"), gbc);
        gbc.gridx = 3;
        imprimirCostoCombo = new JComboBox<>(new String[] { "No", "Si" });
        imprimirCostoCombo.setBackground(COLOR_PANEL);
        imprimirCostoCombo.setForeground(COLOR_TEXT);
        panel.add(imprimirCostoCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(createLabel("Servicio:"), gbc);
        gbc.gridx = 1;
        servicioCombo = new JComboBox<>(new String[] { "(Todos)", "Comedor", "Barra", "Entrega" });
        servicioCombo.setBackground(COLOR_PANEL);
        servicioCombo.setForeground(COLOR_TEXT);
        panel.add(servicioCombo, gbc);
        gbc.gridx = 2;
        panel.add(createLabel("Area:"), gbc);
        gbc.gridx = 3;
        areaCombo = createAreaCombo();
        areaCombo.setBackground(COLOR_PANEL);
        areaCombo.setForeground(COLOR_TEXT);
        panel.add(areaCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(createLabel("Mesa:"), gbc);
        gbc.gridx = 1;
        mesaCombo = new JComboBox<>(new String[] { "(Todas)", "01", "02", "03" });
        mesaCombo.setBackground(COLOR_PANEL);
        mesaCombo.setForeground(COLOR_TEXT);
        panel.add(mesaCombo, gbc);
        gbc.gridx = 2;
        panel.add(createLabel("Cuentas:"), gbc);
        gbc.gridx = 3;
        cuentasCombo = new JComboBox<>(new String[] { "Activas y pagadas", "Solo pagadas" });
        cuentasCombo.setBackground(COLOR_PANEL);
        cuentasCombo.setForeground(COLOR_TEXT);
        panel.add(cuentasCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(createLabel("Productos:"), gbc);
        gbc.gridx = 1;
        productosCombo = new JComboBox<>(new String[] { "Vendidos", "Devueltos" });
        productosCombo.setBackground(COLOR_PANEL);
        productosCombo.setForeground(COLOR_TEXT);
        panel.add(productosCombo, gbc);
        gbc.gridx = 2;
        panel.add(createLabel("Clasificacion:"), gbc);
        gbc.gridx = 3;
        clasificacionCombo = new JComboBox<>(new String[] { "(Todas)", "A", "B" });
        clasificacionCombo.setBackground(COLOR_PANEL);
        clasificacionCombo.setForeground(COLOR_TEXT);
        panel.add(clasificacionCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(createLabel("Orden:"), gbc);
        gbc.gridx = 1;
        ordenCombo = new JComboBox<>(new String[] { "Alfabetico", "Cantidad" });
        ordenCombo.setBackground(COLOR_PANEL);
        ordenCombo.setForeground(COLOR_TEXT);
        panel.add(ordenCombo, gbc);

        JPanel checks = new JPanel(new GridLayout(2, 2));
        checks.setBackground(COLOR_PANEL);
        incluirImpuestosCheck = new JCheckBox("Incluir impuestos");
        precioPromedioCheck = new JCheckBox("Calcular precio promedio");
        agruparProductosCheck = new JCheckBox("Agrupar productos");
        imprimirResumenCheck = new JCheckBox("Imprimir resumen");
        agruparProductosCheck.setSelected(true);

        checks.add(incluirImpuestosCheck);
        checks.add(precioPromedioCheck);
        checks.add(agruparProductosCheck);
        checks.add(imprimirResumenCheck);

        for (java.awt.Component component : checks.getComponents()) {
            if (component instanceof JCheckBox) {
                component.setBackground(COLOR_PANEL);
                component.setForeground(COLOR_TEXT);
                ((JCheckBox) component).addActionListener(e -> updateSoldTable());
            }
        }

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 4;
        panel.add(checks, gbc);
        gbc.gridwidth = 1;

        ChangeListener rangeListener = e -> updateSoldTable();
        fromSpinner.addChangeListener(rangeListener);
        toSpinner.addChangeListener(rangeListener);

        serieCombo.addActionListener(e -> updateSoldTable());
        imprimirCostoCombo.addActionListener(e -> updateSoldTable());
        servicioCombo.addActionListener(e -> updateSoldTable());
        areaCombo.addActionListener(e -> updateSoldTable());
        mesaCombo.addActionListener(e -> updateSoldTable());
        cuentasCombo.addActionListener(e -> updateSoldTable());
        productosCombo.addActionListener(e -> updateSoldTable());
        clasificacionCombo.addActionListener(e -> updateSoldTable());
        ordenCombo.addActionListener(e -> updateSoldTable());

        return panel;
    }

    private JComboBox<String> createAreaCombo() {
        List<String> items = new ArrayList<>();
        items.add("(Todas)");
        if (productRepository != null) {
            try {
                for (String area : productRepository.getDistinctAreaValues()) {
                    if (!area.isBlank()) {
                        items.add(area);
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        JComboBox<String> combo = new JComboBox<>(items.toArray(new String[0]));
        combo.setBackground(COLOR_PANEL);
        combo.setForeground(COLOR_TEXT);
        return combo;
    }

    private JPanel createSoldTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(COLOR_PANEL);
        TitledBorder soldBorder = BorderFactory.createTitledBorder("Productos vendidos");
        soldBorder.setTitleColor(COLOR_TEXT);
        panel.setBorder(soldBorder);

        JScrollPane scrollPane = new JScrollPane(soldTable);
        scrollPane.getViewport().setBackground(COLOR_PANEL);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setBackground(COLOR_BACKGROUND);
        JButton csvButton = new JButton("Generar CSV");
        csvButton.setBackground(COLOR_HIGHLIGHT);
        csvButton.setForeground(COLOR_TEXT);
        csvButton.addActionListener(e -> exportCsv());
        JButton closeButton = new JButton("Cerrar");
        closeButton.setBackground(COLOR_DANGER);
        closeButton.setForeground(COLOR_BACKGROUND);
        closeButton.addActionListener(e -> dispose());
        panel.add(csvButton);
        panel.add(closeButton);
        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(COLOR_HEADER);
        footer.setBorder(new EmptyBorder(6, 12, 6, 12));

        JLabel status = createLabel("Items: 0");
        footer.add(status, BorderLayout.WEST);

        JLabel hint = createMutedLabel("Aplique filtros y genere el CSV");
        hint.setBorder(new EmptyBorder(0, 12, 0, 0));
        footer.add(hint, BorderLayout.CENTER);

        JLabel time = createMutedLabel("--:--");
        time.setHorizontalAlignment(SwingConstants.RIGHT);
        footer.add(time, BorderLayout.EAST);

        footer.putClientProperty("statusLabel", status);
        footer.putClientProperty("timeLabel", time);
        return footer;
    }

    private JSpinner createDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy HH:mm");
        spinner.setEditor(editor);
        editor.getTextField().setBackground(COLOR_PANEL);
        editor.getTextField().setForeground(COLOR_TEXT);
        editor.getTextField().setCaretColor(COLOR_TEXT);
        spinner.setBackground(COLOR_PANEL);
        return spinner;
    }

    private void setDefaultDateRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(7);
        fromSpinner.setValue(java.util.Date.from(start.atZone(ZoneId.systemDefault()).toInstant()));
        toSpinner.setValue(java.util.Date.from(now.atZone(ZoneId.systemDefault()).toInstant()));
    }

    private void setShiftDateRange() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(today, LocalTime.of(6, 0));
        LocalDateTime end = LocalDateTime.now();
        fromSpinner.setValue(java.util.Date.from(start.atZone(ZoneId.systemDefault()).toInstant()));
        toSpinner.setValue(java.util.Date.from(end.atZone(ZoneId.systemDefault()).toInstant()));
    }

    private void setYearDateRange() {
        LocalDate currentDate = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(LocalDate.of(currentDate.getYear(), 1, 1), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(LocalDate.of(currentDate.getYear(), 12, 31), LocalTime.MAX);
        fromSpinner.setValue(java.util.Date.from(start.atZone(ZoneId.systemDefault()).toInstant()));
        toSpinner.setValue(java.util.Date.from(end.atZone(ZoneId.systemDefault()).toInstant()));
    }

    private void updateSoldTable() {
        if (productRepository == null) {
            soldTableModel.setRowCount(0);
            currentRows.clear();
            statusLabel.setText("Items: 0");
            return;
        }

        try {
            updateTableHeaders();
            StatsFilter filter = buildFilterFromControls();
            List<StatsSaleRow> rows = productRepository.getStatsSalesRows(filter);
            List<StatsSaleRow> filteredByGroup = applyGroupFilters(rows);
            List<AggregatedRow> aggregatedRows = aggregateRows(filteredByGroup);
            sortAggregatedRows(aggregatedRows);

            soldTableModel.setRowCount(0);
            currentRows.clear();
            currentRows.addAll(aggregatedRows);

            int totalQuantity = 0;
            for (AggregatedRow row : aggregatedRows) {
                soldTableModel.addRow(new Object[] { row.label, row.quantity });
                totalQuantity += row.quantity;
            }

            statusLabel.setText("Items: " + aggregatedRows.size() + " | Unidades: " + totalQuantity);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            soldTableModel.setRowCount(0);
            currentRows.clear();
            statusLabel.setText("Items: 0");
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error cargando reporte", JOptionPane.ERROR_MESSAGE);
        }
    }

    private StatsFilter buildFilterFromControls() {
        LocalDateTime from = toLocalDateTime((java.util.Date) fromSpinner.getValue());
        LocalDateTime to = toLocalDateTime((java.util.Date) toSpinner.getValue());
        if (from.isAfter(to)) {
            LocalDateTime tmp = from;
            from = to;
            to = tmp;
        }

        String area = selectedText(areaCombo);
        String serie = selectedText(serieCombo);
        String servicio = selectedText(servicioCombo);
        String mesa = selectedText(mesaCombo);
        boolean onlyPaid = "Solo pagadas".equals(selectedText(cuentasCombo));
        String productsMode = selectedText(productosCombo);
        String clasificacion = selectedText(clasificacionCombo);

        return new StatsFilter(from, to, area, serie, servicio, mesa, onlyPaid, productsMode, clasificacion);
    }

    private List<StatsSaleRow> applyGroupFilters(List<StatsSaleRow> rows) {
        Map<String, Boolean> includeByGroupKey = selectedGroupInclusions();
        String comboGroup = selectedText(groupCombo);

        List<StatsSaleRow> filtered = new ArrayList<>();
        for (StatsSaleRow row : rows) {
            String groupKey = classifyGroupKey(row.getProductName());
            boolean includedByTable = includeByGroupKey.getOrDefault(groupKey, true);
            if (!includedByTable) {
                continue;
            }

            if (!"Todos".equalsIgnoreCase(comboGroup)) {
                if ("Tacos".equalsIgnoreCase(comboGroup) && !"01".equals(groupKey)) {
                    continue;
                }
                if ("Bebidas".equalsIgnoreCase(comboGroup) && !"02".equals(groupKey)) {
                    continue;
                }
                if ("Extras".equalsIgnoreCase(comboGroup) && !"03".equals(groupKey)) {
                    continue;
                }
                if ("Otros".equalsIgnoreCase(comboGroup) && !"04".equals(groupKey)) {
                    continue;
                }
            }

            filtered.add(row);
        }

        return filtered;
    }

    private Map<String, Boolean> selectedGroupInclusions() {
        Map<String, Boolean> includes = new LinkedHashMap<>();
        if (groupTable == null) {
            return includes;
        }

        for (int row = 0; row < groupTable.getRowCount(); row++) {
            Object key = groupTable.getValueAt(row, 0);
            Object include = groupTable.getValueAt(row, 2);
            if (key instanceof String && include instanceof Boolean) {
                includes.put((String) key, (Boolean) include);
            }
        }

        return includes;
    }

    private String classifyGroupKey(String productName) {
        String value = productName == null ? "" : productName.toLowerCase();
        if (value.contains("taco")) {
            return "01";
        }
        if (value.contains("coca") || value.contains("agua") || value.contains("refresco") || value.contains("jugo")
                || value.contains("cafe") || value.contains("ponche")) {
            return "02";
        }
        if (value.contains("extra") || value.contains("aderezo") || value.contains("salsa")) {
            return "03";
        }
        return "04";
    }

    private List<AggregatedRow> aggregateRows(List<StatsSaleRow> rows) {
        boolean groupProducts = agruparProductosCheck != null && agruparProductosCheck.isSelected();
        boolean withAverage = precioPromedioCheck != null && precioPromedioCheck.isSelected();
        int reportMode = reportList == null ? 0 : reportList.getSelectedIndex();

        Map<String, AggregationBucket> aggregate = new LinkedHashMap<>();

        for (StatsSaleRow row : rows) {
            String key;
            if (!groupProducts) {
                key = "Pedido #" + row.getOrderId() + " - " + row.getProductName();
            } else if (reportMode == 1) {
                key = "Mesero " + row.getEmployeeId();
            } else {
                key = row.getProductName();
            }

            AggregationBucket current = aggregate.get(key);
            if (current == null) {
                current = new AggregationBucket();
                aggregate.put(key, current);
            }
            current.totalQuantity += row.getQuantity();
            current.maxQuantity = Math.max(current.maxQuantity, row.getQuantity());
            BigDecimal unitPrice = row.getUnitPrice() == null ? BigDecimal.ZERO : row.getUnitPrice();
            current.totalAmount = current.totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(row.getQuantity())));
            current.orderIds.add(row.getOrderId());
        }

        List<AggregatedRow> result = new ArrayList<>();
        for (Map.Entry<String, AggregationBucket> entry : aggregate.entrySet()) {
            String label = entry.getKey();
            AggregationBucket bucket = entry.getValue();
            int quantity = bucket.totalQuantity;

            if (reportMode == 2) {
                quantity = bucket.maxQuantity;
                if (withAverage) {
                    int orders = Math.max(1, bucket.orderIds.size());
                    double avg = (double) bucket.totalQuantity / (double) orders;
                    label = label + " (PROM " + String.format(java.util.Locale.US, "%.2f", avg) + ")";
                }
            }

            result.add(new AggregatedRow(label, quantity));
        }

        return result;
    }

    private void sortAggregatedRows(List<AggregatedRow> rows) {
        String order = selectedText(ordenCombo);
        if ("Cantidad".equalsIgnoreCase(order)) {
            rows.sort((a, b) -> Integer.compare(b.quantity, a.quantity));
            return;
        }
        rows.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    private String selectedText(JComboBox<String> combo) {
        if (combo == null || combo.getSelectedItem() == null) {
            return "";
        }
        return String.valueOf(combo.getSelectedItem());
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(COLOR_TEXT);
        return label;
    }

    private JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(COLOR_MUTED);
        return label;
    }

    private void updateTableHeaders() {
        String[] headers = buildColumnHeaders();
        soldTableModel.setColumnIdentifiers(headers);
    }

    private String[] buildColumnHeaders() {
        boolean groupProducts = agruparProductosCheck != null && agruparProductosCheck.isSelected();
        int reportMode = reportList == null ? 0 : reportList.getSelectedIndex();

        String labelHeader;
        if (!groupProducts) {
            labelHeader = "PEDIDO / PRODUCTO";
        } else if (reportMode == 1) {
            labelHeader = "MESERO / REPARTIDOR";
        } else {
            labelHeader = "PRODUCTO";
        }

        String quantityHeader = reportMode == 2 ? "CANTIDAD MAX" : "CANTIDAD";
        return new String[] { labelHeader, quantityHeader };
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("reporte_ventas.csv"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            String[] headers = buildColumnHeaders();
            writer.write(headers[0] + "," + headers[1]);
            writer.newLine();

            int total = 0;
            for (AggregatedRow row : currentRows) {
                writer.write(escapeCsv(row.label) + "," + row.quantity);
                writer.newLine();
                total += row.quantity;
            }

            if (imprimirResumenCheck != null && imprimirResumenCheck.isSelected()) {
                writer.newLine();
                writer.write("TOTAL," + total);
                writer.newLine();
            }

            JOptionPane.showMessageDialog(this, "CSV generado: " + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el CSV: " + e.getMessage());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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

    private LocalDateTime toLocalDateTime(java.util.Date date) {
        Instant instant = date.toInstant();
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private static class AggregatedRow {
        private final String label;
        private final int quantity;

        private AggregatedRow(String label, int quantity) {
            this.label = label;
            this.quantity = quantity;
        }
    }

    private static class AggregationBucket {
        private int totalQuantity;
        private int maxQuantity;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private final List<Integer> orderIds = new LinkedList<>();
    }
}
