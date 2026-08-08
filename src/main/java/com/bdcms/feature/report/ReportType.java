package com.bdcms.feature.report;

import java.util.Optional;

/** The three reports the proposal asks for (FR-7). */
public enum ReportType {

    /** Every donation in a date range, with donor, card and verifying staff. */
    DONATIONS("donations", "Donations by Date Range"),

    /** How many donors and donations fall into each blood group. */
    BLOOD_GROUP("blood-group", "Blood Group Distribution"),

    /** Per-donor activity: donation count, last donation, current eligibility. */
    DONOR_ACTIVITY("donor-activity", "Donor Activity");

    private final String slug;
    private final String title;

    ReportType(String slug, String title) {
        this.slug = slug;
        this.title = title;
    }

    /** URL-friendly identifier, e.g. {@code /app/reports?type=blood-group}. */
    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    /** True when this report is bounded by a date range. */
    public boolean isDateRanged() {
        return this == DONATIONS;
    }

    public static Optional<ReportType> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim().toLowerCase();
        for (ReportType type : values()) {
            if (type.slug.equals(trimmed) || type.name().equalsIgnoreCase(trimmed)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
