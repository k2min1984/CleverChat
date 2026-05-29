package kr.co.cleverchat.domain.search.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class PopularQueryDaily {

    private LocalDate statDate;
    private String normalizedQuery;
    private String queryTextSample;
    private int searchCount;
    private int noResultCount;
    private Long topResultScenarioId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public String getNormalizedQuery() {
        return normalizedQuery;
    }

    public void setNormalizedQuery(String normalizedQuery) {
        this.normalizedQuery = normalizedQuery;
    }

    public String getQueryTextSample() {
        return queryTextSample;
    }

    public void setQueryTextSample(String queryTextSample) {
        this.queryTextSample = queryTextSample;
    }

    public int getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(int searchCount) {
        this.searchCount = searchCount;
    }

    public int getNoResultCount() {
        return noResultCount;
    }

    public void setNoResultCount(int noResultCount) {
        this.noResultCount = noResultCount;
    }

    public Long getTopResultScenarioId() {
        return topResultScenarioId;
    }

    public void setTopResultScenarioId(Long topResultScenarioId) {
        this.topResultScenarioId = topResultScenarioId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
