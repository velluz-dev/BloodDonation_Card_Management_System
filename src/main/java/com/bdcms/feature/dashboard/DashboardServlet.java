package com.bdcms.feature.dashboard;

import com.bdcms.feature.card.CardService;
import com.bdcms.feature.card.CardStatus;
import com.bdcms.feature.donation.Donation;
import com.bdcms.feature.donation.DonationService;
import com.bdcms.feature.donor.BloodGroup;
import com.bdcms.feature.donor.DonorService;
import com.bdcms.feature.eligibility.EligibilityService;
import com.bdcms.feature.verification.VerificationLog;
import com.bdcms.feature.verification.VerificationService;
import com.bdcms.shared.config.AppConfig;
import com.bdcms.shared.db.Tx;
import com.bdcms.shared.web.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The shift dashboard - the landing page after sign-in.
 *
 * <p>A read-only composition of every other feature, reached only through their
 * services. It owns no table and no rule of its own, which is why it can depend
 * on everything without creating a cycle: nothing depends on it.
 */
@WebServlet(name = "dashboardServlet", urlPatterns = "/app/dashboard")
public class DashboardServlet extends BaseServlet {

    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("EEEE, d MMMM");

    private final DonorService donorService = new DonorService();
    private final CardService cardService = new CardService();
    private final DonationService donationService = new DonationService();
    private final VerificationService verificationService = new VerificationService();
    private final EligibilityService eligibilityService = new EligibilityService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate weekStart = LocalDate.now().minusDays(7);

        request.setAttribute("todayLong", LONG_DATE.format(LocalDate.now()));
        request.setAttribute("centreName", AppConfig.defaultCentre());

        request.setAttribute("donorCount", donorService.countAll());
        request.setAttribute("activeCardCount", cardService.countActive());
        request.setAttribute("donationsThisMonth", donationService.countSince(monthStart));
        request.setAttribute("verificationsThisWeek", verificationService.attemptsSince(weekStart));
        request.setAttribute("failedVerifications", verificationService.failedAttemptsSince(weekStart));
        request.setAttribute("lostCardCount", cardService.countByStatus(CardStatus.LOST));

        // The interval is a configured rule, so it is read rather than assumed.
        request.setAttribute("intervalDays",
                Tx.read(eligibilityService::configuredIntervalDays));

        request.setAttribute("bloodGroupMix", bloodGroupMix());

        List<Donation> recentDonations = donationService.recent(6);
        List<VerificationLog> recentAttempts = verificationService.recentAttempts(6);
        request.setAttribute("recentDonations", recentDonations);
        request.setAttribute("recentAttempts", recentAttempts);

        render(request, response, "dashboard/dashboard.jsp");
    }

    /**
     * Blood-group counts as bar widths.
     *
     * <p>Scaled against the largest group rather than the total, so the rail
     * always uses its full width and the comparison between groups stays
     * readable even when one group dominates.
     */
    private List<GroupShare> bloodGroupMix() {
        Map<BloodGroup, Integer> counts = donorService.countByBloodGroup();
        int largest = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        List<GroupShare> rows = new ArrayList<>(counts.size());
        for (Map.Entry<BloodGroup, Integer> entry : counts.entrySet()) {
            int share = largest == 0 ? 0 : Math.round(entry.getValue() * 100f / largest);
            rows.add(new GroupShare(entry.getKey().getLabel(), entry.getValue(), share));
        }
        return rows;
    }
}
