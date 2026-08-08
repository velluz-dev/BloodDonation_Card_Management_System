package com.bdcms.feature.card;

import java.util.Optional;

/**
 * Lifecycle state of an issued donor card (FR-3).
 *
 * <p>Only {@link #ACTIVE} may be presented at the counter. A card that is lost
 * or superseded stops working immediately, but the donations recorded against
 * it stay attached to the <em>donor</em>, so no history is lost when a card is
 * replaced.
 */
public enum CardStatus {

    /** Currently valid. At most one per donor. */
    ACTIVE("Active", "success"),

    /** Superseded by a reissue, or deliberately withdrawn. */
    INACTIVE("Inactive", "secondary"),

    /** Reported lost or stolen by the donor. */
    LOST("Lost", "danger");

    private final String label;
    private final String badgeClass;

    CardStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    /** Bootstrap contextual class used by the views. */
    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isUsable() {
        return this == ACTIVE;
    }

    public static Optional<CardStatus> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(CardStatus.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
