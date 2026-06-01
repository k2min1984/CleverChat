package kr.co.cleverchat.domain.chatbot.model;

import java.time.OffsetDateTime;

public class ChatRecommendation {

    private Long chatRecommendationNo;
    private Long scenarioNo;
    private String label;
    private int priority;
    private String useYn;
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;

    public Long getChatRecommendationNo() {
        return chatRecommendationNo;
    }

    public void setChatRecommendationNo(Long chatRecommendationNo) {
        this.chatRecommendationNo = chatRecommendationNo;
    }

    public Long getScenarioNo() {
        return scenarioNo;
    }

    public void setScenarioNo(Long scenarioNo) {
        this.scenarioNo = scenarioNo;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
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
