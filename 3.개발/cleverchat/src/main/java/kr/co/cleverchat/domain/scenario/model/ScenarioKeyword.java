package kr.co.cleverchat.domain.scenario.model;

import java.time.OffsetDateTime;

public class ScenarioKeyword {

    private Long scenarioKeywordNo;
    private Long scenarioNo;
    private String keyword;
    private int weight;
    private String useYn;
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;

    public Long getScenarioKeywordNo() {
        return scenarioKeywordNo;
    }

    public void setScenarioKeywordNo(Long scenarioKeywordNo) {
        this.scenarioKeywordNo = scenarioKeywordNo;
    }

    public Long getScenarioNo() {
        return scenarioNo;
    }

    public void setScenarioNo(Long scenarioNo) {
        this.scenarioNo = scenarioNo;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
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
