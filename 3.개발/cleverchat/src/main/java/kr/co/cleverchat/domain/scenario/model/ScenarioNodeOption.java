package kr.co.cleverchat.domain.scenario.model;

import java.time.OffsetDateTime;

public class ScenarioNodeOption {

    private Long scenarioNodeOptionNo;
    private Long nodeNo;
    private Long nextNodeNo;
    private String label;
    private String conditionExpr;
    private int sortOrder;
    private String useYn;
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;

    public Long getScenarioNodeOptionNo() {
        return scenarioNodeOptionNo;
    }

    public void setScenarioNodeOptionNo(Long scenarioNodeOptionNo) {
        this.scenarioNodeOptionNo = scenarioNodeOptionNo;
    }

    public Long getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Long nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Long getNextNodeNo() {
        return nextNodeNo;
    }

    public void setNextNodeNo(Long nextNodeNo) {
        this.nextNodeNo = nextNodeNo;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getConditionExpr() {
        return conditionExpr;
    }

    public void setConditionExpr(String conditionExpr) {
        this.conditionExpr = conditionExpr;
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
