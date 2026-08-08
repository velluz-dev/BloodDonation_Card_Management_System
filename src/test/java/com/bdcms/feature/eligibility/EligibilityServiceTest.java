package com.bdcms.feature.eligibility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the donation waiting-period rule (FR-5).
 *
 * <p>These run without a database, a container or a clock: the rule is a pure
 * function of (last donation, interval, today), so every boundary can be stated
 * directly. That is the payoff of keeping the eligibility feature free of any
 * dependency on the donation feature's tables.
 */
class EligibilityServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 7);
    private static final int INTERVAL = 84;

    private final EligibilityService service = new EligibilityService();

    @Nested
    @DisplayName("First-time donors")
    class FirstDonation {

        @Test
        @DisplayName("a donor with no recorded donation is eligible")
        void noHistoryIsEligible() {
            EligibilityResult result = service.evaluate(null, INTERVAL, TODAY);

            assertTrue(result.isEligible());
            assertTrue(result.isFirstDonation());
            assertNull(result.lastDonationDate());
            assertEquals(0, result.getDaysRemaining());
        }

        @Test
        @DisplayName("the explanation says no waiting period applies")
        void explainsWhy() {
            EligibilityResult result = service.evaluate(null, INTERVAL, TODAY);

            assertTrue(result.getExplanation().contains("First recorded donation"));
        }
    }

    @Nested
    @DisplayName("The interval boundary")
    class Boundary {

        @Test
        @DisplayName("exactly 84 days since the last donation is ELIGIBLE")
        void exactlyAtIntervalIsEligible() {
            LocalDate lastDonation = TODAY.minusDays(INTERVAL);

            EligibilityResult result = service.evaluate(lastDonation, INTERVAL, TODAY);

            assertEquals(EligibilityStatus.ELIGIBLE, result.status());
            assertEquals(INTERVAL, result.daysSinceLast());
        }

        @Test
        @DisplayName("83 days - one day short - is NOT ELIGIBLE")
        void oneDayShortIsNotEligible() {
            LocalDate lastDonation = TODAY.minusDays(INTERVAL - 1);

            EligibilityResult result = service.evaluate(lastDonation, INTERVAL, TODAY);

            assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.status());
            assertEquals(83, result.daysSinceLast());
        }

        @Test
        @DisplayName("85 days is comfortably eligible")
        void oneDayPastIsEligible() {
            EligibilityResult result = service.evaluate(TODAY.minusDays(INTERVAL + 1), INTERVAL, TODAY);

            assertTrue(result.isEligible());
        }

        @Test
        @DisplayName("donating today makes the donor ineligible")
        void sameDayIsNotEligible() {
            EligibilityResult result = service.evaluate(TODAY, INTERVAL, TODAY);

            assertFalse(result.isEligible());
            assertEquals(0, result.daysSinceLast());
        }
    }

    @Nested
    @DisplayName("The next permitted date")
    class NextEligibleDate {

        @Test
        @DisplayName("is the last donation plus the interval")
        void computedFromLastDonation() {
            LocalDate lastDonation = LocalDate.of(2026, 7, 1);

            EligibilityResult result = service.evaluate(lastDonation, INTERVAL, TODAY);

            assertFalse(result.isEligible());
            assertEquals(LocalDate.of(2026, 9, 23), result.nextEligibleDate());
        }

        @Test
        @DisplayName("days remaining counts from today, not from the last donation")
        void daysRemainingIsFromToday() {
            // 30 days elapsed of 84 required, so 54 remain.
            EligibilityResult result = service.evaluate(LocalDate.now().minusDays(30), INTERVAL,
                    LocalDate.now());

            assertEquals(54, result.getDaysRemaining());
        }

        @Test
        @DisplayName("is not in the future once the donor is eligible")
        void zeroRemainingWhenEligible() {
            EligibilityResult result = service.evaluate(TODAY.minusDays(200), INTERVAL, TODAY);

            assertEquals(0, result.getDaysRemaining());
        }
    }

    @Nested
    @DisplayName("A configurable interval")
    class ConfigurableInterval {

        @ParameterizedTest(name = "{0} days elapsed against a {1} day interval -> eligible={2}")
        @CsvSource({
                "56,  56, true",
                "55,  56, false",
                "84,  56, true",
                "90, 120, false",
                "120,120, true",
                "1,    1, true",
                "0,    0, true"
        })
        @DisplayName("the rule follows whatever interval is configured")
        void honoursConfiguredInterval(long daysElapsed, int intervalDays, boolean expected) {
            EligibilityResult result =
                    service.evaluate(TODAY.minusDays(daysElapsed), intervalDays, TODAY);

            assertEquals(expected, result.isEligible());
            assertEquals(intervalDays, result.intervalDays());
        }

        @Test
        @DisplayName("an interval of zero makes every donor eligible immediately")
        void zeroIntervalAlwaysEligible() {
            assertTrue(service.evaluate(TODAY, 0, TODAY).isEligible());
        }
    }

    @Nested
    @DisplayName("Bad input")
    class BadInput {

        @Test
        @DisplayName("a donation dated in the future is treated as not eligible, not as a huge gap")
        void futureDonationIsNotEligible() {
            EligibilityResult result = service.evaluate(TODAY.plusDays(5), INTERVAL, TODAY);

            assertFalse(result.isEligible());
            assertEquals(-5, result.daysSinceLast());
        }

        @Test
        @DisplayName("a negative interval is rejected rather than silently accepted")
        void negativeIntervalRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(TODAY.minusDays(10), -1, TODAY));
        }

        @Test
        @DisplayName("a missing 'today' is rejected")
        void nullTodayRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(TODAY.minusDays(10), INTERVAL, null));
        }
    }

    @Test
    @DisplayName("the default interval is the 84 days the proposal specifies")
    void defaultIntervalMatchesProposal() {
        assertEquals(84, EligibilityService.DEFAULT_INTERVAL_DAYS);
    }
}
