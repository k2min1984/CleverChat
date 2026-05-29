package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatFailure {

    private Long id;
    private String sessionId;
    private Long messageId;
    private String reason;
    private String detail;
    private boolean reviewed;
    private Long reviewedBy;
    private OffsetDateTime reviewedAt;
    private String reviewComment;
    private String reviewCommentCiphertext;
    private String reviewCommentKeyId;
    private Integer reviewCommentEncryptionVersion;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
