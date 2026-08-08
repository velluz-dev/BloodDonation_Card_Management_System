package com.bdcms.feature.report;

import java.time.LocalDate;
import java.util.List;

/**
 * A rendered report: title, column headers, rows and the period covered (FR-7).
 *
 * @param type    which report this is
 * @param title   heading shown on screen and in the PDF
 * @param subtitle the period or filter in words
 * @param headers column headings
 * @param rows    the data
 * @param from    start of the period, or null when not date-bounded
 * @param to      end of the period, or null when not date-bounded
 */
public record Report(
        ReportType type,
        String title,
        String subtitle,
        List<String> headers,
        List<ReportRow> rows,
        LocalDate from,
        LocalDate to) {

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public int getRowCount() {
        return rows.size();
    }

    // ---- JavaBean accessors for JSP/EL -------------------------------------
    // Tomcat 10.1 implements Jakarta EL 5.0, which cannot read record
    // components as properties (added in EL 6.0). See SessionUser for the note.

    public ReportType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<ReportRow> getRows() {
        return rows;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }
}
