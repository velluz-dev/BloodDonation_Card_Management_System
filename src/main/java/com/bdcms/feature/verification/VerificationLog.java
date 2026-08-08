package com.bdcms.feature.verification;

import java.time.LocalDateTime;

/**
 * One recorded verification attempt (FR-4, NFR Auditability).
 *
 * <p>Append-only: rows are never updated or deleted. {@code cardNo} is stored
 * as text rather than a foreign key, and {@code donorId} is nullable, so an
 * attempt with a card number that does not exist at all is still auditable.
 */
public class VerificationLog {

    private long logId;
    private String cardNo;
    private Long donorId;
    private long staffId;
    private VerificationResult result;
    private String detail;
    private LocalDateTime attemptedAt;

    // Joined for display only; not columns of verification_log.
    private String donorName;
    private String staffName;

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public Long getDonorId() {
        return donorId;
    }

    public void setDonorId(Long donorId) {
        this.donorId = donorId;
    }

    public long getStaffId() {
        return staffId;
    }

    public void setStaffId(long staffId) {
        this.staffId = staffId;
    }

    public VerificationResult getResult() {
        return result;
    }

    public void setResult(VerificationResult result) {
        this.result = result;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(LocalDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    /**
     * The timestamp already formatted for display.
     *
     * <p>JSTL's {@code fmt} tags predate {@code java.time} and cannot format a
     * {@link LocalDateTime}. Rather than have every JSP parse the ISO string
     * back into a date - fragile, and logic in a view besides - the formatting
     * is done once here.
     */
    public String getAttemptedAtDisplay() {
        return attemptedAt == null ? "" : com.bdcms.shared.util.Dates.display(attemptedAt);
    }
}
