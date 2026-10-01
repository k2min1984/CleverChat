package kr.co.cleverchat.domain.crawl.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class KepcoBoardCrawlerIntegrationTest {

    private static final List<String> KEPCO_BOARD_URLS =
            List.of(
                    "https://www.kepco.co.kr/home/about/invest/announce/boardList.do",
                    "https://www.kepco.co.kr/home/about/invest/fininfo/finstatements/boardList.do",
                    "https://www.kepco.co.kr/home/about/invest/irinfo/irreport/boardList.do",
                    "https://www.kepco.co.kr/home/about/orgaznization/global/boardList.do",
                    "https://www.kepco.co.kr/home/customer/library/electricity-statistics/kepco-stats/boardList.do",
                    "https://www.kepco.co.kr/home/customer/public-resources/boardList.do",
                    "https://www.kepco.co.kr/home/disclosure/addisclosure/boardList.do",
                    "https://www.kepco.co.kr/home/disclosure/dissystem/analysis/disforms/boardList.do",
                    "https://www.kepco.co.kr/home/disclosure/mandisclosure/etc/objectives/boardList.do",
                    "https://www.kepco.co.kr/home/disclosure/realnameguide/boardList.do",
                    "https://www.kepco.co.kr/home/disclosure/regulations/internalrule/boardList.do",
                    "https://www.kepco.co.kr/home/disclosure/regulations/revision/revision/boardList.do",
                    "https://www.kepco.co.kr/home/esg/esgreport/sustainability/boardList.do",
                    "https://www.kepco.co.kr/home/media/newsroom/advertise/tv/boardList.do",
                    "https://www.kepco.co.kr/home/media/newsroom/magazine/boardList.do",
                    "https://www.kepco.co.kr/home/media/newsroom/notice/boardList.do",
                    "https://www.kepco.co.kr/home/media/newsroom/pr/boardList.do",
                    "https://www.kepco.co.kr/home/media/newsroom/social/boardList.do",
                    "https://www.kepco.co.kr/home/media/newsroom/videos/boardList.do");

    @Test
    void crawlsConfiguredKepcoBoard() {
        String url = System.getenv("CLEVERCHAT_KEPCO_BOARD_URL");
        assumeTrue(url != null && !url.isBlank(), "CLEVERCHAT_KEPCO_BOARD_URL is not set.");

        CrawlBrowserProperties properties = new CrawlBrowserProperties();
        properties.setEnabled(true);
        boolean fullBoard = "true".equalsIgnoreCase(System.getenv("CLEVERCHAT_KEPCO_FULL_BOARD"));
        properties.setMaxPages(fullBoard ? 0 : 1);
        properties.setMaxDetails(fullBoard ? 0 : 1);
        properties.setBrowsersPath(System.getenv("PLAYWRIGHT_BROWSERS_PATH"));
        properties.setNativeLibraryPath(
                System.getenv("CLEVERCHAT_CRAWL_BROWSER_NATIVE_LIBRARY_PATH"));
        BoardCrawler crawler =
                new BoardCrawler(new PlaywrightBrowserProvider(properties), properties);

        BrowserCrawlResult result = crawler.crawl(url);

        assertThat(result.coverage().listPages()).isGreaterThanOrEqualTo(1);
        assertThat(result.coverage().listItemsFound()).isGreaterThan(0);
        assertThat(result.coverage().detailsFetched()).isGreaterThan(0);
        assertThat(result.pages()).isNotEmpty();
        String content = result.pages().get(0).content();
        assertThat(content).isNotBlank();
        // Some live posts have an image-only body: preserve the title fallback.
        assertThat(result.pages().get(0).title()).isNotBlank();
        assertThat(result.pages().get(0).url()).contains("/boardView.do");
        assertThat(content).doesNotContain("만족하셨습니까", "자동 로그아웃", "페이지 번호 입력");
    }

    @Test
    void recognizesEveryKepcoBoardMenuLayout() {
        assumeTrue(
                "true".equalsIgnoreCase(System.getenv("CLEVERCHAT_KEPCO_ALL_BOARDS")),
                "CLEVERCHAT_KEPCO_ALL_BOARDS is not enabled.");

        CrawlBrowserProperties properties = new CrawlBrowserProperties();
        properties.setEnabled(true);
        properties.setMaxPages(1);
        properties.setMaxDetails(1);
        properties.setBrowsersPath(System.getenv("PLAYWRIGHT_BROWSERS_PATH"));
        BoardCrawler crawler =
                new BoardCrawler(new PlaywrightBrowserProvider(properties), properties);
        List<String> failures = new ArrayList<>();

        for (String url : KEPCO_BOARD_URLS) {
            try {
                BrowserCrawlResult result = crawler.crawl(url);
                if (result.coverage().listItemsFound() < 1 || result.pages().isEmpty()) {
                    failures.add(url + " produced no list items or documents");
                }
            } catch (RuntimeException e) {
                failures.add(
                        url + " failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        assertThat(failures).isEmpty();
    }

    @Test
    void recognizesNestedKepcoBoardsIncludingKnownEmptyBoard() {
        assumeTrue(
                "true".equalsIgnoreCase(System.getenv("CLEVERCHAT_KEPCO_ALL_BOARDS")),
                "CLEVERCHAT_KEPCO_ALL_BOARDS is not enabled.");

        CrawlBrowserProperties properties = new CrawlBrowserProperties();
        properties.setEnabled(true);
        properties.setMaxPages(1);
        properties.setMaxDetails(1);
        properties.setBrowsersPath(System.getenv("PLAYWRIGHT_BROWSERS_PATH"));
        BoardCrawler crawler =
                new BoardCrawler(new PlaywrightBrowserProvider(properties), properties);

        BrowserCrawlResult populated =
                crawler.crawl(
                        "https://www.kepco.co.kr/home/disclosure/addisclosure/boardSubList.do?boardMngNo=8&pBoardNo=2282");
        BrowserCrawlResult empty =
                crawler.crawl(
                        "https://www.kepco.co.kr/home/disclosure/addisclosure/boardSubList.do?boardMngNo=8&pBoardNo=2347");

        assertThat(populated.coverage().listItemsFound()).isGreaterThan(0);
        assertThat(populated.coverage().detailsFetched()).isGreaterThan(0);
        assertThat(populated.pages()).isNotEmpty();
        assertThat(populated.pages().get(0).url()).contains("/boardSubView.do");
        assertThat(empty.coverage().listPages()).isEqualTo(1);
        assertThat(empty.coverage().listItemsFound()).isZero();
        assertThat(empty.coverage().detailsFailed()).isZero();
        assertThat(empty.coverage().truncated()).isFalse();
    }

    @Test
    void electronicAnnouncementFirstPageDoesNotLoseSparseDetails() {
        assumeTrue(
                "true".equalsIgnoreCase(System.getenv("CLEVERCHAT_KEPCO_ALL_BOARDS")),
                "CLEVERCHAT_KEPCO_ALL_BOARDS is not enabled.");

        CrawlBrowserProperties properties = new CrawlBrowserProperties();
        properties.setEnabled(true);
        properties.setMaxPages(1);
        properties.setBrowsersPath(System.getenv("PLAYWRIGHT_BROWSERS_PATH"));
        BoardCrawler crawler =
                new BoardCrawler(new PlaywrightBrowserProvider(properties), properties);

        BrowserCrawlResult result =
                crawler.crawl("https://www.kepco.co.kr/home/about/invest/announce/boardList.do");

        assertThat(result.coverage().listItemsFound()).isGreaterThan(0);
        assertThat(result.coverage().detailsFetched())
                .isEqualTo(result.coverage().listItemsFound());
        assertThat(result.coverage().detailsFailed()).isZero();
    }

    @Test
    void internalRuleAjaxPaginationCrawlsAllPublishedItems() throws Exception {
        assumeTrue(
                "true".equalsIgnoreCase(System.getenv("CLEVERCHAT_KEPCO_ALL_BOARDS")),
                "CLEVERCHAT_KEPCO_ALL_BOARDS is not enabled.");

        CrawlBrowserProperties properties = new CrawlBrowserProperties();
        properties.setEnabled(true);
        properties.setBrowsersPath(System.getenv("PLAYWRIGHT_BROWSERS_PATH"));
        BoardCrawler crawler =
                new BoardCrawler(new PlaywrightBrowserProvider(properties), properties);

        BrowserCrawlResult result =
                crawler.crawl(
                        "https://www.kepco.co.kr/home/disclosure/regulations/internalrule/boardList.do");

        // Compare with the live site's published descending ordinals, not an old snapshot count.
        var live = org.jsoup.Jsoup.connect(
                "https://www.kepco.co.kr/home/disclosure/regulations/internalrule/boardList.do")
                .userAgent("CleverChatBot/1.0").timeout(30_000).get();
        var cards = live.select(".internalrule-content .card.board");
        assertThat(cards).isNotEmpty();
        int publishedItems = cards.select(".badge").stream()
                .map(org.jsoup.nodes.Element::text)
                .filter(text -> text.matches("No\\.\\d+"))
                .mapToInt(text -> Integer.parseInt(text.substring(3)))
                .max().orElseThrow();
        int publishedPages = (publishedItems + cards.size() - 1) / cards.size();
        System.out.printf("[LIVE] internal rules: %d items, %d pages%n", publishedItems, publishedPages);
        assertThat(result.coverage().listPages()).isEqualTo(publishedPages);
        assertThat(result.coverage().listItemsFound()).isEqualTo(publishedItems);
        assertThat(result.coverage().detailsFetched()).isEqualTo(publishedItems);
        assertThat(result.coverage().detailsFailed()).isZero();
        assertThat(result.coverage().truncated()).isFalse();
    }
}
