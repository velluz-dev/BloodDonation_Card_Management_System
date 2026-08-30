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

    /**
     * Bundle key for this value's display name, e.g. {@code role.ADMIN}.
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
        return "role." + name();
    }


    public static Role of(String value) {
        return value == null ? STAFF : Role.valueOf(value.trim().toUpperCase());
    }
}
