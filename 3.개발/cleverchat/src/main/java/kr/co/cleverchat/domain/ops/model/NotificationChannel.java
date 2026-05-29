package kr.co.cleverchat.domain.ops.model;

import java.time.OffsetDateTime;

public class NotificationChannel {
    private Long id;
    private String name;
    private String type;
    private boolean enabled;
    private String endpointEnvKey;
    private String previousEndpointEnvKey;
    private Integer rateLimitPerHour;
    private Long createdBy;
    private Long updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
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
}
