package com.bdcms.feature.auth;

import com.bdcms.shared.config.AppConfig;
import org.mindrot.jbcrypt.BCrypt;

/**
 * One-way password hashing (NFR Security).
 *
 * <p>The single place in the system where a plaintext secret is handled.
 * BCrypt is used with a per-password random salt, so two staff with the same
 * password produce different hashes and a stolen database yields no passwords.
 * The same helper hashes the optional four-digit donor PIN from Section 10 of
 * the proposal - a PIN is short, but it is still never stored in the clear.
 */
public final class Passwords {

    private Passwords() {
    }

    /** Hashes {@code plaintext} with a fresh random salt. */
    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(AppConfig.bcryptCost()));
    }

    /**
     * Verifies {@code plaintext} against a stored hash. Returns false rather
     * than throwing on a null or malformed hash, so a corrupt row cannot be
     * distinguished from a wrong password by an attacker.
     */
    public static boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null || storedHash.length() != 60) {
            return false;
        }
        try {
            return BCrypt.checkpw(plaintext, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
