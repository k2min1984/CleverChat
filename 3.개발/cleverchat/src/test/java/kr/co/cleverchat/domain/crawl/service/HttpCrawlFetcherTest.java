package kr.co.cleverchat.domain.crawl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpCrawlFetcherTest {
    private HttpServer server;
    private String base;
    private HttpCrawlFetcher fetcher;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/robots.txt",
                exchange -> {
                    byte[] bytes =
                            "User-agent: *\nDisallow: /blocked".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (var output = exchange.getResponseBody()) {
                        output.write(bytes);
                    }
                });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        fetcher = new HttpCrawlFetcher(new CrawlUrlPolicy("127.0.0.1"), new CrawlRobotsService());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void followsRelativeRedirectAndDecodesKoreanHtmlMetadata() {
        redirect("/start", "/article");
        page(
                "/article",
                "text/html",
                "<meta charset='EUC-KR'><title>전기요금</title><p>납부 안내</p>"
                        .getBytes(Charset.forName("EUC-KR")));
        var page = fetcher.fetch(base + "/start");
        assertThat(page.finalUrl()).isEqualTo(base + "/article");
        assertThat(page.html()).contains("전기요금", "납부 안내");
    }

    @Test
    void revalidatesRedirectHostBeforeMakingRequest() {
        redirect("/start", "http://localhost:" + server.getAddress().getPort() + "/private");
        assertThatThrownBy(() -> fetcher.fetch(base + "/start"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CRAWL_URL_BLOCKED);
    }

    @Test
    void enforcesRobotsOnRedirectDestination() {
        AtomicInteger requests = new AtomicInteger();
        server.createContext(
                "/blocked",
                exchange -> {
                    requests.incrementAndGet();
                    exchange.close();
                });
        redirect("/start", "/blocked");
        assertThatThrownBy(() -> fetcher.fetch(base + "/start"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CRAWL_ROBOTS_BLOCKED);
        assertThat(requests).hasValue(0);
    }

    @Test
    void rejectsRedirectLoopNonHtmlAndOversizedResponse() {
        redirect("/loop", "/loop");
        page("/json", "application/json", "{}".getBytes(StandardCharsets.UTF_8));
        page("/large", "text/html", new byte[5 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> fetcher.fetch(base + "/loop"))
                .hasMessageContaining("redirect limit");
        assertThatThrownBy(() -> fetcher.fetch(base + "/json")).hasMessageContaining("not HTML");
        assertThatThrownBy(() -> fetcher.fetch(base + "/large")).hasMessageContaining("5 MiB");
    }

    private void redirect(String path, String location) {
        server.createContext(
                path,
                exchange -> {
                    exchange.getResponseHeaders().set("Location", location);
                    exchange.sendResponseHeaders(302, -1);
                    exchange.close();
                });
    }

    private void page(String path, String contentType, byte[] bytes) {
        server.createContext(
                path,
                exchange -> {
                    exchange.getResponseHeaders().set("Content-Type", contentType);
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (var output = exchange.getResponseBody()) {
                        output.write(bytes);
                    }
                });
    }
}
