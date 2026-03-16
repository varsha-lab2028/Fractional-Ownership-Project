package edu.iiitd.dbms.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ServerConnector {
// In edu.iiitd.dbms.config.ServerConnector
    private static final String URL = "jdbc:mysql://localhost:3306/fractional_ownership_db";
    private static final String USER = "root";
    private static final String PASS = "YES"; // Case-sensitive, exactly as you typed in terminal

    public static Connection DBConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
