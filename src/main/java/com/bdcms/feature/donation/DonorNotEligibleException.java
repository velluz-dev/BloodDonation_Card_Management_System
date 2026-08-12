package com.bdcms.feature.donation;

import java.time.LocalDate;

public class DonorNotEligibleException extends RuntimeException {

    private final long daysRemaining;
    private final LocalDate nextPermittedDate;

    public DonorNotEligibleException(long daysRemaining, LocalDate nextPermittedDate, String message) {
        super(message);
        this.daysRemaining = daysRemaining;
        this.nextPermittedDate = nextPermittedDate;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public LocalDate getNextPermittedDate() {
        return nextPermittedDate;
    }
}
