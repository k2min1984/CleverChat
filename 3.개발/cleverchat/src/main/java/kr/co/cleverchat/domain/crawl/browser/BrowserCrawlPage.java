package kr.co.cleverchat.domain.crawl.browser;

public record BrowserCrawlPage(String url, String title, String content, Integer httpStatus) {}
