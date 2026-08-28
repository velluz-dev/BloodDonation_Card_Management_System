package com.bdcms.feature.admin;

import com.bdcms.feature.card.CardService;
import com.bdcms.feature.donation.DonationService;
import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.DonorService;
import com.bdcms.feature.donor.PhotoStore;
import com.bdcms.feature.verification.VerificationService;
import com.bdcms.shared.db.Tx;

import java.util.Optional;

/**
 * Destructive data operations, restricted to administrators.
 *
 * <p>Like the dashboard, this sits at the top of the dependency graph: it
 * composes four features through their services and nothing depends on it. It
 * is the only place in the system that deletes donor data, which is why the
 * rules live here rather than scattered across the features that own each table.
 *
 * <h2>Two rules the deletion obeys</h2>
 *
 * <p><strong>A donor with donations cannot be deleted.</strong> Donation history
 * is append-only because the eligibility rule is computed from it; removing a
 * donor would either orphan those rows or destroy them, and a donation that
 * happened must stay recorded. Such a request is refused and the count reported.
 *
 * <p><strong>The audit trail is kept, not deleted.</strong> Verification
 * attempts are not removed - their {@code donor_id} is set to null. Every row
 * keeps its card number, outcome, staff account and timestamp, so what happened
 * at the counter is still auditable, while the link to a person no longer in the
 * system is gone. {@code verification_log.donor_id} is nullable precisely so
 * this is possible.
 *
 * <p>The whole deletion is one transaction: either the donor, their cards and
 * the detached audit rows all settle together, or nothing changes.
 */
public class DataAdminService {

    private final DonorService donorService;
    private final CardService cardService;
    private final DonationService donationService;
    private final VerificationService verificationService;

    public DataAdminService() {
        this(new DonorService(), new CardService(), new DonationService(), new VerificationService());
    }

    public DataAdminService(DonorService donorService, CardService cardService,
                            DonationService donationService,
                            VerificationService verificationService) {
        this.donorService = donorService;
        this.cardService = cardService;
        this.donationService = donationService;
        this.verificationService = verificationService;
    }

    /**
     * Deletes a donor, their cards and their photograph, keeping the audit trail.
     *
     * <p>The donation count is read inside the same transaction as the deletes,
     * so a donation recorded a moment earlier cannot slip through the guard.
     */
    public DonorDeletion deleteDonor(long donorId) {
        // Photo path is captured inside the transaction and the file removed
        // after it commits: deleting the image first would leave a donor with a
        // dangling photo_path if the transaction then rolled back.
        String[] photoPath = new String[1];

        DonorDeletion outcome = Tx.inTransaction(conn -> {
            Optional<Donor> found = donorService.findById(conn, donorId);
            if (found.isEmpty()) {
                return DonorDeletion.notFound();
            }
            Donor donor = found.get();

            int donations = donationService.countForDonor(conn, donorId);
            if (donations > 0) {
                return DonorDeletion.hasDonations(donor.getFullName(), donations);
            }

            int auditRows = verificationService.detachDonor(conn, donorId);
            int cards = cardService.deleteAllForDonor(conn, donorId);
            donorService.delete(conn, donorId);

            photoPath[0] = donor.getPhotoPath();
            return DonorDeletion.deleted(donor.getFullName(), cards, auditRows);
        });

        if (outcome.isDeleted() && photoPath[0] != null) {
            PhotoStore.delete(photoPath[0]);
        }
        return outcome;
    }
}
