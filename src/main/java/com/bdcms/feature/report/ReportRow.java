package com.bdcms.feature.report;

import java.util.List;

/**
 * One row of a report, held generically so that the HTML table and the PDF are
 * rendered from the same data (FR-7).
 *
 * <p>That is the point of the shape: a report defined once as headers plus rows
 * cannot show one thing on screen and a different thing in the exported file.
 */
public record ReportRow(List<String> cells) {

    public static ReportRow of(String... cells) {
        return new ReportRow(List.of(cells));
    }

    public String cell(int index) {
        return index < cells.size() ? cells.get(index) : "";
    }

    /**
     * JavaBean accessor for JSP/EL. Tomcat 10.1 implements Jakarta EL 5.0,
     * which cannot read record components as properties (added in EL 6.0).
     */
    public List<String> getCells() {
        return cells;
    }
}
