package edu.iiitd.dbms.config;

/**
 * Simple in-memory session holder.
 * Set once at login, read everywhere else (UIs, services).
 * Eliminates the need for every UI to hardcode investorId = 1.
 */
public class SessionManager {

    private static int    currentUserId   = -1;
    private static String currentUserName = "Unknown";
    private static String currentUserType = "";   // "INVESTOR" or "ADMIN"

    /** Called by loginUI after a successful authentication. */
    public static void login(int linkedId, String name, String userType) {
        currentUserId   = linkedId;
        currentUserName = name;
        currentUserType = userType;
    }

    /** Called when the user logs out or the app exits. */
    public static void logout() {
        currentUserId   = -1;
        currentUserName = "Unknown";
        currentUserType = "";
    }

    public static int    getUserId()   { return currentUserId;   }
    public static String getUserName() { return currentUserName; }
    public static String getUserType() { return currentUserType; }
    public static boolean isLoggedIn() { return currentUserId > 0; }
}
