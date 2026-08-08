package com.bdcms.feature.donation;

import com.bdcms.feature.card.CardService;
import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.DonorService;
import com.bdcms.shared.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * A donor's complete donation history (FR-6).
 *
 * <p>The one screen in the system that draws the whole picture together -
 * donor, card, history and current eligibility - because the donation feature
 * is the only one permitted to compose the others.
 */
@WebServlet(name = "donationHistoryServlet", urlPatterns = "/app/donations/history")
public class DonationHistoryServlet extends BaseServlet {

    private final DonationService donationService = new DonationService();
    private final DonorService donorService = new DonorService();
    private final CardService cardService = new CardService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Optional<Long> donorId = longParam(request, "donorId");
        if (donorId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "donorId is required");
            return;
        }
        Optional<Donor> donor = donorService.findById(donorId.get());
        if (donor.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such donor");
            return;
        }

        request.setAttribute("donor", donor.get());
        request.setAttribute("donations", donationService.historyForDonor(donorId.get()));
        request.setAttribute("eligibility", donationService.eligibilityFor(donorId.get()));
        request.setAttribute("activeCard",
                cardService.findActiveByDonor(donorId.get()).orElse(null));

        render(request, response, "donation/history.jsp");
    }
}
