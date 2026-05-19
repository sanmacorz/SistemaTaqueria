package controller;

import model.ProductRepository;
import view.ButtonsPanel;
import view.InventoryWindow;
import view.KitchenWindow;
import view.MainWindow;
import view.SellWindow;
import view.StatsWindow;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class Controller implements ActionListener {
    private MainWindow mainWindow;
    private final ProductRepository tacoRepository;
    private SellWindow sellWindow;
    private KitchenWindow kitchenWindow;
    private InventoryWindow inventoryWindow;
    private StatsWindow statsWindow;

    public Controller(MainWindow mainWindow, ProductRepository tacoRepository) {
        this.mainWindow = mainWindow;
        this.tacoRepository = tacoRepository;
        this.mainWindow.buttonsPanel.registerController(this);
    }

    public void actionPerformed(ActionEvent ae) {
        String command = ae.getActionCommand();
        if (isAnyWindowOpen() && !isSameWindowCommand(command)) {
            return;
        }
        if (ButtonsPanel.ACTION_EXIT.equals(command)) {
            mainWindow.dispose();
            System.exit(0);
        } else if (ButtonsPanel.ACTION_SELL.equals(command)) {
            if (sellWindow == null || !sellWindow.isDisplayable()) {
                sellWindow = new SellWindow(loadItems(), tacoRepository);
                sellWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        sellWindow = null;
                    }
                });
            } else {
                sellWindow.toFront();
                sellWindow.requestFocus();
            }
        } else if (ButtonsPanel.ACTION_KITCHEN.equals(command)) {
            if (kitchenWindow == null || !kitchenWindow.isDisplayable()) {
                kitchenWindow = new KitchenWindow(tacoRepository);
                kitchenWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        kitchenWindow = null;
                    }
                });
            } else {
                kitchenWindow.toFront();
                kitchenWindow.requestFocus();
            }
        } else if (ButtonsPanel.ACTION_INVENTORY.equals(command)) {
            if (inventoryWindow == null || !inventoryWindow.isDisplayable()) {
                inventoryWindow = new InventoryWindow(tacoRepository);
                inventoryWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        inventoryWindow = null;
                    }
                });
            } else {
                inventoryWindow.toFront();
                inventoryWindow.requestFocus();
            }
        } else if (ButtonsPanel.ACTION_STATS.equals(command)) {
            if (statsWindow == null || !statsWindow.isDisplayable()) {
                statsWindow = new StatsWindow(tacoRepository);
                statsWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        statsWindow = null;
                    }
                });
            } else {
                statsWindow.toFront();
                statsWindow.requestFocus();
            }
        }
    }

    private boolean isAnyWindowOpen() {
        return isWindowOpen(sellWindow)
                || isWindowOpen(kitchenWindow)
                || isWindowOpen(inventoryWindow)
                || isWindowOpen(statsWindow);
    }

    private boolean isSameWindowCommand(String command) {
        return (ButtonsPanel.ACTION_SELL.equals(command) && isWindowOpen(sellWindow))
                || (ButtonsPanel.ACTION_KITCHEN.equals(command) && isWindowOpen(kitchenWindow))
                || (ButtonsPanel.ACTION_INVENTORY.equals(command) && isWindowOpen(inventoryWindow))
                || (ButtonsPanel.ACTION_STATS.equals(command) && isWindowOpen(statsWindow));
    }

    private boolean isWindowOpen(javax.swing.JFrame window) {
        return window != null && window.isDisplayable();
    }

    public List<String> loadItems() {
        return tacoRepository.getItemNames();
    }
}
