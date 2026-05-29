package kr.co.cleverchat.domain.search.model;

import java.time.OffsetDateTime;

public class SearchLog {

    private Long id;
    private String queryText;
    private String normalizedQuery;
    private int resultCount;
    private Long topScenarioId;
    private String source;
    private Integer latencyMs;
    private String anonymousIdHash;
    private Long userId;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getNormalizedQuery() {
        return normalizedQuery;
    }

    public void setNormalizedQuery(String normalizedQuery) {
        this.normalizedQuery = normalizedQuery;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public Long getTopScenarioId() {
        return topScenarioId;
    }

    public void setTopScenarioId(Long topScenarioId) {
        this.topScenarioId = topScenarioId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
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
