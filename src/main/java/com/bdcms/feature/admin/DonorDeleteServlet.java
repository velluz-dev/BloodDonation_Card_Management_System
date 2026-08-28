package com.bdcms.feature.admin;

import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.Flash;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * Deletes a donor record (administrators only).
 *
 * <p>Mapped under {@code /app/admin}, so
 * {@link com.bdcms.shared.web.AuthFilter} already restricts it;
 * {@link #requireAdmin} repeats the check because an operation this destructive
 * should not depend on a URL prefix staying where it is.
 *
 * <p>POST only. A deletion must never be reachable by following a link, which a
 * GET would allow - including from a crawler or a mistyped URL.
 */
@WebServlet(name = "donorDeleteServlet", urlPatterns = "/app/admin/donors/delete")
public class DonorDeleteServlet extends BaseServlet {

    private final DataAdminService dataAdminService = new DataAdminService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if (!requireAdmin(request, response)) {
            return;
        }

        Optional<Long> donorId = longParam(request, "donorId");
        if (donorId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "donorId is required");
            return;
        }

        DonorDeletion outcome = dataAdminService.deleteDonor(donorId.get());

        switch (outcome.getStatus()) {
            case DELETED -> {
                Flash.success(request, "Donor " + outcome.getDonorName() + " deleted, along with "
                        + outcome.getCardsRemoved() + " card(s). "
                        + outcome.getAuditRowsKept() + " verification log entr"
                        + (outcome.getAuditRowsKept() == 1 ? "y was" : "ies were")
                        + " kept and detached from the record.");
                redirect(request, response, "/app/donors");
            }
            case HAS_DONATIONS -> {
                Flash.error(request, outcome.getDonorName() + " has "
                        + outcome.getDonationCount() + " recorded donation(s) and cannot be deleted. "
                        + "Donation history is append-only because the eligibility rule is computed "
                        + "from it. Deactivate their card instead if they should no longer donate.");
                redirect(request, response, "/app/donors/view?donorId=" + donorId.get());
            }
            case NOT_FOUND -> {
                Flash.error(request, "No such donor - it may already have been deleted.");
                redirect(request, response, "/app/donors");
            }
        }
    }

    /** A deletion is never a link. Send anyone who arrives by GET to the record. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Optional<Long> donorId = longParam(request, "donorId");
        redirect(request, response, donorId
                .map(id -> "/app/donors/view?donorId=" + id)
                .orElse("/app/donors"));
    }
}
