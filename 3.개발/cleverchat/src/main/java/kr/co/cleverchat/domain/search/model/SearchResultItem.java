package kr.co.cleverchat.domain.search.model;

public class SearchResultItem {

    private Long scenarioNo;
    private String scenarioTitle;
    private Long crawlDocumentNo;
    private String crawlUrl;
    private String matchedField;
    private double score;
    private String snippet;

    public Long getScenarioNo() {
        return scenarioNo;
    }

    public void setScenarioNo(Long scenarioNo) {
        this.scenarioNo = scenarioNo;
    }

    public String getScenarioTitle() {
        return scenarioTitle;
    }

    public void setScenarioTitle(String scenarioTitle) {
        this.scenarioTitle = scenarioTitle;
    }

    public Long getCrawlDocumentNo() {
        return crawlDocumentNo;
    }

    public void setCrawlDocumentNo(Long crawlDocumentNo) {
        this.crawlDocumentNo = crawlDocumentNo;
    }

    public String getCrawlUrl() {
        return crawlUrl;
    }

    public void setCrawlUrl(String crawlUrl) {
        this.crawlUrl = crawlUrl;
    }

    public String getMatchedField() {
        return matchedField;
    }

    public void setMatchedField(String matchedField) {
        this.matchedField = matchedField;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}
