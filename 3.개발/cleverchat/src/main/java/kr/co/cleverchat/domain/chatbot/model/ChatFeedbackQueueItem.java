package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatFeedbackQueueItem {

    private Long chatFeedbackNo;
    private Long messageNo;
    private String sessionKey;
    private Long scenarioNo;
    private String scenarioTitle;
    private String rating;
    private String comment;
    private String commentCiphertext;
    private String commentKeyId;
    private Integer commentEncryptionVersion;
    private OffsetDateTime frstRegDt;

    public Long getChatFeedbackNo() {
        return chatFeedbackNo;
    }

    public void setChatFeedbackNo(Long chatFeedbackNo) {
        this.chatFeedbackNo = chatFeedbackNo;
    }

    public Long getMessageNo() {
        return messageNo;
    }

    public void setMessageNo(Long messageNo) {
        this.messageNo = messageNo;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Long getScenarioNo() {
        return scenarioNo;
    }

    public void setScenarioNo(Long scenarioNo) {
        this.scenarioNo = scenarioNo;
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

    public OffsetDateTime getFrstRegDt() {
        return frstRegDt;
    }

    public void setFrstRegDt(OffsetDateTime frstRegDt) {
        this.frstRegDt = frstRegDt;
    }
}
