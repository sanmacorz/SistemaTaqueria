package main;

import controller.Controller;
import model.DBConnection;
import model.ProductRepository;
import view.MainWindow;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DBConnection dbConnection = new DBConnection();
            ProductRepository tacoRepository = new ProductRepository(dbConnection);
            MainWindow mainWindow = new MainWindow();
            new Controller(mainWindow, tacoRepository);
        });
    }
}
