package com.bdcms.feature.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for password and PIN hashing (FR-1, NFR Security). */
class PasswordsTest {

    private static final String PASSWORD = "Staff@123";

    @Test
    @DisplayName("a hash is never the plaintext, and does not contain it")
    void hashIsNotPlaintext() {
        String hash = Passwords.hash(PASSWORD);

        assertNotEquals(PASSWORD, hash);
        assertFalse(hash.contains(PASSWORD));
    }

    @Test
    @DisplayName("the correct password verifies against its own hash")
    void correctPasswordVerifies() {
        String hash = Passwords.hash(PASSWORD);

        assertTrue(Passwords.verify(PASSWORD, hash));
    }

    @Test
    @DisplayName("a wrong password does not verify")
    void wrongPasswordFails() {
        String hash = Passwords.hash(PASSWORD);

        assertFalse(Passwords.verify("Staff@124", hash));
        assertFalse(Passwords.verify("staff@123", hash));
        assertFalse(Passwords.verify("", hash));
    }

    @Test
    @DisplayName("the same password hashed twice gives different hashes - the salt is random")
    void saltIsRandom() {
        String first = Passwords.hash(PASSWORD);
        String second = Passwords.hash(PASSWORD);

        assertNotEquals(first, second,
                "identical hashes mean a fixed salt, which would let a rainbow table "
                        + "crack every account at once");
        // Both must still verify: the salt travels inside the hash.
        assertTrue(Passwords.verify(PASSWORD, first));
        assertTrue(Passwords.verify(PASSWORD, second));
    }

    @Test
    @DisplayName("a hash is the 60 characters the CHAR(60) column expects")
    void hashFitsTheColumn() {
        assertEquals(60, Passwords.hash(PASSWORD).length());
    }

    @Test
    @DisplayName("verification against a null or malformed hash fails rather than throwing")
    void malformedHashFailsClosed() {
        // A corrupt row must look exactly like a wrong password, not like an error.
        assertFalse(Passwords.verify(PASSWORD, null));
        assertFalse(Passwords.verify(PASSWORD, ""));
        assertFalse(Passwords.verify(PASSWORD, "not-a-bcrypt-hash"));
        assertFalse(Passwords.verify(PASSWORD, "$2a$12$tooshort"));
    }

    @Test
    @DisplayName("a null plaintext fails verification rather than throwing")
    void nullPlaintextFails() {
        assertFalse(Passwords.verify(null, Passwords.hash(PASSWORD)));
    }

    @Test
    @DisplayName("hashing an empty password is rejected outright")
    void emptyPasswordRejected() {
        assertThrows(IllegalArgumentException.class, () -> Passwords.hash(""));
        assertThrows(IllegalArgumentException.class, () -> Passwords.hash(null));
    }

    @Test
    @DisplayName("the same mechanism protects a four-digit donor PIN")
    void hashesDonorPins() {
        String hash = Passwords.hash("1234");

        assertTrue(Passwords.verify("1234", hash));
        assertFalse(Passwords.verify("4321", hash));
        assertFalse(hash.contains("1234"));
    }

    @Test
    @DisplayName("the seeded admin hash in db/02_seed.sql verifies against Admin@123")
    void seededAdminHashIsValid() {
        // Guards against the seed file and the login page drifting apart - a
        // failure here means a fresh install cannot be signed into.
        String seeded = "$2a$12$gK6zx5buD9/iRUaLose07eKTAxZO4NPeLgZcoUUgVCqJQ7NQpqfyG";

        assertTrue(Passwords.verify("Admin@123", seeded));
        assertFalse(Passwords.verify("wrong", seeded));
    }
}
