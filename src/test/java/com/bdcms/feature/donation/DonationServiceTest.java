package com.bdcms.feature.donation;

import com.bdcms.feature.auth.AuthService;
import com.bdcms.feature.auth.User;
import com.bdcms.feature.card.CardService;
import com.bdcms.feature.card.DonorCard;
import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.DonorService;
import com.bdcms.feature.eligibility.EligibilityResult;
import com.bdcms.feature.eligibility.EligibilityService;
import com.bdcms.feature.verification.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonationServiceTest {

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 8, 11);
    private static final long DONOR_ID = 42L;
    private static final long CARD_ID = 202L;
    private static final long STAFF_ID = 9001L;

    private DonationRepository donationRepository;
    private CardService cardService;
    private VerificationService verificationService;
    private EligibilityService eligibilityService;
    private DonorService donorService;
    private AuthService authService;
    private Clock fixedClock;
    private DonationService service;

    @BeforeEach
    void setUp() {
        donationRepository = mock(DonationRepository.class);
        cardService = mock(CardService.class);
        verificationService = mock(VerificationService.class);
        eligibilityService = mock(EligibilityService.class);
        donorService = mock(DonorService.class);
        authService = mock(AuthService.class);
        fixedClock = Clock.fixed(FIXED_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        service = new DonationService(donationRepository, cardService, verificationService,
                eligibilityService, donorService, authService, fixedClock);
    }

    @Test
    @DisplayName("successful donation recording writes a donation and returns a donation entity")
    void successfulRecording() {
        Donation donation = new Donation();
        Donor donor = donorWithId(DONOR_ID);
        DonorCard card = donorCard(CARD_ID, DONOR_ID, true);
        User staff = user(STAFF_ID, true);
        EligibilityResult eligibility = eligibleResult(null, 84);

        Connection conn = mock(Connection.class);
        when(donorService.findById(any(), eq(DONOR_ID))).thenReturn(Optional.of(donor));
        when(cardService.findById(any(), eq(CARD_ID))).thenReturn(Optional.of(card));
        when(authService.findById(any(), eq(STAFF_ID))).thenReturn(Optional.of(staff));
        when(donationRepository.findLastDonationDate(any(), eq(DONOR_ID))).thenReturn(Optional.empty());
        when(eligibilityService.evaluate(any(), isNull())).thenReturn(eligibility);
        when(donationRepository.insert(any(), any(Donation.class))).thenAnswer(invocation -> {
            Donation saved = invocation.getArgument(1);
            saved.setDonationId(123L);
            return 123L;
        });

        Donation result = service.recordDonation(conn, DONOR_ID, CARD_ID, "Central Station", 450, STAFF_ID);

        assertEquals(123L, result.getDonationId());
        assertEquals(DONOR_ID, result.getDonorId());
        assertEquals(CARD_ID, result.getCardId());
        assertEquals("Central Station", result.getCentre());
        assertEquals(450, result.getVolumeMl());
        assertEquals(STAFF_ID, result.getVerifiedBy());
        assertEquals(FIXED_DATE, result.getDonationDate());
        verify(donationRepository).insert(any(), any(Donation.class));
    }

    @Test
    @DisplayName("ineligible donor causes a DonorNotEligibleException and no donation is saved")
    void ineligibleDonorRejected() {
        Donor donor = donorWithId(DONOR_ID);
        DonorCard card = donorCard(CARD_ID, DONOR_ID, true);
        User staff = user(STAFF_ID, true);
        EligibilityResult eligibility = notEligibleResult(FIXED_DATE.minusDays(83), 84, FIXED_DATE.plusDays(1));

        Connection conn = mock(Connection.class);
        when(donorService.findById(any(), eq(DONOR_ID))).thenReturn(Optional.of(donor));
        when(cardService.findById(any(), eq(CARD_ID))).thenReturn(Optional.of(card));
        when(authService.findById(any(), eq(STAFF_ID))).thenReturn(Optional.of(staff));
        when(donationRepository.findLastDonationDate(any(), eq(DONOR_ID)))
                .thenReturn(Optional.of(FIXED_DATE.minusDays(83)));
        when(eligibilityService.evaluate(any(), eq(FIXED_DATE.minusDays(83))))
                .thenReturn(eligibility);

        DonorNotEligibleException exception = assertThrows(DonorNotEligibleException.class,
                () -> service.recordDonation(conn, DONOR_ID, CARD_ID, "Central Station", 450, STAFF_ID));
        assertTrue(exception.getMessage().contains("donate"));
        verify(donationRepository, never()).insert(any(), any());
    }

    @Test
    @DisplayName("card belonging to another donor is rejected")
    void cardBelongsToAnotherDonor() {
        Donor donor = donorWithId(DONOR_ID);
        DonorCard card = donorCard(CARD_ID, DONOR_ID + 1, true);
        User staff = user(STAFF_ID, true);

        Connection conn = mock(Connection.class);
        when(donorService.findById(any(), eq(DONOR_ID))).thenReturn(Optional.of(donor));
        when(cardService.findById(any(), eq(CARD_ID))).thenReturn(Optional.of(card));
        when(authService.findById(any(), eq(STAFF_ID))).thenReturn(Optional.of(staff));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.recordDonation(conn, DONOR_ID, CARD_ID, "Central Station", 450, STAFF_ID));
        assertTrue(exception.getMessage().contains("does not belong"));
        verify(donationRepository, never()).insert(any(), any());
    }

    @Test
    @DisplayName("inactive card is rejected")
    void inactiveCardRejected() {
        Donor donor = donorWithId(DONOR_ID);
        DonorCard card = donorCard(CARD_ID, DONOR_ID, false);
        User staff = user(STAFF_ID, true);

        Connection conn = mock(Connection.class);
        when(donorService.findById(any(), eq(DONOR_ID))).thenReturn(Optional.of(donor));
        when(cardService.findById(any(), eq(CARD_ID))).thenReturn(Optional.of(card));
        when(authService.findById(any(), eq(STAFF_ID))).thenReturn(Optional.of(staff));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.recordDonation(conn, DONOR_ID, CARD_ID, "Central Station", 450, STAFF_ID));
        assertTrue(exception.getMessage().contains("not active"));
        verify(donationRepository, never()).insert(any(), any());
    }

    @Test
    @DisplayName("invalid volume is rejected before saving")
    void invalidVolumeRejected() {
        Donor donor = donorWithId(DONOR_ID);
        DonorCard card = donorCard(CARD_ID, DONOR_ID, true);
        User staff = user(STAFF_ID, true);

        Connection conn = mock(Connection.class);
        when(donorService.findById(any(), eq(DONOR_ID))).thenReturn(Optional.of(donor));
        when(cardService.findById(any(), eq(CARD_ID))).thenReturn(Optional.of(card));
        when(authService.findById(any(), eq(STAFF_ID))).thenReturn(Optional.of(staff));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.recordDonation(conn, DONOR_ID, CARD_ID, "Central Station", 50, STAFF_ID));
        assertTrue(exception.getMessage().contains("between 100 ml and 700 ml"));
        verify(donationRepository, never()).insert(any(), any());
    }

    private Donor donorWithId(long donorId) {
        Donor donor = new Donor();
        donor.setDonorId(donorId);
        return donor;
    }

    private DonorCard donorCard(long cardId, long donorId, boolean active) {
        DonorCard card = new DonorCard();
        card.setCardId(cardId);
        card.setDonorId(donorId);
        card.setStatus(active ? com.bdcms.feature.card.CardStatus.ACTIVE : com.bdcms.feature.card.CardStatus.INACTIVE);
        card.setCardNo("CARD-" + cardId);
        return card;
    }

    private User user(long userId, boolean active) {
        User user = new User();
        user.setUserId(userId);
        user.setFullName("Staff Member");
        user.setActive(active);
        return user;
    }

    private EligibilityResult eligibleResult(LocalDate lastDonation, int intervalDays) {
        return EligibilityResult.eligible(lastDonation, lastDonation == null ? -1 : 84,
                intervalDays, FIXED_DATE, FIXED_DATE);
    }

    private EligibilityResult notEligibleResult(LocalDate lastDonation, int intervalDays, LocalDate nextPermitted) {
        return EligibilityResult.notEligible(lastDonation, lastDonation == null ? -1 : 83,
                intervalDays, nextPermitted, FIXED_DATE);
    }
}
