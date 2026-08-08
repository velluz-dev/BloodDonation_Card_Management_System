package com.bdcms.feature.auth;

/** Access role of a staff account (FR-1). Mirrors the {@code users.role} enum. */
public enum Role {

    /** Full access, including reports, user management and system settings. */
    ADMIN("Administrator"),

    /** Day-to-day counter work: registration, cards, verification, donations. */
    STAFF("Staff");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Role of(String value) {
        return value == null ? STAFF : Role.valueOf(value.trim().toUpperCase());
    }
}
