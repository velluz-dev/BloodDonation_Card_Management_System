package com.bdcms.feature.card;

import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.DonorService;
import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * A donor's cards: the current one and the trail of superseded ones (FR-3).
 *
 * <p>This is the card feature reading the donor feature through
 * {@link DonorService} - a service call, never {@code DonorDao}. The dependency
 * runs one way only: donor knows nothing about cards.
 */
@WebServlet(name = "cardServlet", urlPatterns = "/app/cards")
public class CardServlet extends BaseServlet {

    private final CardService cardService = new CardService();
    private final DonorService donorService = new DonorService();

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

        List<DonorCard> cards = cardService.historyForDonor(donorId.get());
        request.setAttribute("donor", donor.get());
        request.setAttribute("cards", cards);
        request.setAttribute("activeCard", cards.stream().filter(DonorCard::isUsable).findFirst().orElse(null));
        render(request, response, "card/list.jsp");
    }

    /** Reports a card lost, or withdraws it. Both are audited by the card's status trail. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Optional<Long> cardId = longParam(request, "cardId");
        Optional<Long> donorId = longParam(request, "donorId");
        String action = param(request, "action");

        if (cardId.isEmpty() || donorId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "cardId and donorId are required");
            return;
        }

        switch (action == null ? "" : action) {
            case "reportLost" -> {
                cardService.reportLost(cardId.get());
                Flash.success(request, msg(request, "flash.cardReportedLost"));
            }
            case "deactivate" -> {
                cardService.deactivate(cardId.get());
                Flash.success(request, msg(request, "flash.cardDeactivated"));
            }
            default -> {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
                return;
            }
        }
        redirect(request, response, "/app/cards?donorId=" + donorId.get());
    }
}
