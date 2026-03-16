package edu.iiitd.dbms.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ServerConnector {
// In edu.iiitd.dbms.config.ServerConnector
    private static final String URL = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/test?sslMode=VERIFY_IDENTITY";
    private static final String USER = "TJ3oPDmKLZBZTNr.root";
    private static final String PASS = "34hbnVC37EpRVUlf"; // Case-sensitive, exactly as you typed in terminal

    public static Connection DBConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
