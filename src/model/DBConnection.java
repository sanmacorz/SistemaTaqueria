package model;

import io.github.cdimascio.dotenv.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final Dotenv DOTENV = Dotenv.load();
    private final Connection db;

    public DBConnection() {
        try {
            String url = requireEnv("DB_URL");
            String user = requireEnv("DB_USER");
            String password = requireEnv("DB_PASSWORD");

            db = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating DB connection", e);
        }
    }

    private static String requireEnv(String key) {
        String value = DOTENV.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required secret: " + key);
        }
        return value;
    }

    public Connection getConnection() {
        return db;
    }

    public void close() {
        try {
            if (db != null && !db.isClosed())
                db.close();
        } catch (SQLException e) {
        }
    }
}
