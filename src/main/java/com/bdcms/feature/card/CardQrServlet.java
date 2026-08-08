package com.bdcms.feature.card;

import com.bdcms.shared.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Serves the QR code image for a card (FR-3).
 *
 * <p>Generated on demand rather than stored: a QR code is a pure function of
 * the card number, so caching it in the database would add a second copy of the
 * same fact that could drift out of step with the first.
 *
 * <p>The number is looked up before anything is drawn, so this cannot be used
 * as an oracle to render QR codes for arbitrary strings.
 */
@WebServlet(name = "cardQrServlet", urlPatterns = "/app/cards/qr")
public class CardQrServlet extends BaseServlet {

    private final CardService cardService = new CardService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String cardNo = param(request, "cardNo");
        int size = intParam(request, "size", 260);

        if (cardNo == null || cardService.findByCardNo(cardNo).isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such card");
            return;
        }

        byte[] png = QrCodeGenerator.png(CardNumberGenerator.normalise(cardNo), size);
        response.setContentType("image/png");
        response.setContentLength(png.length);
        response.setHeader("Cache-Control", "private, max-age=3600");
        try (OutputStream out = response.getOutputStream()) {
            out.write(png);
        }
    }
}
