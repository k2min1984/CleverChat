package kr.co.cleverchat.domain.ops.model;

import java.time.OffsetDateTime;

public class Notice {

    private Long noticeNo;
    private String title;
    private String content;
    private String useYn;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private int priority;
    private Long frstRegrEmpno;
    private Long lstChgrEmpno;
    private OffsetDateTime frstRegDt;
    private OffsetDateTime lstChgDt;

    public Long getNoticeNo() {
        return noticeNo;
    }

    public void setNoticeNo(Long noticeNo) {
        this.noticeNo = noticeNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(OffsetDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(OffsetDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Long getFrstRegrEmpno() {
        return frstRegrEmpno;
    }

    public void setFrstRegrEmpno(Long frstRegrEmpno) {
        this.frstRegrEmpno = frstRegrEmpno;
    }

    public Long getLstChgrEmpno() {
        return lstChgrEmpno;
    }

    public void setLstChgrEmpno(Long lstChgrEmpno) {
        this.lstChgrEmpno = lstChgrEmpno;
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
