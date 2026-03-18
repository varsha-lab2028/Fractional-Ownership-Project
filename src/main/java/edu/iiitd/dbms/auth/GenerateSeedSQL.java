package edu.iiitd.dbms.auth;

import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Run this class ONCE to generate seed_passwords_generated.sql
 * with real bcrypt hashes for all users.
 *
 * Then run the generated SQL in your TiDB/MySQL console.
 *
 * This approach does NOT need a DB connection.
 */
public class GenerateSeedSQL {

    public static void main(String[] args) throws Exception {
        Map<String, String> credentials = new LinkedHashMap<>();
        // Investors
        credentials.put("aman@gmail.com",    "aman123");
        credentials.put("riya@gmail.com",    "riya123");
        credentials.put("karan@gmail.com",   "karan123");
        credentials.put("sneha@gmail.com",   "sneha123");
        credentials.put("arjun@gmail.com",   "arjun123");
        credentials.put("meera@gmail.com",   "meera123");
        credentials.put("rahul@gmail.com",   "rahul123");
        credentials.put("tanya@gmail.com",   "tanya123");
        credentials.put("dev@gmail.com",     "dev123");
        credentials.put("ishita@gmail.com",  "ishita123");
        credentials.put("harsh@gmail.com",   "harsh123");
        credentials.put("neeraj@gmail.com",  "neeraj123");
        credentials.put("simran@gmail.com",  "simran123");
        credentials.put("rohit@gmail.com",   "rohit123");
        credentials.put("pooja@gmail.com",   "pooja123");
        // Admins
        credentials.put("ananya@platform.com", "ananya123");
        credentials.put("raghav@platform.com", "raghav123");
        credentials.put("priya@platform.com",  "priya123");
        credentials.put("vikram@platform.com", "vikram123");
        credentials.put("neha@platform.com",   "neha123");
        credentials.put("aditya@platform.com", "aditya123");
        credentials.put("sonal@platform.com",  "sonal123");
        credentials.put("kunal@platform.com",  "kunal123");
        credentials.put("ira@platform.com",    "ira123");
        credentials.put("sameer@platform.com", "sameer123");
        credentials.put("divya@platform.com",  "divya123");
        credentials.put("manav@platform.com",  "manav123");
        credentials.put("aisha@platform.com",  "aisha123");
        credentials.put("nikhil@platform.com", "nikhil123");
        credentials.put("ritu@platform.com",   "ritu123");

        StringBuilder sql = new StringBuilder();
        sql.append("USE fractional_ownership_db;\n\n");
        sql.append("-- Generated bcrypt hashes (cost=10). Run this in TiDB/MySQL after auth.sql.\n\n");

        for (Map.Entry<String, String> e : credentials.entrySet()) {
            String hash = PasswordHasher.hash(e.getValue());
            sql.append(String.format(
                "UPDATE user_auth SET password_hash = '%s' WHERE email = '%s';\n",
                hash, e.getKey()
            ));
        }

        sql.append("\nSELECT 'Passwords seeded successfully.' AS status;\n");
        sql.append("SELECT email, user_type, LEFT(password_hash, 7) AS hash_prefix FROM user_auth ORDER BY user_type, email;\n");

        // Write to file
        String outPath = "seed_passwords_generated.sql";
        try (FileWriter fw = new FileWriter(outPath)) {
            fw.write(sql.toString());
        }
        System.out.println("✓ Generated: " + outPath);
        System.out.println("  Run this SQL file in your TiDB/MySQL console.");
        System.out.println("  All " + credentials.size() + " users will be updated.");
    }
}
