package edu.iiitd.dbms.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ServerConnector {

    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASS;

    static {
        Properties props = new Properties();
        try (InputStream in = ServerConnector.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new RuntimeException(
                    "db.properties not found in src/main/resources. " +
                    "Create it with db.url, db.user, db.password.");
            }
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load db.properties: " + e.getMessage(), e);
        }
        DB_URL  = props.getProperty("db.url");
        DB_USER = props.getProperty("db.user");
        DB_PASS = props.getProperty("db.password");

        if (DB_URL == null || DB_USER == null || DB_PASS == null) {
            throw new RuntimeException(
                "db.properties is missing one or more required keys: db.url, db.user, db.password");
        }
    }

    //Opens and returns a new JDBC connection.
    public static Connection DBConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
}
