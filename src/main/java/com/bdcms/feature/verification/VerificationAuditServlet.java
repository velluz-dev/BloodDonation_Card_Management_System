package com.bdcms.feature.verification;

import com.bdcms.shared.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * The verification audit trail (NFR Auditability).
 *
 * <p>Every attempt, successful or not, with the staff account and timestamp
 * responsible. Mapped under {@code /app/admin} so
 * {@link com.bdcms.shared.web.AuthFilter} restricts it to administrators - the
 * audit log is a supervisory record, and counter staff should not be able to
 * review their own failed attempts.
 */
@WebServlet(name = "verificationAuditServlet", urlPatterns = "/app/admin/verification-log")
public class VerificationAuditServlet extends BaseServlet {

    private static final int PAGE_SIZE = 50;

    private final VerificationService verificationService = new VerificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Long donorId = longParam(request, "donorId").orElse(null);
        LocalDate from = dateParam(request, "from").orElse(null);
        LocalDate to = dateParam(request, "to").orElse(null);
        int page = Math.max(1, intParam(request, "page", 1));

        List<VerificationLog> entries =
                verificationService.auditTrail(donorId, from, to, PAGE_SIZE, (page - 1) * PAGE_SIZE);

        request.setAttribute("entries", entries);
        request.setAttribute("donorId", donorId);
        request.setAttribute("from", from);
        request.setAttribute("to", to);
        request.setAttribute("page", page);
        request.setAttribute("hasNextPage", entries.size() == PAGE_SIZE);
        request.setAttribute("results", VerificationResult.values());

        render(request, response, "verification/audit.jsp");
    }
}
