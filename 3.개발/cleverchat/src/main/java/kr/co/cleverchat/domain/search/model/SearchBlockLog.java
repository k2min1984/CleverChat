package kr.co.cleverchat.domain.search.model;

import java.time.OffsetDateTime;

public class SearchBlockLog {

    private Long id;
    private int queryLength;
    private String piiTypes;
    private String source;
    private String anonymousIdHash;
    private Long userId;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getQueryLength() {
        return queryLength;
    }

    public void setQueryLength(int queryLength) {
        this.queryLength = queryLength;
    }

    public String getPiiTypes() {
        return piiTypes;
    }

    public void setPiiTypes(String piiTypes) {
        this.piiTypes = piiTypes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAnonymousIdHash() {
        return anonymousIdHash;
    }

    public void setAnonymousIdHash(String anonymousIdHash) {
        this.anonymousIdHash = anonymousIdHash;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
