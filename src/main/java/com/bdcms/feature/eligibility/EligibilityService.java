package com.bdcms.feature.eligibility;

import com.bdcms.shared.config.SettingsService;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The donation waiting-period rule (FR-5).
 *
 * <p>This feature owns no table and imports no other feature. It is handed the
 * donor's last donation date and returns a verdict. The donation feature, which
 * does own that data, is the one that supplies it.
 *
 * <p>That separation is what makes the rule testable without a database, and it
 * is why the rule can be exercised against a hundred boundary dates in
 * milliseconds rather than requiring a seeded MySQL instance.
 *
 * <p>The interval itself is never a constant here - it is read from the
 * {@code settings} table through {@link SettingsService}, so a centre can move
 * from 84 days to a different figure without a redeployment.
 */
public class EligibilityService {

    /** Fallback if the settings row is missing: 12 weeks, per proposal S10.1. */
    public static final int DEFAULT_INTERVAL_DAYS = 84;

    private final SettingsService settings;

    public EligibilityService() {
        this(new SettingsService());
    }

    public EligibilityService(SettingsService settings) {
        this.settings = settings;
    }

    /**
     * Evaluates eligibility using the interval currently configured, joining the
     * caller's transaction so the donation flow reads and writes consistently.
     *
     * @param lastDonationDate the donor's most recent donation, or null if none
     */
    public EligibilityResult evaluate(Connection conn, LocalDate lastDonationDate) {
        int intervalDays = configuredIntervalDays(conn);
        return evaluate(lastDonationDate, intervalDays, LocalDate.now());
    }

    /**
     * The rule itself: pure, with today's date passed in rather than read from
     * the clock, so every boundary case is directly testable.
     *
     * <p>The comparison is {@code >=}: a donor whose last donation was exactly
     * {@code intervalDays} ago has completed the waiting period and is eligible.
     */
    public EligibilityResult evaluate(LocalDate lastDonationDate, int intervalDays, LocalDate today) {
        if (intervalDays < 0) {
            throw new IllegalArgumentException("Donation interval cannot be negative");
        }
        if (today == null) {
            throw new IllegalArgumentException("Today's date is required");
        }
        if (lastDonationDate == null) {
            return EligibilityResult.firstDonation(intervalDays, today);
        }

        long daysSince = ChronoUnit.DAYS.between(lastDonationDate, today);

        // A donation dated in the future is bad data, not eligibility. Treat it
        // as not eligible rather than silently letting a negative gap pass.
        if (daysSince < 0) {
            return EligibilityResult.notEligible(lastDonationDate, daysSince, intervalDays);
        }
        return daysSince >= intervalDays
                ? EligibilityResult.eligible(lastDonationDate, daysSince, intervalDays, today)
                : EligibilityResult.notEligible(lastDonationDate, daysSince, intervalDays);
    }

    /** The interval currently in force, in days. */
    public int configuredIntervalDays(Connection conn) {
        return settings.getInt(conn, SettingsService.MIN_DONATION_INTERVAL_DAYS,
                DEFAULT_INTERVAL_DAYS);
    }
}
