package com.bdcms.feature.admin;

/**
 * What happened when an administrator tried to delete a donor.
 *
 * <p>A class with JavaBean getters rather than a record: Tomcat 10.1 implements
 * Jakarta EL 5.0, which cannot read record components as properties. See
 * {@code com.bdcms.shared.web.SessionUser} for the full note.
 */
public class DonorDeletion {

    public enum Status {
        /** The donor and their cards are gone; the audit trail was kept. */
        DELETED,
        /** No donor with that id - already deleted, or a stale link. */
        NOT_FOUND,
        /** The donor has donations, so deleting them would destroy history. */
        HAS_DONATIONS
    }

    private final Status status;
    private final String donorName;
    private final int donationCount;
    private final int cardsRemoved;
    private final int auditRowsKept;

    private DonorDeletion(Status status, String donorName, int donationCount,
                          int cardsRemoved, int auditRowsKept) {
        this.status = status;
        this.donorName = donorName;
        this.donationCount = donationCount;
        this.cardsRemoved = cardsRemoved;
        this.auditRowsKept = auditRowsKept;
    }

    static DonorDeletion deleted(String donorName, int cardsRemoved, int auditRowsKept) {
        return new DonorDeletion(Status.DELETED, donorName, 0, cardsRemoved, auditRowsKept);
    }

    static DonorDeletion notFound() {
        return new DonorDeletion(Status.NOT_FOUND, null, 0, 0, 0);
    }

    static DonorDeletion hasDonations(String donorName, int donationCount) {
        return new DonorDeletion(Status.HAS_DONATIONS, donorName, donationCount, 0, 0);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isDeleted() {
        return status == Status.DELETED;
    }

    public String getDonorName() {
        return donorName;
    }

    public int getDonationCount() {
        return donationCount;
    }

    public int getCardsRemoved() {
        return cardsRemoved;
    }

    public int getAuditRowsKept() {
        return auditRowsKept;
    }
}
