package edu.iiitd.dbms.auth;

import edu.iiitd.dbms.config.ServerConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;

public class SeedUserPasswords {
    public static void main(String[] args) {
        Map<String, String> investorPasswords = new LinkedHashMap<>();
        investorPasswords.put("aman@gmail.com",   "aman123");
        investorPasswords.put("riya@gmail.com",   "riya123");
        investorPasswords.put("karan@gmail.com",  "karan123");
        investorPasswords.put("sneha@gmail.com",  "sneha123");
        investorPasswords.put("arjun@gmail.com",  "arjun123");
        investorPasswords.put("meera@gmail.com",  "meera123");
        investorPasswords.put("rahul@gmail.com",  "rahul123");
        investorPasswords.put("tanya@gmail.com",  "tanya123");
        investorPasswords.put("dev@gmail.com",    "dev123");
        investorPasswords.put("ishita@gmail.com", "ishita123");
        investorPasswords.put("harsh@gmail.com",  "harsh123");
        investorPasswords.put("neeraj@gmail.com", "neeraj123");
        investorPasswords.put("simran@gmail.com", "simran123");
        investorPasswords.put("rohit@gmail.com",  "rohit123");
        investorPasswords.put("pooja@gmail.com",  "pooja123");

        // admin email → raw password
        Map<String, String> adminPasswords = new LinkedHashMap<>();
        adminPasswords.put("ananya@platform.com", "ananya123");
        adminPasswords.put("raghav@platform.com", "raghav123");
        adminPasswords.put("priya@platform.com",  "priya123");
        adminPasswords.put("vikram@platform.com", "vikram123");
        adminPasswords.put("neha@platform.com",   "neha123");
        adminPasswords.put("aditya@platform.com", "aditya123");
        adminPasswords.put("sonal@platform.com",  "sonal123");
        adminPasswords.put("kunal@platform.com",  "kunal123");
        adminPasswords.put("ira@platform.com",    "ira123");
        adminPasswords.put("sameer@platform.com", "sameer123");
        adminPasswords.put("divya@platform.com",  "divya123");
        adminPasswords.put("manav@platform.com",  "manav123");
        adminPasswords.put("aisha@platform.com",  "aisha123");
        adminPasswords.put("nikhil@platform.com", "nikhil123");
        adminPasswords.put("ritu@platform.com",   "ritu123");

        // UPDATE user_auth directly — this is the only table with password_hash
        String updateSql = "UPDATE user_auth SET password_hash = ? WHERE email = ?";

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {

            conn.setAutoCommit(false);
            int updated = 0;

            for (Map.Entry<String, String> entry : investorPasswords.entrySet()) {
                ps.setString(1, PasswordHasher.hash(entry.getValue()));
                ps.setString(2, entry.getKey());
                updated += ps.executeUpdate();
            }

            for (Map.Entry<String, String> entry : adminPasswords.entrySet()) {
                ps.setString(1, PasswordHasher.hash(entry.getValue()));
                ps.setString(2, entry.getKey());
                updated += ps.executeUpdate();
            }

            conn.commit();
            System.out.println("✓ Successfully updated " + updated + " password hashes in user_auth.");

        } catch (Exception e) {
            System.err.println("✗ Seeding failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
