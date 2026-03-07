package edu.iiitd.dbms.auth;

import edu.iiitd.dbms.domain.AuthClass;

public class LoginManager {
    private static AuthClass currentUser;

    public static void login(AuthClass user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static AuthClass getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
