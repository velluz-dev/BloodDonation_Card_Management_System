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
    /**
     * Bundle key for the sentence explaining this verdict.
     *
     * <p>Paired with {@link #getExplanationArgs()}, which supplies the same five
     * values in the same order to all three messages. A message uses the subset
     * it needs and MessageFormat ignores the rest, which is what lets a JSP
     * render any verdict with one block and no {@code <c:choose>} - the "no
     * business logic in JSP pages" rule applies to picking a sentence just as
     * much as to picking a donor.
     */
    public String getExplanationKey() {
        if (isFirstDonation()) {
            return "eligibility.explain.first";
        }
        return isEligible() ? "eligibility.explain.eligible"
                            : "eligibility.explain.notEligible";
    }

    /**
     * Values for {@link #getExplanationKey()}, always in this order:
     * {@code {0}} days since, {@code {1}} last donation, {@code {2}} required
     * interval, {@code {3}} next eligible date, {@code {4}} days remaining.
     */
    public Object[] getExplanationArgs() {
        return new Object[] {
                daysSinceLast, lastDonationDate, intervalDays,
                nextEligibleDate, getDaysRemaining()
        };
    }

    /**
     * The English sentence, for logs and for the audit detail column.
     *
     * <p>Deliberately not translated: a log line is read by whoever is on call,
     * and an audit row is evidence that should read the same to every reviewer
     * regardless of the language the operator happened to have selected.
     * Screens use {@link #getExplanationKey()} instead.
     */
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
