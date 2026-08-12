package com.bdcms.feature.eligibility;

/** Raised when an eligibility lookup targets a donor that does not exist. */
public class DonorNotFoundException extends RuntimeException {

    public DonorNotFoundException(String message) {
        super(message);
    }
}
