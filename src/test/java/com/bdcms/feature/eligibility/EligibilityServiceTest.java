package com.bdcms.feature.eligibility;

import com.bdcms.feature.donation.DonationDao;
import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.DonorDao;
import com.bdcms.shared.config.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EligibilityServiceTest {

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 8, 11);
    private static final long DONOR_ID = 42L;

    private SettingsService settingsService;
    private DonorDao donorDao;
    private DonationDao donationDao;
    private EligibilityService service;

    @BeforeEach
    void setUp() {
        settingsService = mock(SettingsService.class);
        donorDao = mock(DonorDao.class);
        donationDao = mock(DonationDao.class);
        Clock fixedClock = new MockClock(FIXED_DATE);
        service = new EligibilityService(settingsService, donorDao, donationDao, fixedClock);
    }

    @Test
    @DisplayName("first-time donor with no donation history is eligible")
    void firstTimeDonorIsEligible() throws Exception {
        Connection conn = mock(Connection.class);
        Donor donor = donorWithId(DONOR_ID);
        when(donorDao.findById(conn, DONOR_ID)).thenReturn(Optional.of(donor));
        when(donationDao.findLastDonationDate(conn, DONOR_ID)).thenReturn(Optional.empty());
        when(settingsService.getString(conn, "minimum_donation_interval_days", null)).thenReturn("84");

        EligibilityResult result = service.evaluate(conn, DONOR_ID);

        assertEquals(EligibilityStatus.ELIGIBLE, result.status());
        assertEquals(0, result.daysRemaining());
        assertNull(result.nextPermittedDate());
    }

    @Test
    @DisplayName("last donation exactly 84 days before the fixed date is eligible")
    void donationAtBoundaryIsEligible() throws Exception {
        Connection conn = mock(Connection.class);
        Donor donor = donorWithId(DONOR_ID);
        when(donorDao.findById(conn, DONOR_ID)).thenReturn(Optional.of(donor));
        when(donationDao.findLastDonationDate(conn, DONOR_ID)).thenReturn(Optional.of(FIXED_DATE.minusDays(84)));
        when(settingsService.getString(conn, "minimum_donation_interval_days", null)).thenReturn("84");

        EligibilityResult result = service.evaluate(conn, DONOR_ID);

        assertEquals(EligibilityStatus.ELIGIBLE, result.status());
        assertEquals(0, result.daysRemaining());
        assertNull(result.nextPermittedDate());
    }

    @Test
    @DisplayName("last donation 83 days before the fixed date is not eligible")
    void donationOneDayShortIsNotEligible() throws Exception {
        Connection conn = mock(Connection.class);
        Donor donor = donorWithId(DONOR_ID);
        when(donorDao.findById(conn, DONOR_ID)).thenReturn(Optional.of(donor));
        when(donationDao.findLastDonationDate(conn, DONOR_ID)).thenReturn(Optional.of(FIXED_DATE.minusDays(83)));
        when(settingsService.getString(conn, "minimum_donation_interval_days", null)).thenReturn("84");

        EligibilityResult result = service.evaluate(conn, DONOR_ID);

        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.status());
        assertEquals(1, result.daysRemaining());
        assertEquals(FIXED_DATE.plusDays(1), result.nextPermittedDate());
    }

    @Test
    @DisplayName("repeated donation on the same date is not eligible")
    void sameDayDonationIsNotEligible() throws Exception {
        Connection conn = mock(Connection.class);
        Donor donor = donorWithId(DONOR_ID);
        when(donorDao.findById(conn, DONOR_ID)).thenReturn(Optional.of(donor));
        when(donationDao.findLastDonationDate(conn, DONOR_ID)).thenReturn(Optional.of(FIXED_DATE));
        when(settingsService.getString(conn, "minimum_donation_interval_days", null)).thenReturn("84");

        EligibilityResult result = service.evaluate(conn, DONOR_ID);

        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.status());
        assertEquals(84, result.daysRemaining());
        assertEquals(FIXED_DATE.plusDays(84), result.nextPermittedDate());
    }

    @Test
    @DisplayName("donor not found raises a clear exception")
    void donorNotFoundThrows() throws Exception {
        Connection conn = mock(Connection.class);
        when(donorDao.findById(conn, DONOR_ID)).thenReturn(Optional.empty());

        assertThrows(DonorNotFoundException.class, () -> service.evaluate(conn, DONOR_ID));
    }

    @Test
    @DisplayName("custom waiting period is loaded from settings")
    void customWaitingPeriodIsLoaded() throws Exception {
        Connection conn = mock(Connection.class);
        Donor donor = donorWithId(DONOR_ID);
        when(donorDao.findById(conn, DONOR_ID)).thenReturn(Optional.of(donor));
        when(donationDao.findLastDonationDate(conn, DONOR_ID)).thenReturn(Optional.of(FIXED_DATE.minusDays(30)));
        when(settingsService.getString(conn, "minimum_donation_interval_days", null)).thenReturn("60");

        EligibilityResult result = service.evaluate(conn, DONOR_ID);

        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.status());
        assertEquals(30, result.daysRemaining());
        assertEquals(FIXED_DATE.plusDays(30), result.nextPermittedDate());
        verify(settingsService).getString(conn, "minimum_donation_interval_days", null);
    }

    @Test
    @DisplayName("missing settings fall back to the default interval")
    void missingSettingFallsBackToDefault() throws Exception {
        Connection conn = mock(Connection.class);
        Donor donor = donorWithId(DONOR_ID);
        when(donorDao.findById(conn, DONOR_ID)).thenReturn(Optional.of(donor));
        when(donationDao.findLastDonationDate(conn, DONOR_ID)).thenReturn(Optional.of(FIXED_DATE.minusDays(83)));
        when(settingsService.getString(conn, "minimum_donation_interval_days", null)).thenReturn(null);

        EligibilityResult result = service.evaluate(conn, DONOR_ID);

        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.status());
        assertEquals(1, result.daysRemaining());
        assertEquals(FIXED_DATE.plusDays(1), result.nextPermittedDate());
    }

    @Test
    @DisplayName("invalid setting values follow the service fallback policy")
    void invalidSettingFallsBackToDefault() throws Exception {
        Connection conn = mock(Connection.class);
        Donor donor = donorWithId(DONOR_ID);
        when(donorDao.findById(conn, DONOR_ID)).thenReturn(Optional.of(donor));
        when(donationDao.findLastDonationDate(conn, DONOR_ID)).thenReturn(Optional.of(FIXED_DATE.minusDays(83)));
        when(settingsService.getString(conn, "minimum_donation_interval_days", null)).thenReturn("not-a-number");

        EligibilityResult result = service.evaluate(conn, DONOR_ID);

        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.status());
        assertEquals(1, result.daysRemaining());
        assertEquals(FIXED_DATE.plusDays(1), result.nextPermittedDate());
    }

    private Donor donorWithId(long donorId) {
        Donor donor = new Donor();
        donor.setDonorId(donorId);
        return donor;
    }

    private static final class MockClock extends Clock {
        private final LocalDate date;

        private MockClock(LocalDate date) {
            this.date = date;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
    }
}
