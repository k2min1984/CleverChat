package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatSessionListItem {

    private String id;
    private String sessionKey;
    private Long scenarioId;
    private String scenarioTitle;
    private String state;
    private Long currentNodeId;
    private OffsetDateTime startedAt;
    private OffsetDateTime lastActivityAt;
    private OffsetDateTime expiresAt;
    private int messageCount;
    private String lastMessage;
    private String lastMessageCiphertext;
    private String lastMessageKeyId;
    private Integer lastMessageEncryptionVersion;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getScenarioTitle() {
        return scenarioTitle;
    }

    public void setScenarioTitle(String scenarioTitle) {
        this.scenarioTitle = scenarioTitle;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(Long currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(OffsetDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageCiphertext() {
        return lastMessageCiphertext;
    }

    public void setLastMessageCiphertext(String lastMessageCiphertext) {
        this.lastMessageCiphertext = lastMessageCiphertext;
    }

    public String getLastMessageKeyId() {
        return lastMessageKeyId;
    }

    public void setLastMessageKeyId(String lastMessageKeyId) {
        this.lastMessageKeyId = lastMessageKeyId;
    }

    public Integer getLastMessageEncryptionVersion() {
        return lastMessageEncryptionVersion;
    }

    public void setLastMessageEncryptionVersion(Integer lastMessageEncryptionVersion) {
        this.lastMessageEncryptionVersion = lastMessageEncryptionVersion;
    }
}
