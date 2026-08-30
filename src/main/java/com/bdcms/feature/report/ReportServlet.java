package com.bdcms.feature.report;

import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.SessionUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;

/**
 * The three reports and their PDF export (FR-7).
 *
 * <p>Mapped under {@code /app/reports}, which
 * {@link com.bdcms.shared.web.AuthFilter} restricts to administrators.
 *
 * <p>One servlet serves both the screen and the file: the report is built once
 * and then either forwarded to a JSP or handed to {@link PdfExporter}. Building
 * it twice, once per format, is how an export quietly stops matching the page
 * it came from.
 */
@WebServlet(name = "reportServlet", urlPatterns = "/app/reports")
public class ReportServlet extends BaseServlet {

    private final ReportService reportService = new ReportService();
    private final com.bdcms.feature.verification.VerificationService verificationService =
            new com.bdcms.feature.verification.VerificationService();
    private final com.bdcms.feature.donor.DonorService donorService =
            new com.bdcms.feature.donor.DonorService();

    /**
     * Blood-group counts as bar widths, scaled against the largest group so the
     * rail always uses its full width.
     */
    private java.util.List<com.bdcms.feature.dashboard.GroupShare> bloodGroupMix() {
        java.util.Map<com.bdcms.feature.donor.BloodGroup, Integer> counts =
                donorService.countByBloodGroup();
        int largest = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        java.util.List<com.bdcms.feature.dashboard.GroupShare> rows =
                new java.util.ArrayList<>(counts.size());
        for (java.util.Map.Entry<com.bdcms.feature.donor.BloodGroup, Integer> e : counts.entrySet()) {
            int share = largest == 0 ? 0 : Math.round(e.getValue() * 100f / largest);
            rows.add(new com.bdcms.feature.dashboard.GroupShare(
                    e.getKey().getLabel(), e.getValue(), share));
        }
        return rows;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ReportType type = ReportType.parse(param(request, "type")).orElse(ReportType.DONATIONS);
        LocalDate from = dateParam(request, "from").orElse(LocalDate.now().minusMonths(1));
        LocalDate to = dateParam(request, "to").orElse(LocalDate.now());

        if (from.isAfter(to)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }

        Report report = reportService.build(type, from, to,
                com.bdcms.shared.i18n.Locales.current(request));

        if ("pdf".equalsIgnoreCase(param(request, "format"))) {
            sendPdf(request, response, report, type);
            return;
        }

        request.setAttribute("report", report);
        request.setAttribute("type", type);
        request.setAttribute("types", ReportType.values());
        request.setAttribute("from", from);
        request.setAttribute("to", to);

        // Rail figures. These sit beside every report rather than only the one
        // they belong to, because "how many attempts failed" is the question an
        // administrator asks whichever report they opened.
        request.setAttribute("bloodGroupMix", bloodGroupMix());
        request.setAttribute("verificationOutcomes",
                verificationService.outcomeCounts(from, to));

        render(request, response, "report/reports.jsp");
    }

    private void sendPdf(HttpServletRequest request, HttpServletResponse response,
                         Report report, ReportType type) throws IOException {

        SessionUser staff = currentUser(request);
        byte[] pdf = PdfExporter.export(report,
                staff == null ? "unknown" : staff.fullName() + " (" + staff.username() + ")",
                com.bdcms.shared.i18n.Locales.current(request));

        String filename = "bdcms-" + type.getSlug() + "-" + LocalDate.now() + ".pdf";
        response.setContentType("application/pdf");
        response.setContentLength(pdf.length);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try (OutputStream out = response.getOutputStream()) {
            out.write(pdf);
        }
    }
}
