package com.bdcms.feature.card;

import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.DonorService;
import com.bdcms.shared.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * The printable donor card (FR-3), in two forms from one URL:
 *
 * <ul>
 *   <li>{@code /app/cards/print?cardNo=...} - an on-screen preview that shows
 *       staff exactly what will be printed;</li>
 *   <li>{@code ...&format=pdf} - the ID-1 sized PDF that actually goes to the
 *       card printer.</li>
 * </ul>
 */
@WebServlet(name = "cardPrintServlet", urlPatterns = "/app/cards/print")
public class CardPrintServlet extends BaseServlet {

    private final CardService cardService = new CardService();
    private final DonorService donorService = new DonorService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String cardNo = param(request, "cardNo");
        if (cardNo == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "cardNo is required");
            return;
        }

        Optional<DonorCard> card = cardService.findByCardNo(cardNo);
        if (card.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such card");
            return;
        }
        Optional<Donor> donor = donorService.findById(card.get().getDonorId());
        if (donor.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Card is not linked to a donor");
            return;
        }

        if ("pdf".equalsIgnoreCase(param(request, "format"))) {
            sendPdf(response, donor.get(), card.get());
            return;
        }

        request.setAttribute("donor", donor.get());
        request.setAttribute("card", card.get());
        render(request, response, "card/print.jsp");
    }

    private void sendPdf(HttpServletResponse response, Donor donor, DonorCard card)
            throws IOException {
        byte[] pdf = CardPdfRenderer.render(donor, card);
        response.setContentType("application/pdf");
        response.setContentLength(pdf.length);
        // "inline" so it opens in the browser's PDF viewer ready to print,
        // rather than dropping a file in the downloads folder.
        response.setHeader("Content-Disposition",
                "inline; filename=\"card-" + card.getCardNo() + ".pdf\"");
        try (OutputStream out = response.getOutputStream()) {
            out.write(pdf);
        }
    }
}
