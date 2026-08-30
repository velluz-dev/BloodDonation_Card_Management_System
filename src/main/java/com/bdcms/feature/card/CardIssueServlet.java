package com.bdcms.feature.card;

import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.DonorService;
import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.Flash;
import com.bdcms.shared.web.SessionUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * Issues a card, whether it is the donor's first or a replacement (FR-3).
 *
 * <p>The GET shows what is about to happen - including a warning when an
 * existing card will be superseded - and the POST performs it. Issuing is a
 * physical act at the counter, so it is never done silently as a side effect of
 * another screen.
 */
@WebServlet(name = "cardIssueServlet", urlPatterns = "/app/cards/issue")
public class CardIssueServlet extends BaseServlet {

    private final CardService cardService = new CardService();
    private final DonorService donorService = new DonorService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Optional<Donor> donor = loadDonor(request, response);
        if (donor.isEmpty()) {
            return;
        }
        request.setAttribute("donor", donor.get());
        request.setAttribute("existingCard",
                cardService.findActiveByDonor(donor.get().getDonorId()).orElse(null));
        render(request, response, "card/issue.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Optional<Donor> donor = loadDonor(request, response);
        if (donor.isEmpty()) {
            return;
        }
        SessionUser staff = currentUser(request);

        // A reissue for a lost card marks the old one LOST; a routine
        // replacement marks it INACTIVE. The distinction is kept because
        // "lost" is information the audit trail should not throw away.
        CardStatus supersededAs = "lost".equals(param(request, "reason"))
                ? CardStatus.LOST
                : CardStatus.INACTIVE;

        DonorCard issued = cardService.issue(donor.get().getDonorId(), staff.userId(), supersededAs);

        Flash.success(request, msg(request, "flash.cardIssued",
                issued.getCardNo(), donor.get().getFullName()));
        redirect(request, response, "/app/cards/print?cardNo=" + issued.getCardNo());
    }

    private Optional<Donor> loadDonor(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Optional<Long> donorId = longParam(request, "donorId");
        if (donorId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "donorId is required");
            return Optional.empty();
        }
        Optional<Donor> donor = donorService.findById(donorId.get());
        if (donor.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such donor");
        }
        return donor;
    }
}
