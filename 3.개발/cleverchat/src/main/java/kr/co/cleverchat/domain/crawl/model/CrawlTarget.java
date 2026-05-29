package kr.co.cleverchat.domain.crawl.model;

import java.time.OffsetDateTime;

public class CrawlTarget {
    private Long id;
    private String url;
    private String label;
    private boolean enabled;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
