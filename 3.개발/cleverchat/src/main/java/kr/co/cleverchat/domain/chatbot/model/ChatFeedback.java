package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatFeedback {

    private Long chatFeedbackNo;
    private Long messageNo;
    private String rating;
    private String comment;
    private String commentCiphertext;
    private String commentKeyId;
    private Integer commentEncryptionVersion;
    private OffsetDateTime frstRegDt;
    private String ipHash;

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

    public String getIpHash() {
        return ipHash;
    }

    public void setIpHash(String ipHash) {
        this.ipHash = ipHash;
    }
}
