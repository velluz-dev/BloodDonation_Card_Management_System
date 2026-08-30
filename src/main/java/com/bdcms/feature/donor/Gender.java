package com.bdcms.feature.donor;

import java.util.Optional;

/** Donor gender as recorded at registration (FR-2). Mirrors the {@code donors.gender} enum. */
public enum Gender {

    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Bundle key for this value's display name, e.g. {@code gender.MALE}.
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
        return "gender." + name();
    }


    public static Optional<Gender> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Gender.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
