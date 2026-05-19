package view;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Image;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.Font;

public class MainWindow extends JFrame {
    public ButtonsPanel buttonsPanel;
    private static final Color COLOR_BACKGROUND = Color.decode("#F8F9FA");
    private static final Color COLOR_PANEL = Color.decode("#FFFFFF");
    private static final Color COLOR_TEXT = Color.decode("#1A1A1A");

    public MainWindow() {
        this.setTitle("Menú Principal");
        this.setSize(1920, 1080);
        this.setResizable(true);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setLayout(null);
        this.getContentPane().setBackground(COLOR_BACKGROUND);

        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(COLOR_PANEL);
        menuBar.setForeground(COLOR_TEXT);
        JMenu fileMenu = new JMenu("Archivo");
        fileMenu.setBackground(COLOR_PANEL);
        fileMenu.setForeground(COLOR_TEXT);
        menuBar.add(fileMenu);
        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.setBackground(COLOR_PANEL);
        exitItem.setForeground(COLOR_TEXT);
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        JMenu editMenu = new JMenu("Editar");
        editMenu.setBackground(COLOR_PANEL);
        editMenu.setForeground(COLOR_TEXT);
        menuBar.add(editMenu);
        JMenu viewMenu = new JMenu("Ver");
        viewMenu.setBackground(COLOR_PANEL);
        viewMenu.setForeground(COLOR_TEXT);
        menuBar.add(viewMenu);
        JMenu helpMenu = new JMenu("Ayuda");
        helpMenu.setBackground(COLOR_PANEL);
        helpMenu.setForeground(COLOR_TEXT);
        JMenuItem aboutItem = new JMenuItem("Acerca de...");
        aboutItem.setBackground(COLOR_PANEL);
        aboutItem.setForeground(COLOR_TEXT);
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        this.setJMenuBar(menuBar);

        ImageIcon img = new ImageIcon("src/imgs/taco.png");
        this.setIconImage(img.getImage());

        ImageIcon logoIcon = new ImageIcon("src/imgs/logotacos.png");
        Image logoImage = logoIcon.getImage().getScaledInstance(630, 400, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(logoImage));
        logoLabel.setBounds(60, 80, 630, 400);
        this.add(logoLabel);

        buttonsPanel = new ButtonsPanel();
        buttonsPanel.setBounds(0, 845, 1920, 128);
        this.add(buttonsPanel);

        buttonsPanel.setSellIcon(ButtonsPanel.loadScaledIcon("src/imgs/seller.png"));
        buttonsPanel.setKitchenIcon(ButtonsPanel.loadScaledIcon("src/imgs/kitchen.png"));
        buttonsPanel.setInventoryIcon(ButtonsPanel.loadScaledIcon("src/imgs/inventory.png"));
        buttonsPanel.setStatsIcon(ButtonsPanel.loadScaledIcon("src/imgs/dashboard.png"));
        buttonsPanel.setExitIcon(ButtonsPanel.loadScaledIcon("src/imgs/exit.png"));

        ImageIcon timeIcon = new ImageIcon("src/imgs/clock.png");
        Image timeImage = timeIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        JLabel lbTimeImg = new JLabel(new ImageIcon(timeImage));
        lbTimeImg.setBounds(1530, 810, 32, 32);
        this.add(lbTimeImg);

        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a");
        JLabel lbTime = new JLabel();
        lbTime.setForeground(COLOR_TEXT);
        lbTime.setFont(new Font("SansSerif", Font.BOLD, 24));
        lbTime.setBounds(1570, 806, 320, 40);
        this.add(lbTime);
        Timer clockTimer = new Timer(1000, e -> lbTime.setText(LocalDateTime.now().format(timeFormat)));
        clockTimer.setInitialDelay(0);
        clockTimer.start();

        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }
}
