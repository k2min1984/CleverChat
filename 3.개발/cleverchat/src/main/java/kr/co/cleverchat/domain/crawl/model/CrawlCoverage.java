package kr.co.cleverchat.domain.crawl.model;

import java.time.OffsetDateTime;

public class CrawlCoverage {
    private Long coverageNo;
    private Long targetNo;
    private Long runLogNo;
    private int listPages;
    private int listItemsFound;
    private int detailsFetched;
    private int detailsFailed;
    private String truncatedYn;
    private OffsetDateTime frstRegDt;

    public Long getCoverageNo() {
        return coverageNo;
    }

    public void setCoverageNo(Long coverageNo) {
        this.coverageNo = coverageNo;
    }

    public Long getTargetNo() {
        return targetNo;
    }

    public void setTargetNo(Long targetNo) {
        this.targetNo = targetNo;
    }

    public Long getRunLogNo() {
        return runLogNo;
    }

    public void setRunLogNo(Long runLogNo) {
        this.runLogNo = runLogNo;
    }

    public int getListPages() {
        return listPages;
    }

    public void setListPages(int listPages) {
        this.listPages = listPages;
    }

    public int getListItemsFound() {
        return listItemsFound;
    }

    public void setListItemsFound(int listItemsFound) {
        this.listItemsFound = listItemsFound;
    }

    public int getDetailsFetched() {
        return detailsFetched;
    }

    public void setDetailsFetched(int detailsFetched) {
        this.detailsFetched = detailsFetched;
    }

    public int getDetailsFailed() {
        return detailsFailed;
    }

    public void setDetailsFailed(int detailsFailed) {
        this.detailsFailed = detailsFailed;
    }

    public String getTruncatedYn() {
        return truncatedYn;
    }

    public void setTruncatedYn(String truncatedYn) {
        this.truncatedYn = truncatedYn;
    }

    public OffsetDateTime getFrstRegDt() {
        return frstRegDt;
    }

    public void setFrstRegDt(OffsetDateTime frstRegDt) {
        this.frstRegDt = frstRegDt;
    }
}
