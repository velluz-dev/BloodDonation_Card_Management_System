package com.bdcms.feature.donation;

import jakarta.servlet.http.HttpServletRequest;

/** Thin controller for the donation history view (FR-6). */
public class DonationController {

    private final DonationService donationService;

    public DonationController() {
        this(new DonationService());
    }

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    public void prepareHistoryModel(HttpServletRequest request, long donorId) {
        request.setAttribute("donations", donationService.historyForDonor(donorId));
        request.setAttribute("eligibility", donationService.eligibilityFor(donorId));
    }
}
