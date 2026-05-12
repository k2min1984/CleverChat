package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ChatSession {

    private UUID id;
    private UUID anonymousId;
    private Long userId;
    private Long scenarioId;
    private Long versionId;
    private Long currentNodeId;
    private String state;
    private OffsetDateTime startedAt;
    private OffsetDateTime lastActivityAt;
    private OffsetDateTime expiresAt;
    private String ipHash;
    private String userAgentHash;
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAnonymousId() { return anonymousId; }
    public void setAnonymousId(UUID anonymousId) { this.anonymousId = anonymousId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Long getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(Long currentNodeId) { this.currentNodeId = currentNodeId; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(OffsetDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getIpHash() { return ipHash; }
    public void setIpHash(String ipHash) { this.ipHash = ipHash; }
    public String getUserAgentHash() { return userAgentHash; }
    public void setUserAgentHash(String userAgentHash) { this.userAgentHash = userAgentHash; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
