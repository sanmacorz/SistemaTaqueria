package view;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class ButtonsPanel extends JPanel {
    public static final String ACTION_SELL = "sell";
    public static final String ACTION_KITCHEN = "kitchen";
    public static final String ACTION_INVENTORY = "inventory";
    public static final String ACTION_STATS = "stats";
    public static final String ACTION_EXIT = "exit";

    private JButton btnSell, btnKitchen, btnInventory, btnStats, btnExit;
    private static final Dimension BUTTON_SIZE = new Dimension(140, 140);
    private static final int ICON_PADDING = 60;
    private static final Map<String, ImageIcon> ICON_CACHE = new HashMap<>();
    private static final Color COLOR_BACKGROUND = Color.decode("#F8F9FA");
    private static final Color COLOR_TEXT = Color.decode("#1A1A1A");
    private static final Color COLOR_HIGHLIGHT = Color.decode("#F0C808");
    private static final Color COLOR_DANGER = Color.decode("#D81E1E");
    private static final Color COLOR_BORDER = Color.decode("#E0E0E0");

    public ButtonsPanel() {
        // Usamos BorderLayout para que el panel de botones se pegue abajo como en la
        // imagen
        this.setLayout(new BorderLayout());
        this.setBackground(COLOR_BACKGROUND);

        // Creamos un sub-panel para los botones con GridLayout (1 fila, 5 columnas)
        JPanel gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(1, 5, 5, 5)); // (filas, columnas, espacioH, espacioV)
        gridPanel.setBackground(COLOR_BACKGROUND);

        // Inicializamos los botones con tu estilo
        btnSell = createSquareButton("VENTAS", COLOR_HIGHLIGHT, COLOR_TEXT);
        btnKitchen = createSquareButton("COCINA", COLOR_HIGHLIGHT, COLOR_TEXT);
        btnInventory = createSquareButton("INVENTARIO", COLOR_HIGHLIGHT, COLOR_TEXT);
        btnStats = createSquareButton("REPORTES", COLOR_HIGHLIGHT, COLOR_TEXT);
        btnExit = createSquareButton("SALIR", COLOR_DANGER, COLOR_BACKGROUND);

        // Agregamos al grid
        gridPanel.add(btnSell);
        gridPanel.add(btnKitchen);
        gridPanel.add(btnInventory);
        gridPanel.add(btnStats);
        gridPanel.add(btnExit);

        // Agregamos el grid al sur (parte inferior) del panel principal
        this.add(gridPanel, BorderLayout.SOUTH);
    }

    public void registerController(ActionListener listener) {
        btnSell.setActionCommand(ACTION_SELL);
        btnKitchen.setActionCommand(ACTION_KITCHEN);
        btnInventory.setActionCommand(ACTION_INVENTORY);
        btnStats.setActionCommand(ACTION_STATS);
        btnExit.setActionCommand(ACTION_EXIT);

        btnSell.addActionListener(listener);
        btnKitchen.addActionListener(listener);
        btnInventory.addActionListener(listener);
        btnStats.addActionListener(listener);
        btnExit.addActionListener(listener);
    }

    public void setSellIcon(ImageIcon icon) {
        setButtonIcon(btnSell, icon);
    }

    public void setKitchenIcon(ImageIcon icon) {
        setButtonIcon(btnKitchen, icon);
    }

    public void setInventoryIcon(ImageIcon icon) {
        setButtonIcon(btnInventory, icon);
    }

    public void setStatsIcon(ImageIcon icon) {
        setButtonIcon(btnStats, icon);
    }

    public void setExitIcon(ImageIcon icon) {
        setButtonIcon(btnExit, icon);
    }

    public static ImageIcon loadScaledIcon(String path) {
        ImageIcon cached = ICON_CACHE.get(path);
        if (cached != null) {
            return cached;
        }

        try {
            Image image = ImageIO.read(new File(path));
            if (image == null) {
                return null;
            }

            int iconSize = Math.min(BUTTON_SIZE.width, BUTTON_SIZE.height) - ICON_PADDING;
            Image scaled = image.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaled);
            ICON_CACHE.put(path, icon);
            return icon;
        } catch (IOException e) {
            return null;
        }
    }

    // Método auxiliar para crear botones cuadrados con tu paleta
    private JButton createSquareButton(String text, Color background, Color foreground) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Verdana", Font.BOLD, 12));
        btn.setBackground(background);
        btn.setForeground(foreground);

        // Estilo cuadrado y alineación
        btn.setPreferredSize(BUTTON_SIZE);
        btn.setMinimumSize(BUTTON_SIZE);
        btn.setMaximumSize(BUTTON_SIZE);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setVerticalAlignment(SwingConstants.CENTER);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setIconTextGap(8);
        btn.setMargin(new Insets(8, 8, 8, 8));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 2));
        btn.setOpaque(true);

        return btn;
    }

    private void setButtonIcon(JButton button, ImageIcon icon) {
        if (icon == null) {
            button.setIcon(null);
            return;
        }

        int iconSize = Math.min(BUTTON_SIZE.width, BUTTON_SIZE.height) - ICON_PADDING;
        if (icon.getIconWidth() <= iconSize && icon.getIconHeight() <= iconSize) {
            button.setIcon(icon);
            return;
        }

        Image scaled = icon.getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
        button.setIcon(new ImageIcon(scaled));
    }
}