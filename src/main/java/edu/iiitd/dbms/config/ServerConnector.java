package edu.iiitd.dbms.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ServerConnector {
// In edu.iiitd.dbms.config.ServerConnector
    private static final String URL = "jdbc:mysql://loaclhost:3306/test"; // Update with your DB name and port
    private static final String USER = "root";
    private static final String PASS = "YES"; // Case-sensitive, exactly as you typed in terminal

    public static Connection DBConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
