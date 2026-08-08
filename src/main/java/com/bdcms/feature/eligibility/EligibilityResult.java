package com.bdcms.feature.eligibility;

import java.time.LocalDate;

/**
 * The answer to "may this donor donate today?" (FR-5).
 *
 * <p>Carries the working, not just the verdict. When a donor is turned away,
 * staff have to be able to tell them exactly when to come back and why, and the
 * audit trail should show the numbers the decision was made on - including the
 * interval that was in force at the time, since that value is configurable.
 *
 * @param status           eligible or not
 * @param lastDonationDate the most recent donation, or null for a first-time donor
 * @param daysSinceLast    days elapsed since that donation, or -1 if there is none
 * @param nextEligibleDate the earliest permitted date; today or earlier when eligible
 * @param intervalDays     the minimum interval applied, read from settings
 */
public record EligibilityResult(
        EligibilityStatus status,
        LocalDate lastDonationDate,
        long daysSinceLast,
        LocalDate nextEligibleDate,
        int intervalDays) {

    public boolean isEligible() {
        return status.isEligible();
    }

    /** True when this donor has no recorded donation at all. */
    public boolean isFirstDonation() {
        return lastDonationDate == null;
    }

    /** Days the donor still has to wait; zero when eligible. */
    public long getDaysRemaining() {
        if (isEligible() || nextEligibleDate == null) {
            return 0;
        }
        long remaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), nextEligibleDate);
        return Math.max(0, remaining);
    }

    /** Sentence shown on the verification screen and written into the donation record. */
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
                + ". The donor may next donate on " + nextEligibleDate
                + " (" + getDaysRemaining() + " days from today).";
    }

    // ---- JavaBean accessors for JSP/EL -------------------------------------
    // Tomcat 10.1 implements Jakarta EL 5.0, which cannot read record
    // components as properties (added in EL 6.0). See SessionUser for the note.

    public EligibilityStatus getStatus() {
        return status;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public long getDaysSinceLast() {
        return daysSinceLast;
    }

    public LocalDate getNextEligibleDate() {
        return nextEligibleDate;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    // ---- factories ---------------------------------------------------------

    public static EligibilityResult firstDonation(int intervalDays, LocalDate today) {
        return new EligibilityResult(EligibilityStatus.ELIGIBLE, null, -1, today, intervalDays);
    }

    public static EligibilityResult eligible(LocalDate lastDonation, long daysSince, int intervalDays,
                                             LocalDate today) {
        return new EligibilityResult(EligibilityStatus.ELIGIBLE, lastDonation, daysSince, today,
                intervalDays);
    }

    public static EligibilityResult notEligible(LocalDate lastDonation, long daysSince, int intervalDays) {
        return new EligibilityResult(EligibilityStatus.NOT_ELIGIBLE, lastDonation, daysSince,
                lastDonation.plusDays(intervalDays), intervalDays);
    }
}
