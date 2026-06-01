package kr.co.cleverchat.domain.search.model;

import java.time.OffsetDateTime;

public class SearchBlockLog {

    private Long searchBlockLogNo;
    private int queryLength;
    private String piiTypes;
    private String source;
    private String anonymousIdHash;
    private Long userNo;
    private OffsetDateTime frstRegDt;

    public Long getSearchBlockLogNo() {
        return searchBlockLogNo;
    }

    public void setSearchBlockLogNo(Long searchBlockLogNo) {
        this.searchBlockLogNo = searchBlockLogNo;
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
