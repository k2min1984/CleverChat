package kr.co.cleverchat.domain.search.model;

import java.time.OffsetDateTime;

public class SearchLog {

    private Long searchLogNo;
    private String queryText;
    private String normalizedQuery;
    private int resultCount;
    private Long topScenarioNo;
    private String source;
    private Integer latencyMs;
    private String anonymousIdHash;
    private Long userNo;
    private OffsetDateTime frstRegDt;

    public Long getSearchLogNo() {
        return searchLogNo;
    }

    public void setSearchLogNo(Long searchLogNo) {
        this.searchLogNo = searchLogNo;
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

    public Long getTopScenarioNo() {
        return topScenarioNo;
    }

    public void setTopScenarioNo(Long topScenarioNo) {
        this.topScenarioNo = topScenarioNo;
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

    public Long getUserNo() {
        return userNo;
    }

    public void setUserNo(Long userNo) {
        this.userNo = userNo;
    }

    public OffsetDateTime getFrstRegDt() {
        return frstRegDt;
    }

    public void setFrstRegDt(OffsetDateTime frstRegDt) {
        this.frstRegDt = frstRegDt;
    }
}
