package com.bdcms.feature.eligibility;

import com.bdcms.feature.donation.DonationDao;
import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.DonorDao;
import com.bdcms.shared.config.SettingsService;
import com.bdcms.shared.db.Tx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The donation waiting-period rule (FR-5).
 *
 * <p>Eligibility is evaluated from the donor's most recent recorded donation,
 * using the configured waiting period from the settings table. Invalid or blank
 * values fall back to the documented 84-day default and are logged.
 */
public class EligibilityService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityService.class);

    /** Fallback if the settings row is missing: 12 weeks, per proposal S10.1. */
    public static final int DEFAULT_INTERVAL_DAYS = 84;
    private static final String SETTING_KEY = "minimum_donation_interval_days";

    private final SettingsService settings;
    private final DonorDao donorDao;
    private final DonationDao donationDao;
    private final Clock clock;

    public EligibilityService() {
        this(new SettingsService(), new DonorDao(), new DonationDao(), Clock.systemDefaultZone());
    }

    public EligibilityService(SettingsService settings, DonorDao donorDao,
                              DonationDao donationDao, Clock clock) {
        this.settings = settings;
        this.donorDao = donorDao;
        this.donationDao = donationDao;
        this.clock = clock;
    }

    public EligibilityResult evaluate(long donorId) {
        return Tx.read(conn -> evaluate(conn, donorId));
    }

    public EligibilityResult evaluate(Connection conn, LocalDate lastDonationDate) {
        int intervalDays = configuredIntervalDays(conn);
        return evaluate(lastDonationDate, intervalDays, LocalDate.now(clock));
    }

    EligibilityResult evaluate(Connection conn, long donorId) {
        Donor donor = donorDao.findById(conn, donorId)
                .orElseThrow(() -> new DonorNotFoundException("Donor not found for eligibility evaluation."));

        LocalDate lastDonationDate = donationDao.findLastDonationDate(conn, donor.getDonorId())
                .orElse(null);
        int intervalDays = configuredIntervalDays(conn);
        return evaluate(lastDonationDate, intervalDays, LocalDate.now(clock));
    }

    /**
     * The rule itself: pure, with today's date passed in rather than read from
     * the clock, so every boundary case is directly testable.
     */
    public EligibilityResult evaluate(LocalDate lastDonationDate, int intervalDays, LocalDate today) {
        if (intervalDays < 0) {
            throw new IllegalArgumentException("Donation interval cannot be negative");
        }
        if (today == null) {
            throw new IllegalArgumentException("Today's date is required");
        }

        if (lastDonationDate == null) {
            return EligibilityResult.eligible(null, -1, intervalDays, today, null);
        }

        long daysSince = ChronoUnit.DAYS.between(lastDonationDate, today);

        if (daysSince < 0) {
            return EligibilityResult.notEligible(lastDonationDate, daysSince, intervalDays,
                    lastDonationDate.plusDays(intervalDays), today);
        }

        if (daysSince >= intervalDays) {
            return EligibilityResult.eligible(lastDonationDate, daysSince, intervalDays, today, null);
        }

        LocalDate nextPermittedDate = lastDonationDate.plusDays(intervalDays);
        return EligibilityResult.notEligible(lastDonationDate, daysSince, intervalDays,
                nextPermittedDate, today);
    }

    /** The interval currently in force, in days. */
    public int configuredIntervalDays(Connection conn) {
        String configured = settings.getString(conn, SETTING_KEY, null);
        if (configured == null || configured.isBlank()) {
            log.warn("Donation interval setting was blank or missing; using the default {} days.",
                    DEFAULT_INTERVAL_DAYS);
            return DEFAULT_INTERVAL_DAYS;
        }

        try {
            int intervalDays = Integer.parseInt(configured.trim());
            if (intervalDays <= 0) {
                log.warn("Donation interval setting '{}' is invalid; using the default {} days.",
                        configured, DEFAULT_INTERVAL_DAYS);
                return DEFAULT_INTERVAL_DAYS;
            }
            return intervalDays;
        } catch (NumberFormatException e) {
            log.warn("Donation interval setting '{}' is not numeric; using the default {} days.",
                    configured, DEFAULT_INTERVAL_DAYS);
            return DEFAULT_INTERVAL_DAYS;
        }
    }
}
