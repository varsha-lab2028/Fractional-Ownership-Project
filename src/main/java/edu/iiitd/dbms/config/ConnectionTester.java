package edu.iiitd.dbms.config;

import edu.iiitd.dbms.config.DBConnection;
import java.sql.Connection;

public class ConnectionTester {
    public static void main(String[] args){
        try (Connection con = DBConnection.getConnection()){
            System.out.println("DB Connection is successful");
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
