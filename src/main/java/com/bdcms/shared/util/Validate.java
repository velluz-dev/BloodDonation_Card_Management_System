package com.bdcms.shared.util;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Server-side input validation.
 *
 * <p>The browser also validates these fields, but client-side checks are a
 * convenience, not a control: anything reaching a DAO has been through here
 * first.
 *
 * <p>Errors accumulate into a field-keyed map so a form can be redisplayed with
 * every problem marked at once, rather than one per round trip.
 */
public final class Validate {

    private static final Pattern NATIONAL_ID = Pattern.compile("^[A-Za-z0-9-]{5,20}$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9][0-9 \\-()]{6,19}$");
    private static final Pattern PIN = Pattern.compile("^[0-9]{4}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,40}$");

    /** Minimum and maximum ages accepted for a blood donor. */
    public static final int MIN_DONOR_AGE = 16;
    public static final int MAX_DONOR_AGE = 70;

    private Validate() {
    }

    /** Collects {@code field -> message} pairs for redisplay on a form. */
    public static final class Errors {
        private final Map<String, String> messages = new LinkedHashMap<>();

        public void add(String field, String message) {
            messages.putIfAbsent(field, message);
        }

        /** Adds {@code message} against {@code field} when {@code ok} is false. */
        public void check(boolean ok, String field, String message) {
            if (!ok) {
                add(field, message);
            }
        }

        public boolean isEmpty() {
            return messages.isEmpty();
        }

        public boolean hasErrors() {
            return !messages.isEmpty();
        }

        public Map<String, String> asMap() {
            return messages;
        }

        public String first() {
            return messages.isEmpty() ? null : messages.values().iterator().next();
        }
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isPresent(String value) {
        return !isBlank(value);
    }

    public static boolean maxLength(String value, int max) {
        return value == null || value.length() <= max;
    }

    public static boolean isNationalId(String value) {
        return isPresent(value) && NATIONAL_ID.matcher(value).matches();
    }

    public static boolean isPhone(String value) {
        return isPresent(value) && PHONE.matcher(value).matches();
    }

    public static boolean isPin(String value) {
        return isPresent(value) && PIN.matcher(value).matches();
    }

    public static boolean isUsername(String value) {
        return isPresent(value) && USERNAME.matcher(value).matches();
    }

    /** Date of birth must be in the past and give a plausible donor age. */
    public static boolean isDonorAge(LocalDate dateOfBirth, LocalDate today) {
        if (dateOfBirth == null || !dateOfBirth.isBefore(today)) {
            return false;
        }
        int age = java.time.Period.between(dateOfBirth, today).getYears();
        return age >= MIN_DONOR_AGE && age <= MAX_DONOR_AGE;
    }

    /** Normalises a national ID for storage and comparison: trimmed and upper-cased. */
    public static String normaliseNationalId(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
