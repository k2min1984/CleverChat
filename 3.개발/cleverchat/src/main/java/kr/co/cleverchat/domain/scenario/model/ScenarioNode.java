package kr.co.cleverchat.domain.scenario.model;

import java.time.OffsetDateTime;

public class ScenarioNode {

    private Long scenarioNodeNo;
    private Long versionNo;
    private String nodeKey;
    private String nodeType;
    private String title;
    private String content;
    private int sortOrder;
    private String metadata = "{}";
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;

    public Long getScenarioNodeNo() {
        return scenarioNodeNo;
    }

    public void setScenarioNodeNo(Long scenarioNodeNo) {
        this.scenarioNodeNo = scenarioNodeNo;
    }

    public Long getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Long versionNo) {
        this.versionNo = versionNo;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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
