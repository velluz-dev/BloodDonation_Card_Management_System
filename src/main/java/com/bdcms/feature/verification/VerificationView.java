package com.bdcms.feature.verification;

import com.bdcms.feature.card.DonorCard;
import com.bdcms.feature.donor.Donor;

/**
 * What the verification screen is allowed to show about a scanned card (FR-4).
 *
 * <p>A deliberately narrow view rather than the raw {@link Donor}: the national
 * ID arrives already masked, and the PIN hash is not carried at all. If the
 * screen cannot obtain the full ID, no amount of JSP carelessness can leak it.
 *
 * @param found      whether a card with that number exists
 * @param card       the card, when found
 * @param donor      the donor the card belongs to, when found
 * @param maskedId   the donor's national ID with the middle masked
 * @param pinRequired whether this donor set a PIN at registration
 * @param problem    why the card cannot be used, or null when it can
 */
public record VerificationView(
        boolean found,
        DonorCard card,
        Donor donor,
        String maskedId,
        boolean pinRequired,
        VerificationResult problem) {

    /** True when the card exists and is usable, so staff may proceed to confirm. */
    public boolean isUsable() {
        return found && problem == null;
    }

    // ---- JavaBean accessors for JSP/EL -------------------------------------
    // Tomcat 10.1 implements Jakarta EL 5.0, which cannot read record
    // components as properties (added in EL 6.0). See SessionUser for the note.

    public boolean isFound() {
        return found;
    }

    public DonorCard getCard() {
        return card;
    }

    public Donor getDonor() {
        return donor;
    }

    public String getMaskedId() {
        return maskedId;
    }

    public boolean isPinRequired() {
        return pinRequired;
    }

    public VerificationResult getProblem() {
        return problem;
    }

    public static VerificationView notFound() {
        return new VerificationView(false, null, null, null, false,
                VerificationResult.CARD_NOT_FOUND);
    }

    public static VerificationView unusable(DonorCard card, Donor donor, VerificationResult problem) {
        return new VerificationView(true, card, donor, IdMasker.mask(donor.getNationalId()),
                donor.isPinSet(), problem);
    }

    public static VerificationView usable(DonorCard card, Donor donor) {
        return new VerificationView(true, card, donor, IdMasker.mask(donor.getNationalId()),
                donor.isPinSet(), null);
    }
}
