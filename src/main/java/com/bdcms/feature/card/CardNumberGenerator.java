package com.bdcms.feature.card;

import java.security.SecureRandom;
import java.time.Year;

/**
 * Generates the machine-readable card number printed on every donor card
 * (FR-3), e.g. {@code BDC-2026-4KDQ7M2P}.
 *
 * <p>Two properties matter and both are deliberate:
 *
 * <p><strong>Unguessable.</strong> The random part comes from
 * {@link SecureRandom}, not a sequence. A sequential number would let anyone
 * holding one card derive the numbers of cards issued around it, and the card
 * number is the key used to pull up a donor's record.
 *
 * <p><strong>Unambiguous when read aloud or scanned.</strong> The alphabet is
 * Crockford Base32, which omits I, L, O and U - so a scanner misreading a 1 as
 * an I, or a member of staff typing O for 0, cannot produce a different valid
 * card number.
 *
 * <p>Uniqueness is not assumed from randomness alone: the caller retries
 * against the {@code UNIQUE} index on {@code donor_cards.card_no}.
 */
public final class CardNumberGenerator {

    /** Crockford Base32: digits and letters, minus I, L, O and U. */
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static final String PREFIX = "BDC";
    private static final int RANDOM_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    /** A new candidate card number for the current year. */
    public String next() {
        return next(Year.now().getValue());
    }

    /** A new candidate card number for a given year, so the format can be tested deterministically. */
    public String next(int year) {
        StringBuilder sb = new StringBuilder(PREFIX).append('-').append(year).append('-');
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    /**
     * True when {@code value} has the shape this generator produces.
     * Used to reject obvious rubbish at the verification screen before a query
     * is issued.
     */
    public static boolean isWellFormed(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim().toUpperCase();
        if (trimmed.length() != PREFIX.length() + 1 + 4 + 1 + RANDOM_LENGTH) {
            return false;
        }
        if (!trimmed.startsWith(PREFIX + "-") || trimmed.charAt(8) != '-') {
            return false;
        }
        for (int i = 4; i < 8; i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        for (int i = 9; i < trimmed.length(); i++) {
            if (!isAllowed(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Normalises operator or scanner input: trims, upper-cases, and maps the
     * characters Crockford Base32 treats as equivalent (I/L to 1, O to 0), so a
     * mistyped card number still finds the right donor.
     */
    public static String normalise(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toUpperCase();
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (char c : trimmed.toCharArray()) {
            switch (c) {
                case 'I', 'L' -> sb.append('1');
                case 'O' -> sb.append('0');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isAllowed(char c) {
        for (char allowed : ALPHABET) {
            if (allowed == c) {
                return true;
            }
        }
        return false;
    }
}
