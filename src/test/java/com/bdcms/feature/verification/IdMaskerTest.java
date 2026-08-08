package com.bdcms.feature.verification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for national ID masking (FR-4).
 *
 * <p>The interesting cases are the short ones. A masking rule that keeps the
 * last four characters is fine for a nine-character ID and a disclosure for a
 * five-character one, so those are asserted explicitly.
 */
class IdMaskerTest {

    @Test
    @DisplayName("masks the proposal's own example: S1234567A -> S****567A")
    void masksTheProposalExample() {
        assertEquals("S••••567A", IdMasker.mask("S1234567A"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "S1234567A, S••••567A",
            "AB1234567, A••••4567",
            "1234567890, 1•••••7890",
            "ABCDEF, A•CDEF"
    })
    @DisplayName("keeps the first character and the last four")
    void keepsHeadAndTail(String input, String expected) {
        assertEquals(expected, IdMasker.mask(input));
    }

    @ParameterizedTest(name = "\"{0}\" is fully masked")
    @ValueSource(strings = {"A", "AB", "ABC", "ABCD", "ABCDE"})
    @DisplayName("values too short to mask meaningfully reveal nothing at all")
    void shortValuesAreFullyMasked(String input) {
        String masked = IdMasker.mask(input);

        assertEquals(input.length(), masked.length());
        assertTrue(masked.chars().allMatch(c -> c == '•'),
                "expected only bullets but got: " + masked);
    }

    @Test
    @DisplayName("the masked form never contains the middle of the original")
    void doesNotLeakTheMiddle() {
        String original = "S9876543A";

        String masked = IdMasker.mask(original);

        assertFalse(masked.contains("98765"), "middle digits leaked: " + masked);
        assertTrue(masked.startsWith("S"));
        assertTrue(masked.endsWith("543A"));
    }

    @Test
    @DisplayName("the masked form is the same length as the original, so nothing is inferred from it")
    void preservesLength() {
        assertEquals(9, IdMasker.mask("S1234567A").length());
        assertEquals(12, IdMasker.mask("ABCDEFGHIJKL").length());
    }

    @Test
    @DisplayName("null and blank input produce an empty string, never the word 'null'")
    void handlesMissingInput() {
        assertEquals("", IdMasker.mask(null));
        assertEquals("", IdMasker.mask(""));
        assertEquals("", IdMasker.mask("   "));
    }

    @Test
    @DisplayName("surrounding whitespace is trimmed before masking")
    void trimsInput() {
        assertEquals(IdMasker.mask("S1234567A"), IdMasker.mask("  S1234567A  "));
    }
}
