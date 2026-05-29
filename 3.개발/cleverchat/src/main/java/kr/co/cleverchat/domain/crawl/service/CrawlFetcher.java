package kr.co.cleverchat.domain.crawl.service;

public interface CrawlFetcher {
    FetchedPage fetch(String url);

    record FetchedPage(int httpStatus, String finalUrl, String html) {}
}
