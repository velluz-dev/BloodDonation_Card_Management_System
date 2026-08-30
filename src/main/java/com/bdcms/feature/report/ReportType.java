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

    /** Bundle key for the title, e.g. {@code report.type.DONATIONS}. */
    public String getMessageKey() {
        return "report.type." + name();
    }

    /** Bundle key for the line under the title. */
    public String getSubtitleKey() {
        return "report.subtitle." + name();
    }

    /** Bundle keys for this report's column headings, left to right. */
    public java.util.List<String> getHeaderKeys() {
        return switch (this) {
            case DONATIONS -> java.util.List.of(
                    "col.date", "col.donor", "col.group", "col.cardNo",
                    "col.centre", "col.volume", "col.verifiedBy");
            case BLOOD_GROUP -> java.util.List.of(
                    "col.bloodGroup", "col.donors", "col.donations", "col.totalVolume");
            case DONOR_ACTIVITY -> java.util.List.of(
                    "col.donor", "col.group", "col.activeCard", "col.donations",
                    "col.lastDonation", "col.eligibility", "col.nextEligible");
        };
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
