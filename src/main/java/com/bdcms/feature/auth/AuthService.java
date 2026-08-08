package com.bdcms.feature.auth;

import com.bdcms.shared.db.Tx;

import java.util.List;
import java.util.Optional;

/**
 * Login and account management rules (FR-1).
 *
 * <p>The service - not the servlet - decides what a successful login is, so the
 * rule is stated once and is unit-testable.
 */
public class AuthService {

    /**
     * A valid BCrypt hash of a value nobody knows, computed once at class load.
     * Verifying against it costs the same as verifying a real password, which
     * is what keeps an unknown-username login from being measurably faster.
     */
    private static final String DUMMY_HASH =
            Passwords.hash("no-such-account-" + java.util.UUID.randomUUID());

    private final UserDao userDao;

    public AuthService() {
        this(new UserDao());
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Authenticates a staff member.
     *
     * <p>A wrong username and a wrong password produce the same empty result,
     * and the password is verified even when the account is disabled, so that
     * response timing does not reveal which usernames exist.
     */
    public Optional<User> login(String username, String plaintextPassword) {
        if (username == null || plaintextPassword == null) {
            return Optional.empty();
        }
        return Tx.read(conn -> {
            Optional<User> found = userDao.findByUsername(conn, username.trim());
            if (found.isEmpty()) {
                // Spend comparable time on an unknown username as on a known one.
                Passwords.verify(plaintextPassword, DUMMY_HASH);
                return Optional.empty();
            }
            User user = found.get();
            boolean passwordOk = Passwords.verify(plaintextPassword, user.getPasswordHash());
            if (!passwordOk || !user.isActive()) {
                return Optional.empty();
            }
            return Optional.of(user);
        });
    }

    public List<User> listUsers() {
        return Tx.read(userDao::findAll);
    }

    public Optional<User> findById(long userId) {
        return Tx.read(conn -> userDao.findById(conn, userId));
    }

    /** Creates a staff account. Returns empty when the username is taken. */
    public Optional<User> createUser(String username, String fullName, Role role, String plaintextPassword) {
        return Tx.inTransaction(conn -> {
            if (userDao.usernameExists(conn, username)) {
                return Optional.empty();
            }
            User user = new User();
            user.setUsername(username.trim());
            user.setFullName(fullName.trim());
            user.setRole(role);
            user.setActive(true);
            user.setPasswordHash(Passwords.hash(plaintextPassword));
            userDao.insert(conn, user);
            return Optional.of(user);
        });
    }

    public void setActive(long userId, boolean active) {
        Tx.inTransaction(conn -> {
            userDao.setActive(conn, userId, active);
            return null;
        });
    }

    /** Changes a password after confirming the current one. */
    public boolean changePassword(long userId, String currentPassword, String newPassword) {
        return Tx.inTransaction(conn -> {
            Optional<User> found = userDao.findById(conn, userId);
            if (found.isEmpty() || !Passwords.verify(currentPassword, found.get().getPasswordHash())) {
                return false;
            }
            userDao.updatePassword(conn, userId, Passwords.hash(newPassword));
            return true;
        });
    }

    /** Resets a password without knowing the old one. Administrator action. */
    public void resetPassword(long userId, String newPassword) {
        Tx.inTransaction(conn -> {
            userDao.updatePassword(conn, userId, Passwords.hash(newPassword));
            return null;
        });
    }
}
