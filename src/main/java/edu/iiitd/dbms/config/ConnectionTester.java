package edu.iiitd.dbms.config;

import edu.iiitd.dbms.config.DBConnection;
import java.sql.Connection;

public class ConnectionTester {
    public static void main(String[] args){
        try {
            Connection connection = DBConnection.getConnection();
            if (connection!=null) {
                //System.out.println("DB Connection is successful");
                System.out.println("Connected to Supabase successfully");
                System.out.println("Database: " + connection.getMetaData().getURL());
                connection.close();
            }
        } catch (Exception e){
            System.out.println("Connection failed");
            System.out.println("Error: " + e.getMessage());
        }
    }
}
