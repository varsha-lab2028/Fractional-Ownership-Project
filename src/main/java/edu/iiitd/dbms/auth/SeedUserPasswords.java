package edu.iiitd.dbms.auth;

import edu.iiitd.dbms.config.ServerConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;

public class SeedUserPasswords {
    public static void main(String[] args){
        //investor passwords
        Map<String, String> investorPasswords = new LinkedHashMap<>();
        investorPasswords.put("aman@gmail.com", "aman123");
        investorPasswords.put("riya@gmail.com", "riya123");
        investorPasswords.put("karan@gmail.com", "karan123");
        investorPasswords.put("sneha@gmail.com", "sneha123");
        investorPasswords.put("arjun@gmail.com", "arjun123");
        investorPasswords.put("meera@gmail.com", "meera123");
        investorPasswords.put("rahul@gmail.com", "rahul123");
        investorPasswords.put("tanya@gmail.com", "tanya123");
        investorPasswords.put("dev@gmail.com", "dev123");
        investorPasswords.put("ishita@gmail.com", "ishita123");
        investorPasswords.put("harsh@gmail.com", "harsh123");
        investorPasswords.put("neeraj@gmail.com", "neeraj123");
        investorPasswords.put("simran@gmail.com", "simran123");
        investorPasswords.put("rohit@gmail.com", "rohit123");
        investorPasswords.put("pooja@gmail.com", "pooja123");

        //admin passwords
        Map<String, String> adminPasswords = new LinkedHashMap<>();
        adminPasswords.put("ananya@platform.com", "ananya123");
        adminPasswords.put("raghav@platform.com", "raghav123");
        adminPasswords.put("priya@platform.com", "priya123");
        adminPasswords.put("vikram@platform.com", "vikram123");
        adminPasswords.put("neha@platform.com", "neha123");
        adminPasswords.put("aditya@platform.com", "aditya123");
        adminPasswords.put("sonal@platform.com", "sonal123");
        adminPasswords.put("kunal@platform.com", "kunal123");
        adminPasswords.put("ira@platform.com", "ira123");
        adminPasswords.put("sameer@platform.com", "sameer123");
        adminPasswords.put("divya@platform.com", "divya123");
        adminPasswords.put("manav@platform.com", "manav123");
        adminPasswords.put("aisha@platform.com", "aisha123");
        adminPasswords.put("nikhil@platform.com", "nikhil123");
        adminPasswords.put("ritu@platform.com", "ritu123");

        String investorPasswordQuery = "UPDATE investor SET password_hash = ? WHERE email = ?";
        String adminPasswordQuery = "UPDATE admin SET password_hash = ? WHERE email = ?";

        try(Connection connect = ServerConnector.DBConnection();
            PreparedStatement investorStatement = connect.prepareStatement(investorPasswordQuery);
            PreparedStatement adminStatement = connect.prepareStatement(adminPasswordQuery)){

            connect.setAutoCommit(false);

            for (Map.Entry<String, String> entry : investorPasswords.entrySet()) {
                String email = entry.getKey();
                String rawPassword = entry.getValue();
                String hashedPassword = PasswordHasher.hash(rawPassword);

                investorStatement.setString(1, hashedPassword);
                investorStatement.setString(2, email);
                investorStatement.executeUpdate();

                //System.out.println("Updated investor: " + email);
            }

            for (Map.Entry<String, String> entry : adminPasswords.entrySet()) {
                String email = entry.getKey();
                String rawPassword = entry.getValue();
                String hashedPassword = PasswordHasher.hash(rawPassword);

                adminStatement.setString(1, hashedPassword);
                adminStatement.setString(2, email);
                adminStatement.executeUpdate();

                //System.out.println("Updated admin: " + email);
            }

            connect.commit();
            System.out.println("All password hashes inserted successfully.");
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
