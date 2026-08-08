package com.bdcms.feature.verification;

import com.bdcms.shared.config.AppConfig;
import com.bdcms.shared.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * The verification screen (FR-4) - steps 1 to 3 of the proposal's fixed sequence.
 *
 * <p>Staff scan the QR code or type the card number; the screen answers with
 * the stored photograph, prominently sized, and the masked national ID, so the
 * comparison against the person and their document can be made at a glance
 * (NFR Usability).
 *
 * <p>The confirmation itself is submitted to the donation feature, not to this
 * servlet. That is deliberate: the reliability requirement says verification,
 * eligibility and insertion must succeed or fail together, so all three have to
 * happen inside one transaction, and the donation feature is the one that owns
 * that transaction. The link between the two features here is a form action -
 * a URL - not an import.
 */
@WebServlet(name = "verificationServlet", urlPatterns = "/app/verify")
public class VerificationServlet extends BaseServlet {

    private final VerificationService verificationService = new VerificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String cardNo = param(request, "cardNo");
        request.setAttribute("cardNo", cardNo);
        request.setAttribute("defaultCentre", AppConfig.defaultCentre());
        request.setAttribute("defaultVolumeMl", 450);

        if (cardNo != null) {
            VerificationView view = verificationService.lookup(cardNo);
            request.setAttribute("view", view);
            if (!view.found()) {
                request.setAttribute("errorMessage",
                        "No card matches that number. Check the card and try again.");
            } else if (view.problem() == VerificationResult.CARD_INACTIVE) {
                request.setAttribute("errorMessage",
                        "This card is " + view.card().getStatus().getLabel().toLowerCase()
                                + " and cannot be used. Issue a replacement before recording a donation.");
            }
        }
        render(request, response, "verification/verify.jsp");
    }
}
