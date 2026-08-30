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

    /**
     * Bundle key for this value's display name, e.g. {@code result.SUCCESS}.
     *
     * <p>JSPs render it with {@code <fmt:message key="${x.messageKey}"/>} rather
     * than reading {@link #getLabel()}, so a badge follows the language
     * switcher. The enum itself stays free of any locale handling - it names a
     * key, and the bundle decides what that says.
     *
     * <p>{@link #getLabel()} remains the English text, and is what PDFs and log
     * lines use: a generated card is a physical artefact and a log line is read
     * by whoever is on call, so neither should shift with a viewer's setting.
     */
    public String getMessageKey() {
        return "result." + name();
    }


    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
