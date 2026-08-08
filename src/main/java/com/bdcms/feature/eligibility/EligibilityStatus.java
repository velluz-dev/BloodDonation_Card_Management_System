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

    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isEligible() {
        return this == ELIGIBLE;
    }
}
