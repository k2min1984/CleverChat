package kr.co.cleverchat.domain.scenario.model;

import java.time.OffsetDateTime;

public class ScenarioSynonym {

    private Long scenarioSynonymNo;
    private Long keywordNo;
    private String synonym;
    private int weight;
    private String useYn;
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;

    public Long getScenarioSynonymNo() {
        return scenarioSynonymNo;
    }

    public void setScenarioSynonymNo(Long scenarioSynonymNo) {
        this.scenarioSynonymNo = scenarioSynonymNo;
    }

    public Long getKeywordNo() {
        return keywordNo;
    }

    public void setKeywordNo(Long keywordNo) {
        this.keywordNo = keywordNo;
    }

    public String getSynonym() {
        return synonym;
    }

    public void setSynonym(String synonym) {
        this.synonym = synonym;
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
