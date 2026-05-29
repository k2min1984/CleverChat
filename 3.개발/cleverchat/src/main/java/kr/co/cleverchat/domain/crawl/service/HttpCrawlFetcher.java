package kr.co.cleverchat.domain.crawl.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class HttpCrawlFetcher implements CrawlFetcher {

    private final HttpClient httpClient;

    public HttpCrawlFetcher() {
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
    }

    @Override
    public FetchedPage fetch(String url) {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "CleverChatCrawler/1.0")
                        .GET()
                        .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new BusinessException(
                        ErrorCode.CRAWL_FETCH_FAILED,
                        "Crawl fetch failed with HTTP " + status + ".");
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            if (!contentType.isBlank() && !contentType.toLowerCase().contains("html")) {
                throw new BusinessException(
                        ErrorCode.CRAWL_FETCH_FAILED, "Crawl target is not HTML.");
            }
            return new FetchedPage(status, response.uri().toString(), response.body());
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.CRAWL_FETCH_FAILED, "Crawl fetch failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    ErrorCode.CRAWL_FETCH_FAILED, "Crawl fetch was interrupted.");
        }
    }
}
