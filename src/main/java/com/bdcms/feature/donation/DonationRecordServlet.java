package com.bdcms.feature.donation;

import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.Flash;
import com.bdcms.shared.web.SessionUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Records a donation (FR-6) - the endpoint the verification screen submits to.
 *
 * <p>Everything of consequence happens in {@link DonationService#record}, in one
 * transaction. This servlet's job is only to read the form, name the signed-in
 * staff member as accountable, and turn the outcome into a page.
 *
 * <p>Success ends in a redirect rather than a rendered page, so that a browser
 * refresh cannot record the same donation twice.
 */
@WebServlet(name = "donationRecordServlet", urlPatterns = "/app/donations/record")
public class DonationRecordServlet extends BaseServlet {

    private final DonationService donationService = new DonationService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        SessionUser staff = currentUser(request);
        String cardNo = param(request, "cardNo");
        boolean confirmed = "on".equals(param(request, "confirmed"))
                || "true".equals(param(request, "confirmed"));
        String pin = param(request, "pin");
        String centre = param(request, "centre", com.bdcms.shared.config.AppConfig.defaultCentre());
        int volumeMl = intParam(request, "volumeMl", 450);
        LocalDate donationDate = dateParam(request, "donationDate").orElse(LocalDate.now());

        DonationOutcome outcome = donationService.record(
                cardNo, staff.userId(), confirmed, pin, centre, volumeMl, donationDate);

        if (outcome.isRecorded()) {
            Flash.success(request, "Donation recorded for "
                    + outcome.donation().getDonorName() + ". "
                    + "The donor may next donate on or after "
                    + donationDate.plusDays(outcome.eligibility().intervalDays()) + ".");
            redirect(request, response,
                    "/app/donations/history?donorId=" + outcome.donation().getDonorId());
            return;
        }

        // Not recorded: re-render the verification screen with the reason, so
        // staff see the donor's photograph and the failure side by side.
        request.setAttribute("outcome", outcome);
        request.setAttribute("errorMessage", outcome.message());
        request.setAttribute("eligibility", outcome.eligibility());
        request.setAttribute("cardNo", cardNo);
        request.setAttribute("defaultCentre", centre);
        request.setAttribute("defaultVolumeMl", volumeMl);

        // Re-run the read-only lookup so the photo and masked ID are on screen.
        request.setAttribute("view", new com.bdcms.feature.verification.VerificationService()
                .lookup(cardNo));

        response.setStatus(outcome.status() == DonationOutcome.Status.NOT_ELIGIBLE
                ? HttpServletResponse.SC_CONFLICT
                : HttpServletResponse.SC_BAD_REQUEST);
        render(request, response, "verification/verify.jsp");
    }

    /** A direct GET has no form to act on; send staff to the verification screen. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        redirect(request, response, "/app/verify");
    }
}
