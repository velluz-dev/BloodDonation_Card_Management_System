package com.bdcms.feature.dashboard;

import com.bdcms.feature.card.CardService;
import com.bdcms.feature.donation.Donation;
import com.bdcms.feature.donation.DonationService;
import com.bdcms.feature.donor.DonorService;
import com.bdcms.feature.verification.VerificationLog;
import com.bdcms.feature.verification.VerificationService;
import com.bdcms.shared.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * The landing page after sign-in.
 *
 * <p>A read-only composition of every other feature, reached only through their
 * services. It owns no table and no rule of its own, which is why it can depend
 * on everything without creating a cycle - nothing depends on it.
 */
@WebServlet(name = "dashboardServlet", urlPatterns = "/app/dashboard")
public class DashboardServlet extends BaseServlet {

    private final DonorService donorService = new DonorService();
    private final CardService cardService = new CardService();
    private final DonationService donationService = new DonationService();
    private final VerificationService verificationService = new VerificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate weekStart = LocalDate.now().minusDays(7);

        request.setAttribute("donorCount", donorService.countAll());
        request.setAttribute("activeCardCount", cardService.countActive());
        request.setAttribute("donationsThisMonth", donationService.countSince(monthStart));
        request.setAttribute("failedVerifications", verificationService.failedAttemptsSince(weekStart));

        List<Donation> recentDonations = donationService.recent(8);
        List<VerificationLog> recentAttempts = verificationService.recentAttempts(8);

        request.setAttribute("recentDonations", recentDonations);
        request.setAttribute("recentAttempts", recentAttempts);

        render(request, response, "dashboard/dashboard.jsp");
    }
}
