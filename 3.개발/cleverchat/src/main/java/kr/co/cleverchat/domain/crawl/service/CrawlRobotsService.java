package kr.co.cleverchat.domain.crawl.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class CrawlRobotsService {

    private static final String USER_AGENT = "CleverChatCrawler";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(10);

    private final HttpClient httpClient;
    private final Clock clock;
    private final Duration cacheTtl;
    private final Map<String, CachedRobots> cache = new ConcurrentHashMap<>();

    public CrawlRobotsService() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                Clock.systemUTC(),
                DEFAULT_CACHE_TTL);
    }

    CrawlRobotsService(HttpClient httpClient, Clock clock, Duration cacheTtl) {
        this.httpClient = httpClient;
        this.clock = clock;
        this.cacheTtl = cacheTtl == null ? DEFAULT_CACHE_TTL : cacheTtl;
    }

    public RobotsDecision check(URI targetUri) {
        CachedRobots cached = cachedPolicy(targetUri);
        if (cached.robotsText() == null) {
            return new RobotsDecision(cached.allowed(), cached.message());
        }
        boolean allowed = isAllowed(cached.robotsText(), targetUri.getRawPath());
        return new RobotsDecision(
                allowed, allowed ? "robots.txt allows crawl." : "robots.txt disallows crawl.");
    }

    private CachedRobots cachedPolicy(URI targetUri) {
        String origin = origin(targetUri);
        Instant now = Instant.now(clock);
        CachedRobots cached = cache.get(origin);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached;
        }
        CachedRobots fetched = fetchPolicy(targetUri, now);
        cache.put(origin, fetched);
        return fetched;
    }

    private CachedRobots fetchPolicy(URI targetUri, Instant now) {
        URI robotsUri = URI.create(origin(targetUri) + "/robots.txt");
        HttpRequest request =
                HttpRequest.newBuilder(robotsUri)
                        .timeout(Duration.ofSeconds(5))
                        .header("User-Agent", USER_AGENT)
                        .GET()
                        .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return cached(null, true, "robots.txt not found.", now);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return cached(
                        null,
                        null,
                        "robots.txt check failed with HTTP " + response.statusCode() + ".",
                        now);
            }
            return cached(response.body(), null, null, now);
        } catch (IOException e) {
            return cached(null, null, "robots.txt check failed: " + e.getMessage(), now);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return cached(null, null, "robots.txt check was interrupted.", now);
        }
    }

    private CachedRobots cached(String robotsText, Boolean allowed, String message, Instant now) {
        return new CachedRobots(robotsText, allowed, message, now.plus(cacheTtl));
    }

    private String origin(URI uri) {
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    public void assertAllowed(URI targetUri) {
        RobotsDecision decision = check(targetUri);
        if (Boolean.FALSE.equals(decision.allowed())) {
            throw new BusinessException(ErrorCode.CRAWL_ROBOTS_BLOCKED, decision.message());
        }
    }

    private boolean isAllowed(String robotsText, String rawPath) {
        String path = rawPath == null || rawPath.isBlank() ? "/" : rawPath;
        List<Rule> applicableRules = parseApplicableRules(robotsText);
        Rule best = null;
        for (Rule rule : applicableRules) {
            if (rule.path().isEmpty()) {
                continue;
            }
            if (path.startsWith(rule.path())
                    && (best == null || rule.path().length() > best.path().length())) {
                best = rule;
            }
        }
        return best == null || best.allow();
    }

    private List<Rule> parseApplicableRules(String robotsText) {
        List<Rule> rules = new ArrayList<>();
        boolean applies = false;
        boolean sawDirectiveInGroup = false;
        for (String rawLine : (robotsText == null ? "" : robotsText).split("\\R")) {
            String line = rawLine.split("#", 2)[0].trim();
            if (line.isBlank()) {
                applies = false;
                sawDirectiveInGroup = false;
                continue;
            }
            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            String field = parts[0].trim().toLowerCase(Locale.ROOT);
            String value = parts[1].trim();
            if ("user-agent".equals(field)) {
                if (sawDirectiveInGroup) {
                    applies = false;
                    sawDirectiveInGroup = false;
                }
                String agent = value.toLowerCase(Locale.ROOT);
                applies =
                        applies
                                || "*".equals(agent)
                                || USER_AGENT.toLowerCase(Locale.ROOT).equals(agent);
                continue;
            }
            if (("allow".equals(field) || "disallow".equals(field)) && applies) {
                sawDirectiveInGroup = true;
                rules.add(new Rule("allow".equals(field), value));
            }
        }
        return rules;
    }

    public record RobotsDecision(Boolean allowed, String message) {}

    private record CachedRobots(
            String robotsText, Boolean allowed, String message, Instant expiresAt) {}

    private record Rule(boolean allow, String path) {}
}
