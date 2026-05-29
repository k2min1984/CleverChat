package kr.co.cleverchat.domain.search.model;

public class SearchResultItem {

    private Long scenarioId;
    private String scenarioTitle;
    private Long crawlDocumentId;
    private String crawlUrl;
    private String matchedField;
    private double score;
    private String snippet;

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getScenarioTitle() {
        return scenarioTitle;
    }

    public void setScenarioTitle(String scenarioTitle) {
        this.scenarioTitle = scenarioTitle;
    }

    public Long getCrawlDocumentId() {
        return crawlDocumentId;
    }

    public void setCrawlDocumentId(Long crawlDocumentId) {
        this.crawlDocumentId = crawlDocumentId;
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
