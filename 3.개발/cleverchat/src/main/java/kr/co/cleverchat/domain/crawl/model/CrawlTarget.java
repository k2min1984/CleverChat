package kr.co.cleverchat.domain.crawl.model;

import java.time.OffsetDateTime;

public class CrawlTarget {
    private Long crawlTargetNo;
    private String url;
    private String label;
    private String useYn;
    private Long frstRegrEmpno;
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;
    private OffsetDateTime lastRunAt;
    private String lastStatus;
    private String lastMessage;
    private boolean scheduleEnabled;
    private Integer scheduleIntervalMinutes;
    private String scheduleMode;
    private String scheduleCron;
    private OffsetDateTime nextRunAt;
    private Boolean robotsAllowed;
    private OffsetDateTime robotsCheckedAt;
    private boolean jsonExportEnabled;
    private String jsonExportDirectory;

    public boolean isJsonExportEnabled() {
        return jsonExportEnabled;
    }

    public void setJsonExportEnabled(boolean jsonExportEnabled) {
        this.jsonExportEnabled = jsonExportEnabled;
    }

    public String getJsonExportDirectory() {
        return jsonExportDirectory;
    }

    public void setJsonExportDirectory(String jsonExportDirectory) {
        this.jsonExportDirectory = jsonExportDirectory;
    }

    public Long getCrawlTargetNo() {
        return crawlTargetNo;
    }

    public void setCrawlTargetNo(Long crawlTargetNo) {
        this.crawlTargetNo = crawlTargetNo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public Long getFrstRegrEmpno() {
        return frstRegrEmpno;
    }

    public void setFrstRegrEmpno(Long frstRegrEmpno) {
        this.frstRegrEmpno = frstRegrEmpno;
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

    public OffsetDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(OffsetDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public boolean isScheduleEnabled() {
        return scheduleEnabled;
    }

    public void setScheduleEnabled(boolean scheduleEnabled) {
        this.scheduleEnabled = scheduleEnabled;
    }

    public Integer getScheduleIntervalMinutes() {
        return scheduleIntervalMinutes;
    }

    public void setScheduleIntervalMinutes(Integer scheduleIntervalMinutes) {
        this.scheduleIntervalMinutes = scheduleIntervalMinutes;
    }

    public String getScheduleMode() {
        return scheduleMode;
    }

    public void setScheduleMode(String scheduleMode) {
        this.scheduleMode = scheduleMode;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public OffsetDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(OffsetDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public Boolean getRobotsAllowed() {
        return robotsAllowed;
    }

    public void setRobotsAllowed(Boolean robotsAllowed) {
        this.robotsAllowed = robotsAllowed;
    }

    public OffsetDateTime getRobotsCheckedAt() {
        return robotsCheckedAt;
    }

    public void setRobotsCheckedAt(OffsetDateTime robotsCheckedAt) {
        this.robotsCheckedAt = robotsCheckedAt;
    }
}
