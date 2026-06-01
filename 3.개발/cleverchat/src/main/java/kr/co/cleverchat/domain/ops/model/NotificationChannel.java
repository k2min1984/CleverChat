package kr.co.cleverchat.domain.ops.model;

import java.time.OffsetDateTime;

public class NotificationChannel {
    private Long notificationChannelNo;
    private String name;
    private String type;
    private String useYn;
    private String endpointEnvKey;
    private String previousEndpointEnvKey;
    private Integer rateLimitPerHour;
    private Long frstRegrEmpno;
    private Long lstChgrEmpno;
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;

    public Long getNotificationChannelNo() {
        return notificationChannelNo;
    }

    public void setNotificationChannelNo(Long notificationChannelNo) {
        this.notificationChannelNo = notificationChannelNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public String getEndpointEnvKey() {
        return endpointEnvKey;
    }

    public void setEndpointEnvKey(String endpointEnvKey) {
        this.endpointEnvKey = endpointEnvKey;
    }

    public String getPreviousEndpointEnvKey() {
        return previousEndpointEnvKey;
    }

    public void setPreviousEndpointEnvKey(String previousEndpointEnvKey) {
        this.previousEndpointEnvKey = previousEndpointEnvKey;
    }

    public Integer getRateLimitPerHour() {
        return rateLimitPerHour;
    }

    public void setRateLimitPerHour(Integer rateLimitPerHour) {
        this.rateLimitPerHour = rateLimitPerHour;
    }

    public Long getFrstRegrEmpno() {
        return frstRegrEmpno;
    }

    public void setFrstRegrEmpno(Long frstRegrEmpno) {
        this.frstRegrEmpno = frstRegrEmpno;
    }

    public Long getLstChgrEmpno() {
        return lstChgrEmpno;
    }

    public void setLstChgrEmpno(Long lstChgrEmpno) {
        this.lstChgrEmpno = lstChgrEmpno;
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
