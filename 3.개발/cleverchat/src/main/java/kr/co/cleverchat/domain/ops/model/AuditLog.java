package kr.co.cleverchat.domain.ops.model;

import java.time.OffsetDateTime;

public class AuditLog {

    private Long auditLogNo;
    private String actor;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private String ip;
    private OffsetDateTime frstRegDt;

    public Long getAuditLogNo() {
        return auditLogNo;
    }

    public void setAuditLogNo(Long auditLogNo) {
        this.auditLogNo = auditLogNo;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public OffsetDateTime getFrstRegDt() {
        return frstRegDt;
    }

    public void setFrstRegDt(OffsetDateTime frstRegDt) {
        this.frstRegDt = frstRegDt;
    }
}
