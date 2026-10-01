package kr.co.cleverchat.domain.adminmanage.model;

import java.time.OffsetDateTime;

public class AdminIpWhitelist {
    private Long adminIpWhitelistNo;
    private String ipCidr;
    private String description;
    private String useYn;
    private String frstRegrEmpno;
    private OffsetDateTime frstRegDt;
    private String lstChgrEmpno;
    private OffsetDateTime lstChgDt;
    private String frstRegrIp;
    private String lstChgrIp;

    public Long getAdminIpWhitelistNo() {
        return adminIpWhitelistNo;
    }

    public void setAdminIpWhitelistNo(Long adminIpWhitelistNo) {
        this.adminIpWhitelistNo = adminIpWhitelistNo;
    }

    public String getIpCidr() {
        return ipCidr;
    }

    public void setIpCidr(String ipCidr) {
        this.ipCidr = ipCidr;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public String getFrstRegrEmpno() {
        return frstRegrEmpno;
    }

    public void setFrstRegrEmpno(String frstRegrEmpno) {
        this.frstRegrEmpno = frstRegrEmpno;
    }

    public OffsetDateTime getFrstRegDt() {
        return frstRegDt;
    }

    public void setFrstRegDt(OffsetDateTime frstRegDt) {
        this.frstRegDt = frstRegDt;
    }

    public String getLstChgrEmpno() {
        return lstChgrEmpno;
    }

    public void setLstChgrEmpno(String lstChgrEmpno) {
        this.lstChgrEmpno = lstChgrEmpno;
    }

    public OffsetDateTime getLstChgDt() {
        return lstChgDt;
    }

    public void setLstChgDt(OffsetDateTime lstChgDt) {
        this.lstChgDt = lstChgDt;
    }

    public String getFrstRegrIp() {
        return frstRegrIp;
    }

    public void setFrstRegrIp(String frstRegrIp) {
        this.frstRegrIp = frstRegrIp;
    }

    public String getLstChgrIp() {
        return lstChgrIp;
    }

    public void setLstChgrIp(String lstChgrIp) {
        this.lstChgrIp = lstChgrIp;
    }
}
