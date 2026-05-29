package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatFeedbackQueueItem {

    private Long id;
    private Long messageId;
    private String sessionKey;
    private Long scenarioId;
    private String scenarioTitle;
    private String rating;
    private String comment;
    private String commentCiphertext;
    private String commentKeyId;
    private Integer commentEncryptionVersion;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getScenarioTitle() {
        return scenarioTitle;
    }

    public void setScenarioTitle(String scenarioTitle) {
        this.scenarioTitle = scenarioTitle;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCommentCiphertext() {
        return commentCiphertext;
    }

    public void setCommentCiphertext(String commentCiphertext) {
        this.commentCiphertext = commentCiphertext;
    }

    public String getCommentKeyId() {
        return commentKeyId;
    }

    public void setCommentKeyId(String commentKeyId) {
        this.commentKeyId = commentKeyId;
    }

    public Integer getCommentEncryptionVersion() {
        return commentEncryptionVersion;
    }

    public void setCommentEncryptionVersion(Integer commentEncryptionVersion) {
        this.commentEncryptionVersion = commentEncryptionVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
