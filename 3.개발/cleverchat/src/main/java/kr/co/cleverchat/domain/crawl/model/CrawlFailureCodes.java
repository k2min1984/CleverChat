package kr.co.cleverchat.domain.crawl.model;

public final class CrawlFailureCodes {
    public static final String PATTERN =
            "ROBOTS_BLOCKED|HTTP_ERROR|TIMEOUT|PARSE_ERROR|DUP_HASH|FETCH_ERROR|NOT_HTML|URL_BLOCKED|SCHEDULE_INVALID|SYSTEM_ERROR|BROWSER_ERROR|EXPORT_ERROR";

    private CrawlFailureCodes() {}
}
