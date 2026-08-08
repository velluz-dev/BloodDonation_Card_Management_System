package com.bdcms.feature.report;

import com.bdcms.feature.eligibility.EligibilityResult;
import com.bdcms.feature.eligibility.EligibilityService;
import com.bdcms.shared.db.Tx;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Builds the three reports (FR-7). */
public class ReportService {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ReportDao reportDao;
    private final EligibilityService eligibilityService;

    public ReportService() {
        this(new ReportDao(), new EligibilityService());
    }

    public ReportService(ReportDao reportDao, EligibilityService eligibilityService) {
        this.reportDao = reportDao;
        this.eligibilityService = eligibilityService;
    }

    public Report build(ReportType type, LocalDate from, LocalDate to) {
        return switch (type) {
            case DONATIONS -> donations(from, to);
            case BLOOD_GROUP -> bloodGroups();
            case DONOR_ACTIVITY -> donorActivity();
        };
    }

    private Report donations(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusMonths(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;

        List<ReportRow> rows = Tx.read(conn -> reportDao.donationsBetween(conn, start, end));
        return new Report(
                ReportType.DONATIONS,
                ReportType.DONATIONS.getTitle(),
                DISPLAY.format(start) + " to " + DISPLAY.format(end)
                        + "  -  " + rows.size() + " donation(s)",
                List.of("Date", "Donor", "Group", "Card No", "Centre", "Volume", "Verified by"),
                rows, start, end);
    }

    private Report bloodGroups() {
        List<ReportRow> rows = Tx.read(reportDao::bloodGroupDistribution);
        return new Report(
                ReportType.BLOOD_GROUP,
                ReportType.BLOOD_GROUP.getTitle(),
                "All registered donors, grouped by ABO/Rh type",
                List.of("Blood Group", "Donors", "Donations", "Total Volume"),
                rows, null, null);
    }

    /**
     * Per-donor activity with the eligibility verdict applied in Java.
     *
     * <p>The interval is read once, inside a single connection, and then applied
     * to every donor - so every line of the report is judged against the same
     * rule even if an administrator changes the setting while it is running.
     */
    private Report donorActivity() {
        List<ReportRow> rows = Tx.read(conn -> {
            List<ReportDao.DonorActivityRow> raw = reportDao.donorActivity(conn);
            int intervalDays = eligibilityService.configuredIntervalDays(conn);
            LocalDate today = LocalDate.now();

            List<ReportRow> built = new ArrayList<>(raw.size());
            for (ReportDao.DonorActivityRow row : raw) {
                EligibilityResult eligibility =
                        eligibilityService.evaluate(row.lastDonation(), intervalDays, today);
                built.add(ReportRow.of(
                        row.fullName(),
                        row.bloodGroup(),
                        row.activeCardNo() == null ? "(no active card)" : row.activeCardNo(),
                        String.valueOf(row.donationCount()),
                        row.lastDonation() == null ? "Never" : DISPLAY.format(row.lastDonation()),
                        eligibility.status().getLabel(),
                        eligibility.isEligible() ? "-" : DISPLAY.format(eligibility.nextEligibleDate())));
            }
            return built;
        });

        return new Report(
                ReportType.DONOR_ACTIVITY,
                ReportType.DONOR_ACTIVITY.getTitle(),
                "Donation counts and current eligibility for every registered donor",
                List.of("Donor", "Group", "Active Card", "Donations", "Last Donation",
                        "Eligibility", "Next Eligible"),
                rows, null, null);
    }
}
