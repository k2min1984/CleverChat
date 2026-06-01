package kr.co.cleverchat.domain.scenario.dto;

public class ScenarioSynonymRow {

    private Long scenarioNo;
    private Long keywordNo;
    private String synonym;
    private int weight;

    public Long getScenarioNo() {
        return scenarioNo;
    }

    public void setScenarioNo(Long scenarioNo) {
        this.scenarioNo = scenarioNo;
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
}
