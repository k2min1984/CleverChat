package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatSessionListItem {

    private String chatSessionNo;
    private String sessionKey;
    private Long scenarioNo;
    private String scenarioTitle;
    private String state;
    private Long currentNodeNo;
    private OffsetDateTime startedAt;
    private OffsetDateTime lastActivityAt;
    private OffsetDateTime expiresAt;
    private int messageCount;
    private String lastMessage;
    private String lastMessageCiphertext;
    private String lastMessageKeyId;
    private Integer lastMessageEncryptionVersion;

    public String getChatSessionNo() {
        return chatSessionNo;
    }

    public void setChatSessionNo(String chatSessionNo) {
        this.chatSessionNo = chatSessionNo;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Long getScenarioNo() {
        return scenarioNo;
    }

    public void setScenarioNo(Long scenarioNo) {
        this.scenarioNo = scenarioNo;
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

    public Long getCurrentNodeNo() {
        return currentNodeNo;
    }

    public void setCurrentNodeNo(Long currentNodeNo) {
        this.currentNodeNo = currentNodeNo;
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
