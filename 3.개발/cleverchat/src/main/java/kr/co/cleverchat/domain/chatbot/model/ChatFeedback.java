package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatFeedback {

    private Long id;
    private Long messageId;
    private String rating;
    private String comment;
    private String commentCiphertext;
    private String commentKeyId;
    private Integer commentEncryptionVersion;
    private OffsetDateTime createdAt;
    private String ipHash;

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

    public String getIpHash() {
        return ipHash;
    }

    public void setIpHash(String ipHash) {
        this.ipHash = ipHash;
    }
}
