package com.bdcms.feature.eligibility;

/** Outcome of the eligibility check (FR-5). */
public enum EligibilityStatus {

    /** The required interval has elapsed - or this is a first donation. */
    ELIGIBLE("Eligible", "success"),

    /** Too soon since the last donation. Donating now would risk the donor's health. */
    NOT_ELIGIBLE("Not eligible", "danger");

    private final String label;
    private final String badgeClass;

    EligibilityStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Bundle key for this value's display name, e.g. {@code eligibility.ELIGIBLE}.
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
        return "eligibility." + name();
    }


    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isEligible() {
        return this == ELIGIBLE;
    }
}
