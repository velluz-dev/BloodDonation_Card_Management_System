package com.bdcms.feature.donation;

import com.bdcms.feature.auth.AuthService;
import com.bdcms.feature.auth.User;
import com.bdcms.feature.card.CardService;
import com.bdcms.feature.card.DonorCard;
import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.DonorService;
import com.bdcms.feature.eligibility.EligibilityResult;
import com.bdcms.feature.eligibility.EligibilityService;
import com.bdcms.feature.verification.VerificationResult;
import com.bdcms.feature.verification.VerificationService;
import com.bdcms.shared.db.Tx;

import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Recording a donation (FR-6) - the one place where four features meet.
 */
public class DonationService {

    private static final int MIN_VOLUME_ML = 100;
    private static final int MAX_VOLUME_ML = 700;

    private final DonationRepository donationRepository;
    private final CardService cardService;
    private final VerificationService verificationService;
    private final EligibilityService eligibilityService;
    private final DonorService donorService;
    private final AuthService authService;
    private final Clock clock;

    public DonationService() {
        this(new DonationRepository(), new CardService(), new VerificationService(),
                new EligibilityService(), new DonorService(), new AuthService(), Clock.systemDefaultZone());
    }

    public DonationService(DonationRepository donationRepository, CardService cardService,
                           VerificationService verificationService,
                           EligibilityService eligibilityService,
                           DonorService donorService,
                           AuthService authService,
                           Clock clock) {
        this.donationRepository = donationRepository;
        this.cardService = cardService;
        this.verificationService = verificationService;
        this.eligibilityService = eligibilityService;
        this.donorService = donorService;
        this.authService = authService;
        this.clock = clock;
    }

    /**
     * Verifies the donor, checks eligibility and records the donation - all or
     * nothing.
     */
    public DonationOutcome record(String cardNo, long staffId, boolean staffConfirmed,
                                  String pin, String centre, int volumeMl, LocalDate donationDate) {

        if (cardNo == null || cardNo.isBlank()) {
            return DonationOutcome.invalid("A card number is required.");
        }
        if (centre == null || centre.isBlank()) {
            return DonationOutcome.invalid("The donation centre is required.");
        }
        if (volumeMl < MIN_VOLUME_ML || volumeMl > MAX_VOLUME_ML) {
            return DonationOutcome.invalid("Volume must be between " + MIN_VOLUME_ML + " ml and " + MAX_VOLUME_ML + " ml.");
        }
        LocalDate date = donationDate == null ? LocalDate.now(clock) : donationDate;
        if (date.isAfter(LocalDate.now(clock))) {
            return DonationOutcome.invalid("A donation cannot be dated in the future.");
        }

        return Tx.inTransaction(conn ->
                recordInTransaction(conn, cardNo, staffId, staffConfirmed, pin, centre, volumeMl, date));
    }

    /** The whole flow on a single connection. Any exception unwinds every step. */
    private DonationOutcome recordInTransaction(Connection conn, String cardNo, long staffId,
                                                boolean staffConfirmed, String pin, String centre,
                                                int volumeMl, LocalDate date) {

        VerificationResult verification =
                verificationService.verify(conn, cardNo, staffId, staffConfirmed, pin);
        if (!verification.isSuccess()) {
            return DonationOutcome.verificationFailed(verification,
                    switch (verification) {
                        case CARD_NOT_FOUND -> "No card matches that number. "
                                + "Check the card and try again.";
                        case CARD_INACTIVE -> "This card is no longer active. Issue a replacement first.";
                        case NOT_CONFIRMED -> "You must confirm the photograph and masked ID "
                                + "before a donation can be recorded.";
                        case PIN_MISMATCH -> "The PIN entered does not match this donor's record.";
                        default -> "Identity could not be verified.";
                    });
        }

        Optional<DonorCard> card = cardService.findByCardNo(conn, cardNo);
        if (card.isEmpty()) {
            return DonationOutcome.verificationFailed(VerificationResult.CARD_NOT_FOUND,
                    "No card matches that number. Check the card and try again.");
        }
        long donorId = card.get().getDonorId();

        LocalDate lastDonation = donationRepository.findLastDonationDate(conn, donorId).orElse(null);
        EligibilityResult eligibility = eligibilityService.evaluate(conn, lastDonation);
        if (!eligibility.isEligible()) {
            return DonationOutcome.notEligible(eligibility);
        }

        Donation donation = new Donation();
        donation.setDonorId(donorId);
        donation.setCardId(card.get().getCardId());
        donation.setDonationDate(date);
        donation.setCentre(centre.trim());
        donation.setVolumeMl(volumeMl);
        donation.setVerifiedBy(staffId);
        donationRepository.insert(conn, donation);

        donation.setCardNo(card.get().getCardNo());
        return DonationOutcome.recorded(donation, eligibility);
    }

    public Donation recordDonation(long donorId, long cardId, String centre,
                                   int volumeMl, long staffUserId) {
        return Tx.inTransaction(conn -> recordDonation(conn, donorId, cardId, centre, volumeMl, staffUserId));
    }

    Donation recordDonation(Connection conn, long donorId, long cardId, String centre,
                            int volumeMl, long staffUserId) {
        Donor donor = donorService.findById(conn, donorId)
                .orElseThrow(() -> new IllegalArgumentException("Donor not found."));
        DonorCard card = cardService.findById(conn, cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found."));
        if (card.getDonorId() != donor.getDonorId()) {
            throw new IllegalArgumentException("Card does not belong to donor.");
        }
        if (!card.isUsable()) {
            throw new IllegalArgumentException("Card is not active.");
        }
        if (centre == null || centre.isBlank()) {
            throw new IllegalArgumentException("Donation centre is required.");
        }
        if (volumeMl < MIN_VOLUME_ML || volumeMl > MAX_VOLUME_ML) {
            throw new IllegalArgumentException("Donation volume must be between "
                    + MIN_VOLUME_ML + " ml and " + MAX_VOLUME_ML + " ml.");
        }
        User staff = authService.findById(conn, staffUserId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found."));
        if (!staff.isActive()) {
            throw new IllegalArgumentException("Staff user is not authorized.");
        }

        LocalDate lastDonation = donationRepository.findLastDonationDate(conn, donorId).orElse(null);
        EligibilityResult eligibility = eligibilityService.evaluate(conn, lastDonation);
        if (!eligibility.isEligible()) {
            throw new DonorNotEligibleException(eligibility.daysRemaining(), eligibility.nextPermittedDate(),
                    eligibility.getExplanation());
        }

        Donation donation = new Donation();
        donation.setDonorId(donorId);
        donation.setCardId(cardId);
        donation.setDonationDate(LocalDate.now(clock));
        donation.setCentre(centre.trim());
        donation.setVolumeMl(volumeMl);
        donation.setVerifiedBy(staffUserId);
        donationRepository.insert(conn, donation);
        return donation;
    }

    /** A donor's complete chronological history (FR-6). */
    public List<Donation> historyForDonor(long donorId) {
        return Tx.read(conn -> donationRepository.findByDonor(conn, donorId));
    }

    public List<Donation> recent(int limit) {
        return Tx.read(conn -> donationRepository.findRecent(conn, limit));
    }

    public int countSince(LocalDate from) {
        return Tx.read(conn -> donationRepository.countSince(conn, from));
    }

    /**
     * Eligibility for a donor right now, without recording anything.
     * Used by the history screen to show when the donor may next donate.
     */
    public EligibilityResult eligibilityFor(long donorId) {
        return Tx.read(conn -> {
            LocalDate last = donationRepository.findLastDonationDate(conn, donorId).orElse(null);
            return eligibilityService.evaluate(conn, last);
        });
    }
}
