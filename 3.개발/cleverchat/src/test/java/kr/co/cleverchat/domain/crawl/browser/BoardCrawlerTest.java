package kr.co.cleverchat.domain.crawl.browser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BoardCrawlerTest {

    @Test
    void crawlLimitsAreUnlimitedByDefault() {
        CrawlBrowserProperties properties = new CrawlBrowserProperties();

        assertThat(properties.getMaxPages()).isZero();
        assertThat(properties.getMaxDetails()).isZero();
        assertThat(properties.getJobTimeoutMinutes()).isEqualTo(180);
        assertThat(properties.getBoardRetryAttempts()).isEqualTo(3);
        assertThat(properties.getBoardRetryDelayMs()).isEqualTo(10000);
    }

    @Test
    void negativeCrawlLimitsAreNormalizedToUnlimited() {
        CrawlBrowserProperties properties = new CrawlBrowserProperties();
        properties.setMaxPages(-1);
        properties.setMaxDetails(-1);

        assertThat(properties.getMaxPages()).isZero();
        assertThat(properties.getMaxDetails()).isZero();
    }

    @Test
    void bestContentTextSkipsTitleOnlyCandidateAndUsesPageMetadata() {
        String title = "2026년도판 한국전력통계(제95호)";
        String metadata =
                "등록일 2026.07.01\n조회수 123\n첨부파일 한국전력통계 제95호.pdf\n"
                        + "담당부서 전력통계부에서 제공하는 최신 통계 자료입니다.";

        assertThat(BoardCrawler.bestContentText(List.of(title, metadata), title))
                .isEqualTo(metadata);
    }

    @Test
    void bestContentTextPrefersFirstSubstantiveSpecificCandidate() {
        String specific = "본문 ".repeat(30);
        String broadContainer = "메뉴와 주변 화면 ".repeat(100);

        assertThat(BoardCrawler.bestContentText(List.of(specific, broadContainer), "제목"))
                .isEqualTo(specific);
    }

    @Test
    void canonicalDetailUrlPreservesBoardIdentifiersFromJavascriptLink() {
        assertThat(
                        BoardCrawler.canonicalDetailUrl(
                                "https://www.kepco.co.kr/home/about/invest/announce/boardList.do?page=2",
                                "javascript:fn_Detail('7','161')"))
                .isEqualTo(
                        "https://www.kepco.co.kr/home/about/invest/announce/boardView.do?boardMngNo=7&boardNo=161");
    }

    @Test
    void canonicalDetailUrlSupportsNestedKepcoBoardDetails() {
        assertThat(
                        BoardCrawler.canonicalDetailUrl(
                                "https://www.kepco.co.kr/home/disclosure/addisclosure/boardSubList.do?boardMngNo=8&pBoardNo=2282",
                                "javascript:fn_SubDetail('8','2348','2282');"))
                .isEqualTo(
                        "https://www.kepco.co.kr/home/disclosure/addisclosure/boardSubView.do?boardMngNo=8&boardNo=2348&pBoardNo=2282");
    }

    @Test
    void canonicalDetailUrlSupportsRegionalHeadquartersBoardDetails() {
        assertThat(
                        BoardCrawler.canonicalDetailUrl(
                                "https://www.kepco.co.kr/home/about/locations/southseoul/headquarters.do?branchNo=11",
                                "javascript:fn_Detail('40','12');"))
                .isEqualTo(
                        "https://www.kepco.co.kr/home/about/locations/southseoul/headquarters/boardView.do?boardMngNo=40&boardNo=12");
    }

    @Test
    void pageParameterIsReplacedWithoutLosingExistingBoardParameters() {
        assertThat(
                        BoardCrawler.withPageParameter(
                                "https://www.kepco.co.kr/home/boardList.do?boardMngNo=8&page=1&pBoardNo=2282",
                                2))
                .isEqualTo(
                        "https://www.kepco.co.kr/home/boardList.do?boardMngNo=8&pBoardNo=2282&page=2");
    }

    @Test
    void transientNetworkNavigationFailuresAreRetryable() {
        assertThat(
                        BoardCrawler.isRetryableNavigationFailure(
                                new RuntimeException("net::ERR_CONNECTION_RESET")))
                .isTrue();
        assertThat(
                        BoardCrawler.isRetryableNavigationFailure(
                                new RuntimeException("Navigation timeout of 30000 ms exceeded")))
                .isTrue();
    }

    @Test
    void invalidNavigationFailuresAreNotRetried() {
        assertThat(
                        BoardCrawler.isRetryableNavigationFailure(
                                new RuntimeException("Cannot navigate to invalid URL")))
                .isFalse();
    }
}
