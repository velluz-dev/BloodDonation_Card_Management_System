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

        Report report = reportService.build(type, from, to);

        if ("pdf".equalsIgnoreCase(param(request, "format"))) {
            sendPdf(request, response, report, type);
            return;
        }

        request.setAttribute("report", report);
        request.setAttribute("type", type);
        request.setAttribute("types", ReportType.values());
        request.setAttribute("from", from);
        request.setAttribute("to", to);
        render(request, response, "report/reports.jsp");
    }

    private void sendPdf(HttpServletRequest request, HttpServletResponse response,
                         Report report, ReportType type) throws IOException {

        SessionUser staff = currentUser(request);
        byte[] pdf = PdfExporter.export(report,
                staff == null ? "unknown" : staff.fullName() + " (" + staff.username() + ")");

        String filename = "bdcms-" + type.getSlug() + "-" + LocalDate.now() + ".pdf";
        response.setContentType("application/pdf");
        response.setContentLength(pdf.length);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try (OutputStream out = response.getOutputStream()) {
            out.write(pdf);
        }
    }
}
