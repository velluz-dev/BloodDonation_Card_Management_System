package com.bdcms.feature.report;

import com.bdcms.feature.eligibility.EligibilityResult;
import com.bdcms.feature.eligibility.EligibilityService;
import com.bdcms.shared.db.Tx;
import com.bdcms.shared.i18n.Messages;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /**
     * Builds a report in {@code locale}.
     *
     * <p>The locale reaches here rather than being applied at render time
     * because the PDF export renders from this same object: an operator who
     * presses "Export PDF" on a Burmese screen should get a Burmese PDF, and
     * translating only in the JSP would give them an English one.
     */
    public Report build(ReportType type, LocalDate from, LocalDate to, Locale locale) {
        return switch (type) {
            case DONATIONS -> donations(from, to, locale);
            case BLOOD_GROUP -> bloodGroups(locale);
            case DONOR_ACTIVITY -> donorActivity(locale);
        };
    }

    /** Column headings for {@code type}, translated. */
    private static List<String> headers(ReportType type, Locale locale) {
        return type.getHeaderKeys().stream()
                .map(key -> Messages.get(locale, key))
                .toList();
    }

    private Report donations(LocalDate from, LocalDate to, Locale locale) {
        LocalDate start = from == null ? LocalDate.now().minusMonths(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;

        List<ReportRow> rows = Tx.read(conn -> reportDao.donationsBetween(conn, start, end));
        return new Report(
                ReportType.DONATIONS,
                Messages.get(locale, ReportType.DONATIONS.getMessageKey()),
                Messages.format(locale, ReportType.DONATIONS.getSubtitleKey(),
                        DISPLAY.format(start), DISPLAY.format(end), rows.size()),
                headers(ReportType.DONATIONS, locale),
                rows, start, end);
    }

    private Report bloodGroups(Locale locale) {
        List<ReportRow> rows = Tx.read(reportDao::bloodGroupDistribution);
        return new Report(
                ReportType.BLOOD_GROUP,
                Messages.get(locale, ReportType.BLOOD_GROUP.getMessageKey()),
                Messages.get(locale, ReportType.BLOOD_GROUP.getSubtitleKey()),
                headers(ReportType.BLOOD_GROUP, locale),
                rows, null, null);
    }

    /**
     * Per-donor activity with the eligibility verdict applied in Java.
     *
     * <p>The interval is read once, inside a single connection, and then applied
     * to every donor - so every line of the report is judged against the same
     * rule even if an administrator changes the setting while it is running.
     */
    private Report donorActivity(Locale locale) {
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
                        row.activeCardNo() == null
                                ? Messages.get(locale, "report.noActiveCard")
                                : row.activeCardNo(),
                        String.valueOf(row.donationCount()),
                        row.lastDonation() == null
                                ? Messages.get(locale, "common.never")
                                : DISPLAY.format(row.lastDonation()),
                        Messages.get(locale, eligibility.status().getMessageKey()),
                        eligibility.isEligible() ? "-" : DISPLAY.format(eligibility.nextEligibleDate())));
            }
            return built;
        });

        return new Report(
                ReportType.DONOR_ACTIVITY,
                Messages.get(locale, ReportType.DONOR_ACTIVITY.getMessageKey()),
                Messages.get(locale, ReportType.DONOR_ACTIVITY.getSubtitleKey()),
                headers(ReportType.DONOR_ACTIVITY, locale),
                rows, null, null);
    }
}
