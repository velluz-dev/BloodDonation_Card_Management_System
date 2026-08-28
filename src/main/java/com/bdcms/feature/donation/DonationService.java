package com.bdcms.feature.donation;

import com.bdcms.feature.card.CardService;
import com.bdcms.feature.card.DonorCard;
import com.bdcms.feature.eligibility.EligibilityResult;
import com.bdcms.feature.eligibility.EligibilityService;
import com.bdcms.feature.verification.VerificationResult;
import com.bdcms.feature.verification.VerificationService;
import com.bdcms.shared.db.Tx;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Recording a donation (FR-6) - the one place where four features meet.
 *
 * <p>This service sits at the top of the dependency graph. It composes the card,
 * verification and eligibility features through their <em>services</em>; it
 * never reaches into another feature's DAO, and no other feature imports it.
 *
 * <p><strong>The transaction.</strong> The reliability requirement states that
 * verification, the eligibility check and the insertion must all succeed or all
 * roll back. That is achievable only because every DAO method in the system
 * takes a {@link Connection}: {@link Tx#inTransaction} opens one connection,
 * and card lookup, verification logging, the last-donation query and the insert
 * all run on it. A failure anywhere unwinds the lot.
 *
 * <p><strong>Why a refusal is returned and never thrown.</strong> A failed
 * verification must survive, or the audit trail would record only the attempts
 * that went well - exactly backwards. That is achieved by the return type
 * rather than by a second transaction: a refusal comes back as a
 * {@link DonationOutcome}, so the block returns normally, the
 * {@code verification_log} row commits, and the donation is simply never
 * inserted. Throwing here would roll the audit row back with everything else.
 *
 * <p>A genuine fault - a dropped connection, a constraint violation - still
 * throws, and then the whole unit unwinds including the log row. That is also
 * correct: in that case nothing happened at all.
 */
public class DonationService {

    private final DonationDao donationDao;
    private final CardService cardService;
    private final VerificationService verificationService;
    private final EligibilityService eligibilityService;

    public DonationService() {
        this(new DonationDao(), new CardService(), new VerificationService(), new EligibilityService());
    }

    public DonationService(DonationDao donationDao, CardService cardService,
                           VerificationService verificationService,
                           EligibilityService eligibilityService) {
        this.donationDao = donationDao;
        this.cardService = cardService;
        this.verificationService = verificationService;
        this.eligibilityService = eligibilityService;
    }

    /**
     * Verifies the donor, checks eligibility and records the donation - all or
     * nothing.
     *
     * @param cardNo         card number scanned or typed at the counter
     * @param staffId        the signed-in staff member, recorded as accountable
     * @param staffConfirmed whether staff explicitly confirmed photo and masked ID
     * @param pin            the donor's four-digit PIN, when they set one
     */
    public DonationOutcome record(String cardNo, long staffId, boolean staffConfirmed,
                                  String pin, String centre, int volumeMl, LocalDate donationDate) {

        if (cardNo == null || cardNo.isBlank()) {
            return DonationOutcome.invalid("A card number is required.");
        }
        if (centre == null || centre.isBlank()) {
            return DonationOutcome.invalid("The donation centre is required.");
        }
        if (volumeMl < 100 || volumeMl > 700) {
            return DonationOutcome.invalid("Volume must be between 100 ml and 700 ml.");
        }
        LocalDate date = donationDate == null ? LocalDate.now() : donationDate;
        if (date.isAfter(LocalDate.now())) {
            return DonationOutcome.invalid("A donation cannot be dated in the future.");
        }

        // A refused attempt is returned, never thrown. That distinction is what
        // keeps the audit trail intact: returning normally commits the
        // verification_log row written inside the block, while the donation
        // simply never gets inserted. Only a genuine fault - a dropped
        // connection, a constraint violation - throws, and then the whole unit
        // including the log row unwinds, which is correct because nothing
        // happened at all.
        return Tx.inTransaction(conn ->
                recordInTransaction(conn, cardNo, staffId, staffConfirmed, pin, centre, volumeMl, date));
    }

    /** The whole flow on a single connection. Any exception unwinds every step. */
    private DonationOutcome recordInTransaction(Connection conn, String cardNo, long staffId,
                                                boolean staffConfirmed, String pin, String centre,
                                                int volumeMl, LocalDate date) {

        // 1. Identity verification. This resolves the card, checks its status,
        //    the staff confirmation and the PIN, and writes the audit row for
        //    whatever it finds - all on this connection.
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

        // 2. Verification succeeded, so the card is present and usable.
        Optional<DonorCard> card = cardService.findByCardNo(conn, cardNo);
        if (card.isEmpty()) {
            return DonationOutcome.verificationFailed(VerificationResult.CARD_NOT_FOUND,
                    "No card matches that number. Check the card and try again.");
        }
        long donorId = card.get().getDonorId();

        // 3. Eligibility, computed from the history read on this same connection.
        LocalDate lastDonation = donationDao.findLastDonationDate(conn, donorId).orElse(null);
        EligibilityResult eligibility = eligibilityService.evaluate(conn, lastDonation);
        if (!eligibility.isEligible()) {
            // Identity was genuinely verified, so that audit row is correct and
            // must persist - but nothing may be inserted. Returning normally
            // commits the verification row without the donation.
            return DonationOutcome.notEligible(eligibility);
        }

        // 4. Append the donation.
        Donation donation = new Donation();
        donation.setDonorId(donorId);
        donation.setCardId(card.get().getCardId());
        donation.setDonationDate(date);
        donation.setCentre(centre.trim());
        donation.setVolumeMl(volumeMl);
        donation.setVerifiedBy(staffId);
        donationDao.insert(conn, donation);

        donation.setCardNo(card.get().getCardNo());
        return DonationOutcome.recorded(donation, eligibility);
    }

    // ---- history and dashboard queries -------------------------------------

    /** A donor's complete chronological history (FR-6). */
    public List<Donation> historyForDonor(long donorId) {
        return Tx.read(conn -> donationDao.findByDonor(conn, donorId));
    }

    /** How many donations a donor has, joining the caller's transaction. */
    public int countForDonor(Connection conn, long donorId) {
        return donationDao.countByDonor(conn, donorId);
    }

    public List<Donation> recent(int limit) {
        return Tx.read(conn -> donationDao.findRecent(conn, limit));
    }

    public int countSince(LocalDate from) {
        return Tx.read(conn -> donationDao.countSince(conn, from));
    }

    /**
     * Eligibility for a donor right now, without recording anything.
     * Used by the history screen to show when the donor may next donate.
     */
    public EligibilityResult eligibilityFor(long donorId) {
        return Tx.read(conn -> {
            LocalDate last = donationDao.findLastDonationDate(conn, donorId).orElse(null);
            return eligibilityService.evaluate(conn, last);
        });
    }
}
