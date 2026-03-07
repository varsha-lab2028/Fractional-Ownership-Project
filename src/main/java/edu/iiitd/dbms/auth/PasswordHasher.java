package edu.iiitd.dbms.auth;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    public static String hash(String raw_password) {
        return BCrypt.hashpw(raw_password, BCrypt.gensalt(15));
    }
    public static boolean verifyHash(String raw_password, String hash){
        if (hash == null || hash.isBlank()) {
            //checking just to be safe
            return false;
        }
        try {
            return BCrypt.checkpw(raw_password, hash);
        } catch (Exception e) {
            return false;
        }
    }
    private PasswordHasher() {}
}
