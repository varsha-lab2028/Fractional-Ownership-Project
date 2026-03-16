package edu.iiitd.dbms.auth;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    public static String hash(String raw_password) {
        return BCrypt.hashpw(raw_password, BCrypt.gensalt(12)); // Salted for security [cite: 14]
    }
    public static boolean verifyHash(String raw_password, String hash){
        if (hash == null || hash.isBlank()) return false;
        try {
            return BCrypt.checkpw(raw_password, hash); // Compares raw to hashed [cite: 14]
        } catch (Exception e) {
            return false;
        }
    }
}