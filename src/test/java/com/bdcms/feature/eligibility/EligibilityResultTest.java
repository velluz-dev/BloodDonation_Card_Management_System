package com.bdcms.feature.eligibility;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EligibilityResultTest {

    @Test
    void capturesTheNextPermittedDateAndDaysRemaining() {
        EligibilityResult result = EligibilityResult.notEligible(
                LocalDate.of(2026, 8, 1),
                30,
                84,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 8, 11)
        );

        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.status());
        assertEquals(4, result.daysRemaining());
        assertEquals(LocalDate.of(2026, 8, 15), result.nextPermittedDate());
    }
}
