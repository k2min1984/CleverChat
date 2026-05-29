package kr.co.cleverchat.domain.crawl.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CrawlRobotsServiceTest {

    @Test
    void cachesRobotsTextByOriginAndAppliesRulesPerPath() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server =
                robotsServer(
                        requestCount,
                        """
                        User-agent: CleverChatCrawler
                        Disallow: /private
                        """);
        server.start();
        try {
            CrawlRobotsService service =
                    new CrawlRobotsService(
                            HttpClient.newHttpClient(),
                            Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneId.of("UTC")),
                            Duration.ofMinutes(10));
            URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

            CrawlRobotsService.RobotsDecision blocked =
                    service.check(baseUri.resolve("/private/page"));
            CrawlRobotsService.RobotsDecision allowed = service.check(baseUri.resolve("/public"));

            assertThat(blocked.allowed()).isFalse();
            assertThat(allowed.allowed()).isTrue();
            assertThat(requestCount).hasValue(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void refreshesRobotsTextAfterTtlExpires() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server =
                robotsServer(
                        requestCount,
                        """
                        User-agent: *
                        Disallow:
                        """);
        server.start();
        try {
            MutableClock clock = new MutableClock(Instant.parse("2026-05-29T00:00:00Z"));
            CrawlRobotsService service =
                    new CrawlRobotsService(
                            HttpClient.newHttpClient(), clock, Duration.ofSeconds(1));
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/help");

            service.check(uri);
            clock.advance(Duration.ofSeconds(2));
            service.check(uri);

            assertThat(requestCount).hasValue(2);
        } finally {
            server.stop(0);
        }
    }

    private HttpServer robotsServer(AtomicInteger requestCount, String robotsText)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/robots.txt",
                exchange -> {
                    requestCount.incrementAndGet();
                    byte[] response = robotsText.getBytes();
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });
        return server;
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
