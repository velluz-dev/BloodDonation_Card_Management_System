package com.bdcms.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the server-side input validation used by donor registration (FR-2). */
class ValidateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 7);

    @ParameterizedTest
    @ValueSource(strings = {"S1234567A", "AB-123456", "12345", "ABCDEFGHIJ1234567890"})
    @DisplayName("accepts plausible national ID formats")
    void acceptsValidNationalIds(String value) {
        assertTrue(Validate.isNationalId(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "S123 4567A", "S1234567A!", "ABCDEFGHIJ12345678901"})
    @DisplayName("rejects national IDs that are too short, too long, or contain separators")
    void rejectsInvalidNationalIds(String value) {
        assertFalse(Validate.isNationalId(value));
    }

    @Test
    @DisplayName("national IDs are normalised to upper case and trimmed before storage")
    void normalisesNationalId() {
        // Without this, "s1234567a" and "S1234567A" would be two donors and the
        // unique index would not stop the duplicate the system exists to prevent.
        assertEquals("S1234567A", Validate.normaliseNationalId("  s1234567a "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+95 9 111 2233", "09777123456", "+1 (555) 123-4567"})
    @DisplayName("accepts international and local phone formats")
    void acceptsValidPhones(String value) {
        assertTrue(Validate.isPhone(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "not-a-phone", "++95911122", "9111 2233 abc"})
    @DisplayName("rejects phone numbers that are too short or contain letters")
    void rejectsInvalidPhones(String value) {
        assertFalse(Validate.isPhone(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000", "1234", "9999"})
    @DisplayName("a PIN is exactly four digits")
    void acceptsFourDigitPins(String value) {
        assertTrue(Validate.isPin(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345", "12a4", "", " 1234"})
    @DisplayName("rejects PINs that are not exactly four digits")
    void rejectsBadPins(String value) {
        assertFalse(Validate.isPin(value));
    }

    @Test
    @DisplayName("donor age must fall inside the accepted range")
    void checksDonorAge() {
        assertTrue(Validate.isDonorAge(TODAY.minusYears(30), TODAY));
        assertTrue(Validate.isDonorAge(TODAY.minusYears(Validate.MIN_DONOR_AGE), TODAY));
        assertTrue(Validate.isDonorAge(TODAY.minusYears(Validate.MAX_DONOR_AGE), TODAY));
    }

    @Test
    @DisplayName("rejects donors who are too young, too old, unborn, or born today")
    void rejectsImplausibleAges() {
        assertFalse(Validate.isDonorAge(TODAY.minusYears(Validate.MIN_DONOR_AGE - 1), TODAY));
        assertFalse(Validate.isDonorAge(TODAY.minusYears(Validate.MAX_DONOR_AGE + 1), TODAY));
        assertFalse(Validate.isDonorAge(TODAY.plusDays(1), TODAY));
        assertFalse(Validate.isDonorAge(TODAY, TODAY));
        assertFalse(Validate.isDonorAge(null, TODAY));
    }

    @Test
    @DisplayName("errors accumulate so a form can show every problem at once")
    void collectsAllErrors() {
        Validate.Errors errors = new Validate.Errors();

        errors.check(false, "fullName", "Full name is required.");
        errors.check(true, "gender", "never added");
        errors.check(false, "contactNo", "Contact number is not valid.");

        assertTrue(errors.hasErrors());
        assertEquals(2, errors.asMap().size());
        assertEquals("Full name is required.", errors.first());
        assertFalse(errors.asMap().containsKey("gender"));
    }

    @Test
    @DisplayName("the first message per field wins, so one field never shows two errors")
    void keepsFirstMessagePerField() {
        Validate.Errors errors = new Validate.Errors();

        errors.add("fullName", "first message");
        errors.add("fullName", "second message");

        assertEquals(1, errors.asMap().size());
        assertEquals("first message", errors.asMap().get("fullName"));
    }

    @Test
    @DisplayName("blank checks treat whitespace as absent")
    void blankTreatsWhitespaceAsEmpty() {
        assertTrue(Validate.isBlank(null));
        assertTrue(Validate.isBlank(""));
        assertTrue(Validate.isBlank("   "));
        assertTrue(Validate.isPresent("a"));
    }
}
