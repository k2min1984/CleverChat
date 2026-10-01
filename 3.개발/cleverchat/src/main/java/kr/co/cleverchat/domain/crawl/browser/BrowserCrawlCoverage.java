package kr.co.cleverchat.domain.crawl.browser;

public record BrowserCrawlCoverage(
        int listPages,
        int listItemsFound,
        int detailsFetched,
        int detailsFailed,
        boolean truncated) {}
