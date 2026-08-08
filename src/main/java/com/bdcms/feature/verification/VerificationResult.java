package com.bdcms.feature.verification;

/**
 * Why a verification attempt succeeded or failed (FR-4).
 *
 * <p>Every one of these values is written to {@code verification_log}, so the
 * audit trail records not merely that an attempt failed but how - which is the
 * difference between noticing a scanner problem and noticing someone trying
 * card numbers at the counter.
 */
public enum VerificationResult {

    /** Card valid, staff confirmed the photograph, PIN correct if one was required. */
    SUCCESS("Verified", "success"),

    /** No card with that number exists. */
    CARD_NOT_FOUND("Card not found", "danger"),

    /** The card exists but has been reported lost or superseded by a reissue. */
    CARD_INACTIVE("Card not active", "warning"),

    /** The donor's four-digit PIN did not match. */
    PIN_MISMATCH("PIN incorrect", "danger"),

    /** Staff did not tick the confirmation box - the identity check was not completed. */
    NOT_CONFIRMED("Not confirmed by staff", "secondary");

    private final String label;
    private final String badgeClass;

    VerificationResult(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
