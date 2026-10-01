package kr.co.cleverchat.domain.scenario.model;

import java.time.OffsetDateTime;

public class ScenarioNodeLink {

    private Long scenarioNodeLinkNo;
    private Long nodeNo;
    private String label;
    private String url;
    private String linkType;
    private int sortOrder;
    private String useYn;
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;

    public Long getScenarioNodeLinkNo() {
        return scenarioNodeLinkNo;
    }

    public void setScenarioNodeLinkNo(Long scenarioNodeLinkNo) {
        this.scenarioNodeLinkNo = scenarioNodeLinkNo;
    }

    public Long getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Long nodeNo) {
        this.nodeNo = nodeNo;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
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
