package edu.iiitd.dbms.auth;

import edu.iiitd.dbms.data_access.AuthDAO;
import edu.iiitd.dbms.domain.AuthClass;

public class AuthenticationService {
    private final AuthDAO authDAO;

    public AuthenticationService() {
        this.authDAO = new AuthDAO();
    }

    public AuthClass login(String email, String rawPassword) throws Exception {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new Exception("Email and password cannot be empty.");
        }

        AuthClass user = authDAO.findByEmail(email);

        if (user == null) {
            throw new Exception("User not found.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getAuthStatus())) {
            throw new Exception("Account is not active.");
        }

        boolean valid = PasswordHasher.verifyHash(rawPassword, user.getPasswordHash());

        if (!valid) {
            throw new Exception("Invalid password.");
        }

        authDAO.updateLastLogin(user.getAuthId());
        LoginManager.login(user);
        return user;
    }

    public void logout() {
        LoginManager.logout();
    }
}
