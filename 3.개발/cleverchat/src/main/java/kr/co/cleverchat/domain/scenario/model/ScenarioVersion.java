package kr.co.cleverchat.domain.scenario.model;

import java.time.OffsetDateTime;

public class ScenarioVersion {

    private Long scenarioVersionNo;
    private Long scenarioNo;
    private int versionNo;
    private String status;
    private Long startNodeNo;
    private String frstRegrEmpno;
    private OffsetDateTime publishedAt;
    private OffsetDateTime frstRegDt;

    public Long getScenarioVersionNo() {
        return scenarioVersionNo;
    }

    public void setScenarioVersionNo(Long scenarioVersionNo) {
        this.scenarioVersionNo = scenarioVersionNo;
    }

    public Long getScenarioNo() {
        return scenarioNo;
    }

    public void setScenarioNo(Long scenarioNo) {
        this.scenarioNo = scenarioNo;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getStartNodeNo() {
        return startNodeNo;
    }

    public void setStartNodeNo(Long startNodeNo) {
        this.startNodeNo = startNodeNo;
    }

    public String getFrstRegrEmpno() {
        return frstRegrEmpno;
    }

    public void setFrstRegrEmpno(String frstRegrEmpno) {
        this.frstRegrEmpno = frstRegrEmpno;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OffsetDateTime getFrstRegDt() {
        return frstRegDt;
    }

    public void setFrstRegDt(OffsetDateTime frstRegDt) {
        this.frstRegDt = frstRegDt;
    }
}
