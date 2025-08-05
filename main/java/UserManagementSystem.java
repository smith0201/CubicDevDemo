package main.java;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Single-class UserManagementSystem
 * Demonstrates CRUD, authentication, password reset, admin tools, and reporting.
 * Contains intentional flaws for Cubic.dev to detect in review.
 */
public class UserManagementSystem {

    // In-memory stores
    private Map<String, User> users = new HashMap<>();
    private Map<String, String> sessions = new HashMap<>(); // token -> username
    private Map<String, String> passwordResetOtps = new HashMap<>(); // username -> OTP
    private List<String> auditLog = new ArrayList<>();

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        UserManagementSystem ums = new UserManagementSystem();

        // Seed users
        ums.createUser("alice", "pass123", "ADMIN");
        ums.createUser("bob", "welcome", "USER");
        ums.createUser("charlie", "123456", "USER");

        ums.listAllUsers();

        // Authenticate
        String token = ums.authenticate("alice", "pass123");
        ums.isAuthorized(token, "ADMIN");

        // Track failed login & lock account
        ums.trackFailedLogin("bob");
        ums.trackFailedLogin("bob");
        ums.trackFailedLogin("bob"); // Lock after 3 attempts

        // Password reset flow
        ums.requestPasswordReset("charlie");
        ums.resetPassword("charlie", "0000", "newpass"); // wrong OTP
        // Simulate correct OTP
        String otp = ums.passwordResetOtps.get("charlie");
        ums.resetPassword("charlie", otp, "newpass");

        // Role update
        ums.updateUserRole("charlie", "ADMIN");

        // Reporting
        ums.countActiveUsers();
        ums.listLockedAccounts();
        ums.listUsersByRole("ADMIN");

        // Admin bulk actions
        ums.assignRoleToMultiple(Arrays.asList("bob", "charlie"), "MANAGER");
        ums.disableUsers(Collections.singletonList("bob"));

        ums.printLastLoginReport();

        // Logout
        ums.logout(token);

        // Print audit log
        ums.printAuditLog();
    }

    /* ======================== USER CRUD ======================== */
    public void createUser(String username, String password, String role) {
        if (users.containsKey(username)) {
            System.err.println("User already exists: " + username);
            return;
        }
        users.put(username, new User(username, password, role));
        logAction(username, "Created user with role " + role);
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public void updateUserRole(String username, String newRole) {
        User u = getUser(username);
        if (u != null) {
            u.role = newRole;
            logAction(username, "Role updated to " + newRole);
        }
    }

    public void deleteUser(String username) {
        if (users.remove(username) != null) {
            logAction(username, "Deleted user");
        }
    }

    public void listAllUsers() {
        System.out.println("=== All Users ===");
        for (User u : users.values()) {
            System.out.println(u);
        }
        System.out.println();
    }

    /* ======================== AUTHENTICATION ======================== */
    public String authenticate(String username, String password) {
        User u = getUser(username);
        if (u == null) {
            System.err.println("User not found: " + username);
            return null;
        }
        if (u.locked) {
            System.err.println("Account locked: " + username);
            return null;
        }
        if (!u.password.equals(password)) {
            System.err.println("Invalid password for " + username);
            trackFailedLogin(username);
            return null;
        }
        u.failedLogins = 0; // reset failed logins
        u.lastLogin = new Date();
        String token = generateSessionToken(username);
        sessions.put(token, username);
        logAction(username, "Authenticated successfully");
        return token;
    }

    public void logout(String token) {
        String username = sessions.remove(token);
        if (username != null) {
            logAction(username, "Logged out");
        }
    }

    public boolean isAuthorized(String token, String requiredRole) {
        String username = sessions.get(token);
        if (username == null) {
            System.err.println("Invalid session token");
            return false;
        }
        User u = getUser(username);
        boolean authorized = u != null && u.role.equals(requiredRole);
        System.out.println("Authorization check for " + username + ": " + authorized);
        return authorized;
    }

    /* ======================== PASSWORD & SECURITY ======================== */
    public void requestPasswordReset(String username) {
        if (!users.containsKey(username)) {
            System.err.println("User not found: " + username);
            return;
        }
        String otp = String.valueOf(new Random().nextInt(9999) + 1000);
        passwordResetOtps.put(username, otp);
        logAction(username, "Requested password reset. OTP=" + otp);
        // In real app: send OTP via email/SMS
    }

    public void resetPassword(String username, String otp, String newPassword) {
        String storedOtp = passwordResetOtps.get(username);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            System.err.println("Invalid OTP for " + username);
            return;
        }
        User u = getUser(username);
        if (u != null) {
            u.password = newPassword;
            logAction(username, "Password reset successfully");
            passwordResetOtps.remove(username);
        }
    }

    public void trackFailedLogin(String username) {
        User u = getUser(username);
        if (u == null) return;
        u.failedLogins++;
        logAction(username, "Failed login attempt #" + u.failedLogins);
        if (u.failedLogins >= 3) {
            lockAccount(username);
        }
    }

    public void lockAccount(String username) {
        User u = getUser(username);
        if (u != null) {
            u.locked = true;
            logAction(username, "Account locked due to failed logins");
        }
    }

    public void unlockAccount(String username) {
        User u = getUser(username);
        if (u != null) {
            u.locked = false;
            u.failedLogins = 0;
            logAction(username, "Account unlocked");
        }
    }

    /* ======================== SESSION MGMT ======================== */
    public String generateSessionToken(String username) {
        return "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public boolean validateSessionToken(String token) {
        return sessions.containsKey(token);
    }

    public void cleanupExpiredSessions() {
        // Not implemented: No expiry logic for demo
        logAction("SYSTEM", "Cleanup expired sessions executed");
    }

    /* ======================== AUDIT & LOGGING ======================== */
    public void logAction(String username, String action) {
        String time = DATE_FMT.format(new Date());
        auditLog.add(time + " - " + username + " - " + action);
    }

    public void printAuditLog() {
        System.out.println("=== Audit Log ===");
        for (String entry : auditLog) {
            System.out.println(entry);
        }
    }

    /* ======================== REPORTING ======================== */
    public int countActiveUsers() {
        int active = (int) users.values().stream().filter(u -> !u.locked).count();
        System.out.println("Active users: " + active);
        return active;
    }

    public void listLockedAccounts() {
        System.out.println("Locked accounts:");
        users.values().stream().filter(u -> u.locked).forEach(u -> System.out.println(u.username));
    }

    public void listUsersByRole(String role) {
        System.out.println("Users with role " + role + ":");
        users.values().stream().filter(u -> u.role.equals(role)).forEach(u -> System.out.println(u.username));
    }

    public void printLastLoginReport() {
        System.out.println("=== Last Login Report ===");
        for (User u : users.values()) {
            String lastLogin = (u.lastLogin != null) ? DATE_FMT.format(u.lastLogin) : "Never";
            System.out.println(u.username + " - " + lastLogin);
        }
    }

    /* ======================== ADMIN TOOLS ======================== */
    public void assignRoleToMultiple(List<String> usernames, String role) {
        for (String user : usernames) {
            updateUserRole(user, role);
        }
    }

    public void disableUsers(List<String> usernames) {
        for (String user : usernames) {
            lockAccount(user);
        }
    }

    /* ======================== INNER CLASS ======================== */
    static class User {
        String username;
        String password; // Plaintext for demo only
        String role;
        int failedLogins = 0;
        boolean locked = false;
        Date lastLogin;

        User(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }

        @Override
        public String toString() {
            return username + " (" + role + ") - Locked: " + locked;
        }
    }
}

