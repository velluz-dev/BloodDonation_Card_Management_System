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
                // The English singular/plural of "entry/entries" used to be
                // spliced in here. Burmese does not inflect for number, so the
                // whole sentence now comes from the bundle and each language
                // handles its own agreement.
                Flash.success(request, msg(request, "flash.donorDeleted",
                        outcome.getDonorName(),
                        outcome.getCardsRemoved(),
                        outcome.getAuditRowsKept()));
                redirect(request, response, "/app/donors");
            }
            case HAS_DONATIONS -> {
                Flash.error(request, msg(request, "flash.donorHasDonations",
                        outcome.getDonorName(), outcome.getDonationCount()));
                redirect(request, response, "/app/donors/view?donorId=" + donorId.get());
            }
            case NOT_FOUND -> {
                Flash.error(request, msg(request, "flash.noSuchDonor"));
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
