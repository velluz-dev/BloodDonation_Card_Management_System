package com.bdcms.feature.verification;

/**
 * Masks a national identification number for display (FR-4).
 *
 * <p>The verification screen has to show staff <em>enough</em> of the ID to
 * check it against the document in the donor's hand, but not so much that the
 * screen becomes a way to harvest identity numbers - anyone glancing at the
 * counter, or a screenshot, would otherwise expose them in full.
 *
 * <p>The rule keeps the first character and the last four, replacing everything
 * between with bullets, exactly as the proposal illustrates:
 * {@code S1234567A} becomes {@code S••••567A}.
 *
 * <p>Short values are handled defensively: anything too short to mask
 * meaningfully is returned fully masked rather than partly revealed.
 */
public final class IdMasker {

    private static final char BULLET = '•';
    private static final int VISIBLE_TAIL = 4;

    private IdMasker() {
    }

    /**
     * Masks {@code nationalId} for display.
     *
     * @return the masked form, or an empty string when there is nothing to mask
     */
    public static String mask(String nationalId) {
        if (nationalId == null) {
            return "";
        }
        String value = nationalId.trim();
        if (value.isEmpty()) {
            return "";
        }
        // Too short for a leading character plus a four-character tail plus at
        // least one masked character: reveal nothing.
        if (value.length() <= VISIBLE_TAIL + 1) {
            return String.valueOf(BULLET).repeat(value.length());
        }

        String head = value.substring(0, 1);
        String tail = value.substring(value.length() - VISIBLE_TAIL);
        int maskedCount = value.length() - 1 - VISIBLE_TAIL;
        return head + String.valueOf(BULLET).repeat(maskedCount) + tail;
    }
}
