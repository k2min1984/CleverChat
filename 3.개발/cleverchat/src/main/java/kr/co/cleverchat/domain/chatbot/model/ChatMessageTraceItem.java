package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatMessageTraceItem {

    private Long chatMessageNo;
    private String sessionNo;
    private int seq;
    private String direction;
    private Long nodeNo;
    private Long optionNo;
    private String content;
    private String contentCiphertext;
    private String contentKeyId;
    private Integer contentEncryptionVersion;
    private String payload;
    private Integer latencyMs;
    private OffsetDateTime frstRegDt;
    private String feedbackRating;
    private String feedbackComment;
    private String feedbackCommentCiphertext;
    private String feedbackCommentKeyId;
    private Integer feedbackCommentEncryptionVersion;

    public Long getChatMessageNo() {
        return chatMessageNo;
    }

    public void setChatMessageNo(Long chatMessageNo) {
        this.chatMessageNo = chatMessageNo;
    }

    public String getSessionNo() {
        return sessionNo;
    }

    public void setSessionNo(String sessionNo) {
        this.sessionNo = sessionNo;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Long getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Long nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Long getOptionNo() {
        return optionNo;
    }

    public void setOptionNo(Long optionNo) {
        this.optionNo = optionNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentCiphertext() {
        return contentCiphertext;
    }

    public void setContentCiphertext(String contentCiphertext) {
        this.contentCiphertext = contentCiphertext;
    }

    public String getContentKeyId() {
        return contentKeyId;
    }

    public void setContentKeyId(String contentKeyId) {
        this.contentKeyId = contentKeyId;
    }

    public Integer getContentEncryptionVersion() {
        return contentEncryptionVersion;
    }

    public void setContentEncryptionVersion(Integer contentEncryptionVersion) {
        this.contentEncryptionVersion = contentEncryptionVersion;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public OffsetDateTime getFrstRegDt() {
        return frstRegDt;
    }

    public void setFrstRegDt(OffsetDateTime frstRegDt) {
        this.frstRegDt = frstRegDt;
    }

    public String getFeedbackRating() {
        return feedbackRating;
    }

    public void setFeedbackRating(String feedbackRating) {
        this.feedbackRating = feedbackRating;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    public void setFeedbackComment(String feedbackComment) {
        this.feedbackComment = feedbackComment;
    }

    public String getFeedbackCommentCiphertext() {
        return feedbackCommentCiphertext;
    }

    public void setFeedbackCommentCiphertext(String feedbackCommentCiphertext) {
        this.feedbackCommentCiphertext = feedbackCommentCiphertext;
    }

    public String getFeedbackCommentKeyId() {
        return feedbackCommentKeyId;
    }

    public void setFeedbackCommentKeyId(String feedbackCommentKeyId) {
        this.feedbackCommentKeyId = feedbackCommentKeyId;
    }

    public Integer getFeedbackCommentEncryptionVersion() {
        return feedbackCommentEncryptionVersion;
    }

    public void setFeedbackCommentEncryptionVersion(Integer feedbackCommentEncryptionVersion) {
        this.feedbackCommentEncryptionVersion = feedbackCommentEncryptionVersion;
    }
}
