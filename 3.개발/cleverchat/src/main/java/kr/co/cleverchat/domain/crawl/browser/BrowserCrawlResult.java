package kr.co.cleverchat.domain.crawl.browser;

import java.util.List;

public record BrowserCrawlResult(List<BrowserCrawlPage> pages, BrowserCrawlCoverage coverage) {}
