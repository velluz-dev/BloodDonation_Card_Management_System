package com.bdcms.feature.verification;

import com.bdcms.feature.auth.Passwords;
import com.bdcms.feature.card.CardService;
import com.bdcms.feature.card.DonorCard;
import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.DonorService;
import com.bdcms.shared.db.Tx;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Donor identity verification (FR-4) - the mechanism at the centre of the system.
 *
 * <p>The workflow the proposal specifies is enforced here rather than left to
 * the screen:
 * <ol>
 *   <li>the card number is scanned or typed and resolved to a donor;</li>
 *   <li>the stored photograph and a masked national ID are shown;</li>
 *   <li>staff make an <em>explicit</em> confirmation - it is a required input,
 *       not a default;</li>
 *   <li>a PIN is demanded as a second factor when the donor set one;</li>
 *   <li>the attempt is written to the audit log whatever the outcome.</li>
 * </ol>
 *
 * <p>Nothing downstream can skip a step, because the donation feature does not
 * record a donation until this service returns {@link VerificationResult#SUCCESS}.
 */
public class VerificationService {

    private final CardService cardService;
    private final DonorService donorService;
    private final VerificationLogDao logDao;

    public VerificationService() {
        this(new CardService(), new DonorService(), new VerificationLogDao());
    }

    public VerificationService(CardService cardService, DonorService donorService,
                               VerificationLogDao logDao) {
        this.cardService = cardService;
        this.donorService = donorService;
        this.logDao = logDao;
    }

    // ---- step 1 and 2: resolve the card, prepare what may be shown ---------

    /**
     * Resolves a scanned or typed card number to the donor behind it.
     *
     * <p>Read-only and unlogged. A lookup is not yet an assertion that this
     * person is the donor - logging every keystroke-corrected scan would bury
     * the real verification events. The attempt is recorded at
     * {@link #verify}, which is where the claim is actually made.
     */
    public VerificationView lookup(String cardNo) {
        return Tx.read(conn -> lookup(conn, cardNo));
    }

    /** As above, joining an existing transaction. */
    public VerificationView lookup(Connection conn, String cardNo) {
        Optional<DonorCard> card = cardService.findByCardNo(conn, cardNo);
        if (card.isEmpty()) {
            return VerificationView.notFound();
        }
        Optional<Donor> donor = donorService.findById(card.get().getDonorId());
        if (donor.isEmpty()) {
            return VerificationView.notFound();
        }
        if (!card.get().isUsable()) {
            return VerificationView.unusable(card.get(), donor.get(), VerificationResult.CARD_INACTIVE);
        }
        return VerificationView.usable(card.get(), donor.get());
    }

    // ---- step 3 to 5: the verification decision itself ----------------------

    /**
     * Performs the verification and records the attempt.
     *
     * <p>Runs on the caller's connection so it can be part of the same
     * transaction that records a donation. Note the ordering: the log row is
     * written for every outcome before the result is returned, so there is no
     * path through this method that decides something without leaving a trace.
     *
     * @param staffConfirmed whether staff explicitly confirmed the photograph
     *                       and masked ID against the person in front of them
     * @param submittedPin   the PIN typed by the donor, or null if none was asked for
     */
    public VerificationResult verify(Connection conn, String cardNo, long staffId,
                                     boolean staffConfirmed, String submittedPin) {
        return verify(conn, cardNo, staffId, staffConfirmed, submittedPin, null);
    }

    /**
     * As above, additionally recording what the assisted face check reported.
     *
     * <p><strong>{@code faceDistance} is an observation, not evidence.</strong>
     * It is computed in the operator's browser, so it could be any number a
     * determined user chose to send. Nothing here branches on it: it is written
     * into the audit row so the trail shows what the operator was looking at
     * when they confirmed, and that is its entire purpose. The decision remains
     * the staff tick, and the accountability remains with the named account
     * that made it.
     *
     * @param faceDistance Euclidean descriptor distance, smaller being more
     *                     alike, or null when the check was not run
     */
    public VerificationResult verify(Connection conn, String cardNo, long staffId,
                                     boolean staffConfirmed, String submittedPin,
                                     Double faceDistance) {

        VerificationView view = lookup(conn, cardNo);

        if (!view.found()) {
            return record(conn, cardNo, null, staffId, VerificationResult.CARD_NOT_FOUND,
                    "No card matches the number presented");
        }
        long donorId = view.donor().getDonorId();

        if (view.problem() != null) {
            return record(conn, view.card().getCardNo(), donorId, staffId, view.problem(),
                    "Card status is " + view.card().getStatus().getLabel());
        }
        if (!staffConfirmed) {
            return record(conn, view.card().getCardNo(), donorId, staffId,
                    VerificationResult.NOT_CONFIRMED,
                    "Staff did not confirm the photograph and masked ID");
        }
        if (view.pinRequired() && !Passwords.verify(submittedPin, view.donor().getPinHash())) {
            return record(conn, view.card().getCardNo(), donorId, staffId,
                    VerificationResult.PIN_MISMATCH,
                    "Second-factor PIN did not match");
        }
        return record(conn, view.card().getCardNo(), donorId, staffId,
                VerificationResult.SUCCESS,
                "Photograph and masked ID confirmed by staff"
                        + (view.pinRequired() ? "; PIN verified" : "")
                        + faceNote(faceDistance));
    }

    /**
     * Records a verification attempt outside any wider transaction.
     *
     * <p>For callers that verify identity on its own - not as part of recording
     * a donation. The donation flow does <em>not</em> use this: it passes its
     * own connection to {@link #verify} so the attempt is logged in the same
     * unit of work.
     */
    public void recordIndependently(String cardNo, Long donorId, long staffId,
                                    VerificationResult result, String detail) {
        Tx.inTransaction(conn -> record(conn, cardNo, donorId, staffId, result, detail));
    }

    /**
     * Severs a deleted donor from their audit rows without removing any of
     * them, joining the caller's transaction. See
     * {@link VerificationLogDao#detachDonor}.
     */
    public int detachDonor(Connection conn, long donorId) {
        return logDao.detachDonor(conn, donorId);
    }

    // ---- audit trail queries ------------------------------------------------

    public List<VerificationLog> recentAttempts(int limit) {
        return Tx.read(conn -> logDao.search(conn, null, null, null, limit, 0));
    }

    public List<VerificationLog> auditTrail(Long donorId, LocalDate from, LocalDate to,
                                            int limit, int offset) {
        return Tx.read(conn -> logDao.search(conn, donorId, from, to, limit, offset));
    }

    public int failedAttemptsSince(LocalDate from) {
        return Tx.read(conn -> logDao.countSince(conn, from, true));
    }

    public int attemptsSince(LocalDate from) {
        return Tx.read(conn -> logDao.countSince(conn, from, false));
    }

    /** Attempts per outcome over a period, every outcome present even at zero. */
    public java.util.Map<VerificationResult, Integer> outcomeCounts(LocalDate from, LocalDate to) {
        return Tx.read(conn -> logDao.countByResult(conn, from, to));
    }

    /**
     * Renders the advisory reading for the audit row, or nothing when the check
     * was not run. Always marked "advisory" so a later reader cannot mistake it
     * for something the system relied on.
     */
    private String faceNote(Double faceDistance) {
        if (faceDistance == null) {
            return "";
        }
        return String.format("; face check %.3f (advisory, not relied on)", faceDistance);
    }

    private VerificationResult record(Connection conn, String cardNo, Long donorId, long staffId,
                                      VerificationResult result, String detail) {
        VerificationLog log = new VerificationLog();
        log.setCardNo(cardNo);
        log.setDonorId(donorId);
        log.setStaffId(staffId);
        log.setResult(result);
        log.setDetail(detail);
        logDao.insert(conn, log);
        return result;
    }
}
