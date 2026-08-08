package com.bdcms.feature.card;

import java.time.LocalDateTime;

/**
 * An issued blood donation card (FR-3).
 *
 * <p>A donor accumulates card rows over time as cards are lost and reissued;
 * the application invariant, enforced by {@link CardService}, is that at most
 * one of them is {@link CardStatus#ACTIVE}.
 */
public class DonorCard {

    private long cardId;
    private String cardNo;
    private long donorId;
    private LocalDateTime issuedOn;
    private CardStatus status = CardStatus.ACTIVE;
    private Long issuedBy;

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public long getDonorId() {
        return donorId;
    }

    public void setDonorId(long donorId) {
        this.donorId = donorId;
    }

    public LocalDateTime getIssuedOn() {
        return issuedOn;
    }

    public void setIssuedOn(LocalDateTime issuedOn) {
        this.issuedOn = issuedOn;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public Long getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(Long issuedBy) {
        this.issuedBy = issuedBy;
    }

    public boolean isUsable() {
        return status != null && status.isUsable();
    }

    /** Pre-formatted for the views: JSTL cannot format a {@code java.time} value. */
    public String getIssuedOnDisplay() {
        return com.bdcms.shared.util.Dates.display(issuedOn);
    }
}
