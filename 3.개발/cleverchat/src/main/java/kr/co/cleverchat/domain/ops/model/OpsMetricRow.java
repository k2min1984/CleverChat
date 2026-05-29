package kr.co.cleverchat.domain.ops.model;

public class OpsMetricRow {

    private String metricKey;
    private long todayCount;
    private long last7DaysCount;

    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    public long getTodayCount() {
        return todayCount;
    }

    public void setTodayCount(long todayCount) {
        this.todayCount = todayCount;
    }

    public long getLast7DaysCount() {
        return last7DaysCount;
    }

    public void setLast7DaysCount(long last7DaysCount) {
        this.last7DaysCount = last7DaysCount;
    }
}
