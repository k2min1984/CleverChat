package kr.co.cleverchat.domain.crawl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class KepcoStaticMenuIntegrationTest {

    private static final String HOME = "https://www.kepco.co.kr/home/index.do";

    @Test
    void parsesEveryOfficialStaticMenuPage() throws Exception {
        assumeTrue(
                "true".equalsIgnoreCase(System.getenv("CLEVERCHAT_KEPCO_ALL_BOARDS")),
                "CLEVERCHAT_KEPCO_ALL_BOARDS is not enabled.");

        Connection.Response homeResponse = fetch(HOME);
        Document sitemap = Jsoup.parse(homeResponse.body(), HOME);
        Set<String> staticUrls = new LinkedHashSet<>();
        for (Element link : sitemap.select(".sitemap-container a[href]")) {
            URI uri = URI.create(HOME).resolve(link.attr("href")).normalize();
            if (isStaticOfficialMenu(uri)) {
                staticUrls.add(uri.toString());
            }
        }

        CrawlService service = new CrawlService(null, null, null, null);
        Method parseHtml =
                CrawlService.class.getDeclaredMethod("parseHtml", String.class, String.class);
        parseHtml.setAccessible(true);
        List<String> failures = new ArrayList<>();
        for (String url : staticUrls) {
            try {
                Connection.Response response = fetch(url);
                parseHtml.invoke(service, response.url().toString(), response.body());
            } catch (InvocationTargetException e) {
                failures.add(url + " failed: " + e.getTargetException().getMessage());
            } catch (RuntimeException e) {
                failures.add(url + " failed: " + e.getMessage());
            }
        }

        // The official sitemap grows over time; require its core pages and parse every discovered URL.
        assertThat(staticUrls).contains(
                "https://www.kepco.co.kr/home/about/introduce/overview.do",
                "https://www.kepco.co.kr/home/about/introduce/history.do",
                "https://www.kepco.co.kr/home/about/introduce/ceo.do");
        System.out.printf("[LIVE] parsed official static menu pages: %d%n", staticUrls.size());
        assertThat(failures).isEmpty();
    }

    private Connection.Response fetch(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent("CleverChatBot/1.0")
                .timeout(30_000)
                .followRedirects(true)
                .execute();
    }

    private boolean isStaticOfficialMenu(URI uri) {
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        return "www.kepco.co.kr".equalsIgnoreCase(uri.getHost())
                && path.startsWith("/home/")
                && !"/home/".equals(path)
                && !path.endsWith("/boardlist.do")
                && !path.contains("/search/")
                && !path.contains("login")
                && !path.contains("logout")
                && !path.contains("member")
                && !path.contains("auth")
                && !path.contains("certification")
                && !path.endsWith("/photo/index.do")
                && !path.endsWith("/gallery/index.do");
    }
}
