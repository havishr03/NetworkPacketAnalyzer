package com.network.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class to handle JDBC connections to the PostgreSQL database.
 */
public class DatabaseUtil {
    
    // We use environment variables so this connects locally on Ubuntu or remotely on Render.com
    private static final String URL = System.getenv("JDBC_URL") != null ? System.getenv("JDBC_URL") : "jdbc:postgresql://localhost:5432/packetdb";
    private static final String USER = System.getenv("JDBC_USER") != null ? System.getenv("JDBC_USER") : "postgres";
    private static final String PASS = System.getenv("JDBC_PASSWORD") != null ? System.getenv("JDBC_PASSWORD") : "postgres";

    static {
        try {
            // Load PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");
            initDatabase();
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load PostgreSQL Driver: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private static void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Initialize the database table if it does not exist
            String sql = "CREATE TABLE IF NOT EXISTS packets (" +
                         "id SERIAL PRIMARY KEY, " +
                         "packet_id VARCHAR(50), " +
                         "packet_type VARCHAR(20), " +
                         "description TEXT" +
                         ")";
            stmt.execute(sql);
            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing database (check if PostgreSQL is running): " + e.getMessage());
        }
    }
}
