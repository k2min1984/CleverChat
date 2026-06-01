package kr.co.cleverchat.domain.search.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class PopularQueryDaily {

    private LocalDate statDate;
    private String normalizedQuery;
    private String queryTextSample;
    private int searchCount;
    private int noResultCount;
    private Long topResultScenarioNo;
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;

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

    public Long getTopResultScenarioNo() {
        return topResultScenarioNo;
    }

    public void setTopResultScenarioNo(Long topResultScenarioNo) {
        this.topResultScenarioNo = topResultScenarioNo;
    }

    public OffsetDateTime getFrstRegDt() {
        return frstRegDt;
    }

    public void setFrstRegDt(OffsetDateTime frstRegDt) {
        this.frstRegDt = frstRegDt;
    }

    public OffsetDateTime getLstChgDt() {
        return lstChgDt;
    }

    public void setLstChgDt(OffsetDateTime lstChgDt) {
        this.lstChgDt = lstChgDt;
    }
}
