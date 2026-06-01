package kr.co.cleverchat.domain.scenario.model;

import java.time.OffsetDateTime;

public class ScenarioCategory {

    private Long scenarioCategoryNo;
    private Long pScenarioCategoryNo;
    private String name;
    private int sortOrder;
    private String useYn;
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;

    public Long getScenarioCategoryNo() {
        return scenarioCategoryNo;
    }

    public void setScenarioCategoryNo(Long scenarioCategoryNo) {
        this.scenarioCategoryNo = scenarioCategoryNo;
    }

    public Long getPScenarioCategoryNo() {
        return pScenarioCategoryNo;
    }

    public void setPScenarioCategoryNo(Long pScenarioCategoryNo) {
        this.pScenarioCategoryNo = pScenarioCategoryNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
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
