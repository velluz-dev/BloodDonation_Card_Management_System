package com.bdcms.feature.donation;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One recorded blood donation (FR-6).
 *
 * <p>Append-only. A donation is a physical event that happened; correcting the
 * record by overwriting it would destroy the history the eligibility rule
 * depends on, so there is no update path in {@link DonationDao}.
 *
 * <p>{@code verifiedBy} is the staff account that confirmed the donor's
 * identity for this donation - the accountability link the proposal requires.
 */
public class Donation {

    private long donationId;
    private long donorId;
    private long cardId;
    private LocalDate donationDate;
    private String centre;
    private int volumeMl;
    private long verifiedBy;
    private LocalDateTime recordedAt;

    // Joined for display only; not columns of donations.
    private String donorName;
    private String nationalId;
    private String bloodGroup;
    private String cardNo;
    private String verifiedByName;

    public long getDonationId() {
        return donationId;
    }

    public void setDonationId(long donationId) {
        this.donationId = donationId;
    }

    public long getDonorId() {
        return donorId;
    }

    public void setDonorId(long donorId) {
        this.donorId = donorId;
    }

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
    }

    public String getCentre() {
        return centre;
    }

    public void setCentre(String centre) {
        this.centre = centre;
    }

    public int getVolumeMl() {
        return volumeMl;
    }

    public void setVolumeMl(int volumeMl) {
        this.volumeMl = volumeMl;
    }

    public long getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(long verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getVerifiedByName() {
        return verifiedByName;
    }

    public void setVerifiedByName(String verifiedByName) {
        this.verifiedByName = verifiedByName;
    }

    // ---- pre-formatted for the views (JSTL cannot format java.time) --------

    public String getDonationDateDisplay() {
        return com.bdcms.shared.util.Dates.display(donationDate);
    }

    public String getRecordedAtDisplay() {
        return com.bdcms.shared.util.Dates.display(recordedAt);
    }
}
