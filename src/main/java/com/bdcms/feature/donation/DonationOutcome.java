package com.bdcms.feature.donation;

import com.bdcms.feature.eligibility.EligibilityResult;
import com.bdcms.feature.verification.VerificationResult;

/**
 * What happened when staff tried to record a donation (FR-6).
 *
 * <p>Three mutually exclusive endings, and the evidence for each:
 * <ul>
 *   <li>{@code RECORDED} - identity verified, interval satisfied, row written;</li>
 *   <li>{@code VERIFICATION_FAILED} - carries the {@link VerificationResult}
 *       saying which step of the identity check failed;</li>
 *   <li>{@code NOT_ELIGIBLE} - carries the {@link EligibilityResult}, which
 *       includes the earliest date the donor may return.</li>
 * </ul>
 */
public record DonationOutcome(
        Status status,
        Donation donation,
        VerificationResult verification,
        EligibilityResult eligibility,
        String message) {

    public enum Status {
        RECORDED,
        VERIFICATION_FAILED,
        NOT_ELIGIBLE,
        INVALID
    }

    public boolean isRecorded() {
        return status == Status.RECORDED;
    }

    static DonationOutcome recorded(Donation donation, EligibilityResult eligibility) {
        return new DonationOutcome(Status.RECORDED, donation, VerificationResult.SUCCESS, eligibility,
                "Donation recorded successfully.");
    }

    static DonationOutcome verificationFailed(VerificationResult result, String message) {
        return new DonationOutcome(Status.VERIFICATION_FAILED, null, result, null, message);
    }

    static DonationOutcome notEligible(EligibilityResult eligibility) {
        return new DonationOutcome(Status.NOT_ELIGIBLE, null, VerificationResult.SUCCESS, eligibility,
                "Identity verified, but the donor is not yet eligible. "
                        + eligibility.getExplanation());
    }

    static DonationOutcome invalid(String message) {
        return new DonationOutcome(Status.INVALID, null, null, null, message);
    }
}
