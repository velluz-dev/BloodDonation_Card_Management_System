package com.bdcms.feature.donor;

import java.util.Optional;

/**
 * ABO/Rh blood group (FR-2).
 *
 * <p>Stored in the database as {@code A_POS} rather than {@code A+} because the
 * {@code +} and {@code -} characters are awkward in enum names and in URLs; the
 * human-readable form is produced by {@link #getLabel()} for the card, the
 * verification screen and the reports.
 */
public enum BloodGroup {

    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Parses either the stored name ({@code A_POS}) or the display form ({@code A+}). */
    public static Optional<BloodGroup> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim().toUpperCase();
        for (BloodGroup group : values()) {
            if (group.name().equals(trimmed) || group.label.equalsIgnoreCase(trimmed)) {
                return Optional.of(group);
            }
        }
        return Optional.empty();
    }
}
