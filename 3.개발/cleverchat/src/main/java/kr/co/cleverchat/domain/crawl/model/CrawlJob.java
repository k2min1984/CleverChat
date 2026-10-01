package kr.co.cleverchat.domain.crawl.model;

import java.time.OffsetDateTime;

public class CrawlJob {
    private Long crawlJobNo;
    private Long targetNo;
    private String triggerType;
    private String status;
    private Long requestedBy;
    private OffsetDateTime requestedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private int attemptCount;
    private String message;

    public Long getCrawlJobNo() {
        return crawlJobNo;
    }

    public void setCrawlJobNo(Long crawlJobNo) {
        this.crawlJobNo = crawlJobNo;
    }

    public Long getTargetNo() {
        return targetNo;
    }

    public void setTargetNo(Long targetNo) {
        this.targetNo = targetNo;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Long requestedBy) {
        this.requestedBy = requestedBy;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
