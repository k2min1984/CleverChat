package kr.co.cleverchat.domain.crawl.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.settings.RuntimeSetting;
import kr.co.cleverchat.domain.settings.RuntimeSettingsService;
import org.jsoup.Jsoup;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class HttpCrawlFetcher implements CrawlFetcher {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RuntimeSettingsService runtimeSettings;

    private final HttpClient httpClient;
    private final CrawlUrlPolicy urlPolicy;
    private final CrawlRobotsService robotsService;
    private static final int MAX_RESPONSE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 5;

    public HttpCrawlFetcher(CrawlUrlPolicy urlPolicy, CrawlRobotsService robotsService) {
        this.urlPolicy = urlPolicy;
        this.robotsService = robotsService;
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
    }

    @Override
    public FetchedPage fetch(String url) {
        URI current = urlPolicy.validateAndNormalize(url);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            robotsService.assertAllowed(current);
            HttpResponse<byte[]> response = request(current);
            int status = response.statusCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                if (redirects == MAX_REDIRECTS) {
                    throw failed("Crawl redirect limit exceeded.");
                }
                String location =
                        response.headers()
                                .firstValue("location")
                                .orElseThrow(
                                        () -> failed("Crawl redirect has no Location header."));
                try {
                    current = urlPolicy.validateAndNormalize(current.resolve(location).toString());
                } catch (IllegalArgumentException e) {
                    throw failed("Crawl redirect URL is invalid.");
                }
                continue;
            }
            if (status < 200 || status >= 300) {
                throw failed("Crawl fetch failed with HTTP " + status + ".");
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            if (!contentType.isBlank()
                    && !contentType.toLowerCase(java.util.Locale.ROOT).contains("html")) {
                throw failed("Crawl target is not HTML.");
            }
            try {
                var charset =
                        contentType.isBlank()
                                ? null
                                : MediaType.parseMediaType(contentType).getCharset();
                String html =
                        Jsoup.parse(
                                        new ByteArrayInputStream(response.body()),
                                        charset == null ? null : charset.name(),
                                        current.toString())
                                .outerHtml();
                return new FetchedPage(status, current.toString(), html);
            } catch (IOException | IllegalArgumentException e) {
                throw failed("Crawl HTML decoding failed: " + e.getMessage());
            }
        }
        throw failed("Crawl redirect limit exceeded.");
    }

    private HttpResponse<byte[]> request(URI uri) {
        HttpRequest request =
                HttpRequest.newBuilder(uri)
                        .timeout(
                                Duration.ofSeconds(
                                        runtimeSettings == null
                                                ? 10
                                                : runtimeSettings
                                                        .current()
                                                        .integer(
                                                                RuntimeSetting
                                                                        .HTTP_TIMEOUT_SECONDS)))
                        .header("User-Agent", "CleverChatCrawler/1.0")
                        .GET()
                        .build();
        try {
            return httpClient.send(
                    request,
                    info ->
                            new kr.co.cleverchat.common.http.LimitedBodySubscriber(
                                    MAX_RESPONSE_BYTES, "Crawl response exceeds 5 MiB."));
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.CRAWL_FETCH_FAILED, "Crawl fetch failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    ErrorCode.CRAWL_FETCH_FAILED, "Crawl fetch was interrupted.");
        }
    }

    private BusinessException failed(String message) {
        return new BusinessException(ErrorCode.CRAWL_FETCH_FAILED, message);
    }
}
