package com.bdcms.feature.card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for card number generation and normalisation (FR-3). */
class CardNumberGeneratorTest {

    private final CardNumberGenerator generator = new CardNumberGenerator();

    @Test
    @DisplayName("produces the documented format BDC-<year>-<8 characters>")
    void producesExpectedFormat() {
        String cardNo = generator.next(2026);

        assertTrue(cardNo.matches("BDC-2026-[0-9A-HJKMNP-TV-Z]{8}"),
                "unexpected format: " + cardNo);
        assertEquals(17, cardNo.length());
    }

    @Test
    @DisplayName("10,000 generated numbers are all distinct")
    void generatesDistinctNumbers() {
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < 10_000; i++) {
            seen.add(generator.next(2026));
        }

        assertEquals(10_000, seen.size(),
                "collisions occurred; the database unique index is the real guard, "
                        + "but the generator should not be producing them at this rate");
    }

    @Test
    @DisplayName("never uses the ambiguous characters I, L, O or U")
    void avoidsAmbiguousCharacters() {
        // Those four are what a scanner or a member of staff confuses with 1 and 0.
        for (int i = 0; i < 2_000; i++) {
            String random = generator.next(2026).substring(9);

            assertFalse(random.contains("I"), "contains I: " + random);
            assertFalse(random.contains("L"), "contains L: " + random);
            assertFalse(random.contains("O"), "contains O: " + random);
            assertFalse(random.contains("U"), "contains U: " + random);
        }
    }

    @Test
    @DisplayName("its own output is always well-formed")
    void ownOutputIsWellFormed() {
        for (int i = 0; i < 500; i++) {
            assertTrue(CardNumberGenerator.isWellFormed(generator.next(2026)));
        }
    }

    @ParameterizedTest(name = "\"{0}\" is rejected as malformed")
    @ValueSource(strings = {
            "BDC-2026-4KDQ7M2",      // too short
            "BDC-2026-4KDQ7M2PP",    // too long
            "XXX-2026-4KDQ7M2P",     // wrong prefix
            "BDC-20X6-4KDQ7M2P",     // non-numeric year
            "BDC-2026-4KDQ7M2I",     // excluded character
            "BDC20264KDQ7M2P",       // missing separators
            ""
    })
    @DisplayName("obvious rubbish is rejected before a query is issued")
    void rejectsMalformed(String value) {
        assertFalse(CardNumberGenerator.isWellFormed(value));
    }

    @Test
    @DisplayName("null is not well-formed and does not throw")
    void nullIsRejected() {
        assertFalse(CardNumberGenerator.isWellFormed(null));
    }

    @Test
    @DisplayName("normalisation upper-cases and trims operator input")
    void normalisesCase() {
        assertEquals("BDC-2026-4KDQ7M2P", CardNumberGenerator.normalise("  bdc-2026-4kdq7m2p  "));
    }

    @Test
    @DisplayName("normalisation maps I and L to 1 and O to 0, so a mistyped card still resolves")
    void normalisesAmbiguousCharacters() {
        // The generator can never emit I, L or O, so any of them in input is a
        // misreading of 1 or 0 and should be corrected rather than rejected.
        assertEquals("BDC-2026-11110000", CardNumberGenerator.normalise("BDC-2026-ILlLOoO0"));
    }

    @Test
    @DisplayName("normalising null returns null rather than throwing")
    void normaliseHandlesNull() {
        assertEquals(null, CardNumberGenerator.normalise(null));
    }
}
