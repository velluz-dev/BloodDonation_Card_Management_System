package com.bdcms.feature.eligibility;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The answer to "may this donor donate today?" (FR-5).
 *
 * <p>This class carries the verdict plus the working values required by the
 * donation flow and the JSP views.
 */
public class EligibilityResult {

    private final EligibilityStatus status;
    private final long daysRemaining;
    private final LocalDate nextPermittedDate;
    private final LocalDate lastDonationDate;
    private final long daysSinceLast;
    private final int intervalDays;

    private EligibilityResult(EligibilityStatus status, LocalDate lastDonationDate, long daysSinceLast,
                              int intervalDays, LocalDate nextPermittedDate, long daysRemaining) {
        this.status = status;
        this.lastDonationDate = lastDonationDate;
        this.daysSinceLast = daysSinceLast;
        this.intervalDays = intervalDays;
        this.nextPermittedDate = nextPermittedDate;
        this.daysRemaining = daysRemaining;
    }

    public EligibilityStatus status() {
        return status;
    }

    public EligibilityStatus getStatus() {
        return status;
    }

    public long daysRemaining() {
        return daysRemaining;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public LocalDate nextPermittedDate() {
        return nextPermittedDate;
    }

    public LocalDate getNextPermittedDate() {
        return nextPermittedDate;
    }

    public LocalDate nextEligibleDate() {
        return nextPermittedDate;
    }

    public LocalDate getNextEligibleDate() {
        return nextPermittedDate;
    }

    public LocalDate lastDonationDate() {
        return lastDonationDate;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public long daysSinceLast() {
        return daysSinceLast;
    }

    public long getDaysSinceLast() {
        return daysSinceLast;
    }

    public int intervalDays() {
        return intervalDays;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public boolean isEligible() {
        return status.isEligible();
    }

    public boolean isFirstDonation() {
        return lastDonationDate == null;
    }

    public boolean isNotEligible() {
        return !isEligible();
    }

    public String getExplanation() {
        if (isFirstDonation()) {
            return "First recorded donation - no waiting period applies.";
        }
        if (isEligible()) {
            return daysSinceLast + " days since the last donation on " + lastDonationDate
                    + ", which meets the required interval of " + intervalDays + " days.";
        }
        return "Only " + daysSinceLast + " of the required " + intervalDays
                + " days have passed since " + lastDonationDate
                + ". The donor may next donate on " + nextPermittedDate
                + " (" + daysRemaining + " days from today).";
    }

    public static EligibilityResult eligible(LocalDate lastDonationDate, long daysSinceLast,
                                             int intervalDays, LocalDate today,
                                             LocalDate nextPermittedDate) {
        return new EligibilityResult(EligibilityStatus.ELIGIBLE, lastDonationDate, daysSinceLast,
                intervalDays, nextPermittedDate, 0L);
    }

    public static EligibilityResult notEligible(LocalDate lastDonationDate, long daysSinceLast,
                                                 int intervalDays, LocalDate nextPermittedDate,
                                                 LocalDate today) {
        long daysRemaining = ChronoUnit.DAYS.between(today, nextPermittedDate);
        return new EligibilityResult(EligibilityStatus.NOT_ELIGIBLE, lastDonationDate, daysSinceLast,
                intervalDays, nextPermittedDate, Math.max(0L, daysRemaining));
    }
}
