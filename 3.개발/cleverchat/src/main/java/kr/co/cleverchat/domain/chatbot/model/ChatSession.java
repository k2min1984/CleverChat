package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatSession {

    private String chatSessionNo;
    private String anonymousId;
    private Long userNo;
    private Long scenarioNo;
    private Long versionNo;
    private Long currentNodeNo;
    private String sessionType;
    private String state;
    private OffsetDateTime startedAt;
    private OffsetDateTime lastActivityAt;
    private OffsetDateTime expiresAt;
    private String ipHash;
    private String userAgentHash;
    private OffsetDateTime frstRegDt;

    public String getChatSessionNo() {
        return chatSessionNo;
    }

    public void setChatSessionNo(String chatSessionNo) {
        this.chatSessionNo = chatSessionNo;
    }

    public String getAnonymousId() {
        return anonymousId;
    }

    public void setAnonymousId(String anonymousId) {
        this.anonymousId = anonymousId;
    }

    public Long getUserNo() {
        return userNo;
    }

    public void setUserNo(Long userNo) {
        this.userNo = userNo;
    }

    public Long getScenarioNo() {
        return scenarioNo;
    }

    public void setScenarioNo(Long scenarioNo) {
        this.scenarioNo = scenarioNo;
    }

    public Long getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Long versionNo) {
        this.versionNo = versionNo;
    }

    public Long getCurrentNodeNo() {
        return currentNodeNo;
    }

    public void setCurrentNodeNo(Long currentNodeNo) {
        this.currentNodeNo = currentNodeNo;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getIpHash() {
        return ipHash;
    }

    public void setIpHash(String ipHash) {
        this.ipHash = ipHash;
    }

    public String getUserAgentHash() {
        return userAgentHash;
    }

    public void setUserAgentHash(String userAgentHash) {
        this.userAgentHash = userAgentHash;
    }

    public OffsetDateTime getFrstRegDt() {
        return frstRegDt;
    }

    public void setFrstRegDt(OffsetDateTime frstRegDt) {
        this.frstRegDt = frstRegDt;
    }
}
