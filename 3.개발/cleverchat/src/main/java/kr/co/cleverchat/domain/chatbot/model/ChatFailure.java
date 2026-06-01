package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatFailure {

    private Long chatFailureNo;
    private String sessionNo;
    private Long messageNo;
    private String reason;
    private String detail;
    private boolean reviewed;
    private Long reviewedBy;
    private OffsetDateTime reviewedAt;
    private String reviewComment;
    private String reviewCommentCiphertext;
    private String reviewCommentKeyId;
    private Integer reviewCommentEncryptionVersion;
    private OffsetDateTime frstRegDt;

    public Long getChatFailureNo() {
        return chatFailureNo;
    }

    public void setChatFailureNo(Long chatFailureNo) {
        this.chatFailureNo = chatFailureNo;
    }

    public String getSessionNo() {
        return sessionNo;
    }

    public void setSessionNo(String sessionNo) {
        this.sessionNo = sessionNo;
    }

    public Long getMessageNo() {
        return messageNo;
    }

    public void setMessageNo(Long messageNo) {
        this.messageNo = messageNo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getReviewCommentCiphertext() {
        return reviewCommentCiphertext;
    }

    public void setReviewCommentCiphertext(String reviewCommentCiphertext) {
        this.reviewCommentCiphertext = reviewCommentCiphertext;
    }

    public String getReviewCommentKeyId() {
        return reviewCommentKeyId;
    }

    public void setReviewCommentKeyId(String reviewCommentKeyId) {
        this.reviewCommentKeyId = reviewCommentKeyId;
    }

    public Integer getReviewCommentEncryptionVersion() {
        return reviewCommentEncryptionVersion;
    }

    public void setReviewCommentEncryptionVersion(Integer reviewCommentEncryptionVersion) {
        this.reviewCommentEncryptionVersion = reviewCommentEncryptionVersion;
    }

    public OffsetDateTime getFrstRegDt() {
        return frstRegDt;
    }

    public void setFrstRegDt(OffsetDateTime frstRegDt) {
        this.frstRegDt = frstRegDt;
    }
}
