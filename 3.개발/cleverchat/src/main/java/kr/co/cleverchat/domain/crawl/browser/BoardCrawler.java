package kr.co.cleverchat.domain.crawl.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BoardCrawler {
    private static final int MAX_CONTENT_LENGTH = 200_000;
    private static final int NAVIGATION_ATTEMPTS = 5;
    private static final Pattern DETAIL_ARGUMENTS =
            Pattern.compile(
                    "fn_Detail\\(\\s*['\\\"]?([^,'\\\")]+)['\\\"]?\\s*,\\s*['\\\"]?([^,'\\\")]+)");
    private static final Pattern SUB_DETAIL_ARGUMENTS =
            Pattern.compile(
                    "fn_SubDetail\\(\\s*['\\\"]?([^,'\\\")]+)['\\\"]?\\s*,\\s*['\\\"]?([^,'\\\")]+)['\\\"]?\\s*,\\s*['\\\"]?([^,'\\\")]+)");
    private static final Logger log = LoggerFactory.getLogger(BoardCrawler.class);

    private final PlaywrightBrowserProvider browserProvider;
    private final CrawlBrowserProperties properties;

    public BoardCrawler(
            PlaywrightBrowserProvider browserProvider, CrawlBrowserProperties properties) {
        this.browserProvider = browserProvider;
        this.properties = properties;
    }

    public BrowserCrawlResult crawl(String url) {
        Browser browser = browserProvider.browser();
        try (BrowserContext context = browser.newContext()) {
            context.setDefaultTimeout(properties.getNavigationTimeoutMs());
            Page page = context.newPage();
            Page detailPage = context.newPage();
            List<BrowserCrawlPage> documents = new ArrayList<>();
            int listPages = 0;
            int listItemsFound = 0;
            int detailsFetched = 0;
            int detailsFailed = 0;
            boolean truncated = false;
            Set<String> visitedListPages = new HashSet<>();

            Response response = navigate(page, url);
            if (!waitForRows(page)) {
                if (isKnownEmptyBoard(page)) {
                    return new BrowserCrawlResult(
                            List.of(), new BrowserCrawlCoverage(1, 0, 0, 0, false));
                }
                throw new IllegalStateException("Board page has no recognized rows: " + url);
            }

            while (true) {
                if (limitReached(properties.getMaxPages(), listPages)) {
                    truncated = true;
                    log.warn(
                            "Browser crawl stopped at the configured page limit: listUrl={}, maxPages={}",
                            url,
                            properties.getMaxPages());
                    break;
                }
                String expectedRows = visibleRowSignature(page);
                if (!visitedListPages.add(expectedRows)) {
                    truncated = true;
                    log.warn(
                            "Browser crawl stopped because a list page repeated: listUrl={}, listPages={}",
                            url,
                            listPages);
                    break;
                }
                listPages++;
                int pageNo = listPages - 1;
                RowSelection rowSelection = resolveRows(page);
                Locator rows = rowSelection.rows();
                int rowCount = rows.count();
                listItemsFound += countVisible(rows);
                for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                    if (limitReached(properties.getMaxDetails(), documents.size())) {
                        truncated = true;
                        break;
                    }
                    boolean listPageNavigated = false;
                    BrowserCrawlPage rowFallback = null;
                    String attemptedDetailUrl = null;
                    try {
                        Locator rowLocator = rows.nth(rowIndex);
                        if (!rowLocator.isVisible()) {
                            continue;
                        }
                        ElementHandle row = rowLocator.elementHandle();
                        if (row == null) {
                            detailsFailed++;
                            continue;
                        }
                        rowFallback =
                                extractInlineDocument(
                                        rowLocator,
                                        page.url(),
                                        response == null ? null : response.status());
                        String canonicalDetailUrl =
                                canonicalDetailUrl(url, rowLocator.getAttribute("href"));
                        attemptedDetailUrl = canonicalDetailUrl;
                        Page contentPage;
                        Integer detailStatus;
                        if (rowSelection.inline()) {
                            if (!rowFallback.content().isBlank()) {
                                documents.add(rowFallback);
                                detailsFetched++;
                            } else {
                                detailsFailed++;
                            }
                            continue;
                        } else if (canonicalDetailUrl != null) {
                            Response detailResponse = navigate(detailPage, canonicalDetailUrl);
                            waitForDetailContent(detailPage);
                            contentPage = detailPage;
                            detailStatus = detailResponse == null ? null : detailResponse.status();
                        } else {
                            listPageNavigated = true;
                            row.click();
                            waitForDetailContent(page);
                            contentPage = page;
                            detailStatus = response == null ? null : response.status();
                        }
                        BrowserCrawlPage document =
                                extractDocument(
                                        contentPage,
                                        detailStatus,
                                        rowIndex + 1,
                                        canonicalDetailUrl);
                        if (!document.content().isBlank()) {
                            documents.add(document);
                            detailsFetched++;
                        } else if (!rowFallback.content().isBlank()
                                && isSparseElectronicAnnouncement(canonicalDetailUrl)) {
                            documents.add(withUrl(rowFallback, canonicalDetailUrl));
                            detailsFetched++;
                            log.info(
                                    "Sparse electronic announcement stored from list metadata: listUrl={}, page={}, row={}, detailUrl={}",
                                    url,
                                    pageNo + 1,
                                    rowIndex + 1,
                                    canonicalDetailUrl);
                        } else if (!rowFallback.content().isBlank()) {
                            documents.add(withUrl(rowFallback, canonicalDetailUrl));
                            detailsFetched++;
                            detailsFailed++;
                            log.warn(
                                    "Browser detail content was blank; stored list fallback: listUrl={}, page={}, row={}, detailUrl={}",
                                    url,
                                    pageNo + 1,
                                    rowIndex + 1,
                                    canonicalDetailUrl);
                        } else {
                            detailsFailed++;
                            log.warn(
                                    "Browser detail content was blank: listUrl={}, page={}, row={}, detailUrl={}",
                                    url,
                                    pageNo + 1,
                                    rowIndex + 1,
                                    page.url());
                        }
                        if (listPageNavigated) {
                            returnToList(page, url, pageNo, expectedRows);
                            rowSelection = resolveRows(page);
                            rows = rowSelection.rows();
                        }
                    } catch (RuntimeException e) {
                        detailsFailed++;
                        if (rowFallback != null && !rowFallback.content().isBlank()) {
                            documents.add(withUrl(rowFallback, attemptedDetailUrl));
                            detailsFetched++;
                        }
                        log.warn(
                                "Browser detail crawl failed: listUrl={}, page={}, row={}, currentUrl={}",
                                url,
                                pageNo + 1,
                                rowIndex + 1,
                                listPageNavigated ? page.url() : detailPage.url(),
                                e);
                        if (listPageNavigated) {
                            returnToList(page, url, pageNo, expectedRows);
                            rowSelection = resolveRows(page);
                            rows = rowSelection.rows();
                        }
                    }
                }
                if (truncated || !clickNext(page)) {
                    break;
                }
            }

            return new BrowserCrawlResult(
                    documents,
                    new BrowserCrawlCoverage(
                            listPages, listItemsFound, detailsFetched, detailsFailed, truncated));
        }
    }

    private boolean limitReached(int configuredLimit, int currentCount) {
        return configuredLimit > 0 && currentCount >= configuredLimit;
    }

    private int countVisible(Locator locators) {
        int visible = 0;
        int count = locators.count();
        for (int index = 0; index < count; index++) {
            try {
                if (locators.nth(index).isVisible()) {
                    visible++;
                }
            } catch (RuntimeException ignored) {
                // The responsive list can be redrawn while its visibility is inspected.
            }
        }
        return visible;
    }

    private boolean waitForRows(Page page) {
        try {
            page.waitForSelector(
                    properties.getRowSelector() + ", " + properties.getInlineRowSelector(),
                    new Page.WaitForSelectorOptions().setTimeout(shortTimeoutMs()));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isKnownEmptyBoard(Page page) {
        Locator main = page.locator("main");
        if (main.count() == 0) {
            return false;
        }
        String text = main.first().innerText();
        return text != null
                && (text.contains("등록된 게시글이 없습니다")
                        || text.contains("등록된 게시물이 없습니다")
                        || text.contains("검색 결과가 없습니다"));
    }

    private RowSelection resolveRows(Page page) {
        Locator detailRows = page.locator(properties.getRowSelector());
        if (countVisible(detailRows) > 0) {
            return new RowSelection(detailRows, false);
        }
        return new RowSelection(page.locator(properties.getInlineRowSelector()), true);
    }

    private void waitForDetailContent(Page page) {
        page.waitForSelector(
                properties.getContentSelector(),
                new Page.WaitForSelectorOptions().setTimeout(shortTimeoutMs()));
    }

    private Response navigate(Page page, String url) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {
            try {
                Response response =
                        page.navigate(
                                url,
                                new Page.NavigateOptions()
                                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                                        .setTimeout(properties.getNavigationTimeoutMs()));
                waitForReady(page);
                return response;
            } catch (RuntimeException e) {
                lastFailure = e;
                if (!isRetryableNavigationFailure(e) || attempt == NAVIGATION_ATTEMPTS) {
                    throw e;
                }
                long delayMs = attempt * 2_000L;
                log.warn(
                        "Transient browser navigation failure; retrying: url={}, attempt={}/{}, delayMs={}, reason={}",
                        url,
                        attempt,
                        NAVIGATION_ATTEMPTS,
                        delayMs,
                        e.getMessage());
                page.waitForTimeout(delayMs);
            }
        }
        throw lastFailure;
    }

    static boolean isRetryableNavigationFailure(RuntimeException failure) {
        String message = failure == null ? null : failure.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toUpperCase(java.util.Locale.ROOT);
        return normalized.contains("ERR_CONNECTION_RESET")
                || normalized.contains("ERR_CONNECTION_CLOSED")
                || normalized.contains("ERR_CONNECTION_ABORTED")
                || normalized.contains("ERR_NETWORK_CHANGED")
                || normalized.contains("ERR_HTTP2_PROTOCOL_ERROR")
                || normalized.contains("ERR_TIMED_OUT")
                || normalized.contains("NAVIGATION TIMEOUT");
    }

    private void waitForReady(Page page) {
        try {
            page.waitForLoadState(
                    LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(shortTimeoutMs()));
        } catch (RuntimeException ignored) {
            // Some KEPCO pages keep background requests open; selector waits do the real gating.
        }
    }

    private void returnToList(Page page, String url, int pageNo, String expectedRows) {
        if (expectedRows.equals(visibleRowSignature(page))) {
            return;
        }
        if (goBackToList(page) && expectedRows.equals(visibleRowSignature(page))) {
            return;
        }

        navigate(page, url);
        waitForRows(page);
        for (int i = 0; i < pageNo; i++) {
            if (!clickNext(page)) {
                break;
            }
        }
    }

    private boolean goBackToList(Page page) {
        try {
            page.goBack(
                    new Page.GoBackOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                            .setTimeout(shortTimeoutMs()));
            waitForReady(page);
            waitForRows(page);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private double shortTimeoutMs() {
        return Math.min(properties.getNavigationTimeoutMs(), 10_000);
    }

    private BrowserCrawlPage extractDocument(
            Page page, Integer httpStatus, int rowIndex, String canonicalDetailUrl) {
        String title = firstTitleText(page, properties.getTitleSelector());
        String content = firstContentText(page, properties.getContentSelector(), title);
        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH);
        }
        String finalUrl = canonicalDetailUrl == null ? page.url() : canonicalDetailUrl;
        if (finalUrl == null || finalUrl.isBlank()) {
            finalUrl = "browser-crawl:row-" + rowIndex;
        }
        return new BrowserCrawlPage(finalUrl, blankToNull(title), content, httpStatus);
    }

    private BrowserCrawlPage extractInlineDocument(
            Locator row, String listUrl, Integer httpStatus) {
        String title =
                oneLineText(
                        (String)
                                row.evaluate(
                                        "el => { const node = el.querySelector('.title,.tit,strong,h3,h4');"
                                                + " return node ? (node.textContent || '') : ''; }"));
        String content =
                multilineText(
                        (String)
                                row.evaluate(
                                        "el => {"
                                                + " const values = Array.from(el.querySelectorAll("
                                                + "'input[name=title],input[name=conts],textarea[name=conts]'))"
                                                + ".map(node => node.value || node.textContent || '').filter(Boolean);"
                                                + " return [el.innerText || '', ...values].join('\\n');"
                                                + " }"));
        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH);
        }
        String href =
                (String)
                        row.evaluate(
                                "el => { const link = Array.from(el.querySelectorAll('a[href]'))"
                                        + ".find(a => { const value = a.getAttribute('href') || '';"
                                        + " return value && !value.toLowerCase().startsWith('javascript:')"
                                        + " && value !== '#'; });"
                                        + " return link ? link.getAttribute('href') : ''; }");
        String finalUrl = resolveContentUrl(listUrl, href);
        if (finalUrl == null) {
            String base = listUrl == null ? "browser-crawl:inline" : listUrl.split("#", 2)[0];
            finalUrl = base + "#crawl-item-" + sha256(title + "\n" + content).substring(0, 16);
        }
        return new BrowserCrawlPage(finalUrl, blankToNull(title), content, httpStatus);
    }

    private BrowserCrawlPage withUrl(BrowserCrawlPage source, String preferredUrl) {
        String finalUrl =
                preferredUrl == null || preferredUrl.isBlank() ? source.url() : preferredUrl;
        return new BrowserCrawlPage(
                finalUrl, source.title(), source.content(), source.httpStatus());
    }

    private boolean isSparseElectronicAnnouncement(String detailUrl) {
        return detailUrl != null && detailUrl.contains("/invest/announce/boardView.do");
    }

    private String resolveContentUrl(String listUrl, String href) {
        if (listUrl == null || href == null || href.isBlank()) {
            return null;
        }
        try {
            URI resolved = URI.create(listUrl).resolve(href.trim()).normalize();
            String scheme = resolved.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                return resolved.toString();
            }
        } catch (IllegalArgumentException ignored) {
            // A stable synthetic URL is generated below.
        }
        return null;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    static String canonicalDetailUrl(String listUrl, String href) {
        if (listUrl == null || href == null) {
            return null;
        }
        Matcher subMatcher = SUB_DETAIL_ARGUMENTS.matcher(href);
        if (subMatcher.find()) {
            int queryIndex = listUrl.indexOf('?');
            String base = queryIndex < 0 ? listUrl : listUrl.substring(0, queryIndex);
            base = base.replace("boardSubList.do", "boardSubView.do");
            return base
                    + "?boardMngNo="
                    + URLEncoder.encode(subMatcher.group(1).trim(), StandardCharsets.UTF_8)
                    + "&boardNo="
                    + URLEncoder.encode(subMatcher.group(2).trim(), StandardCharsets.UTF_8)
                    + "&pBoardNo="
                    + URLEncoder.encode(subMatcher.group(3).trim(), StandardCharsets.UTF_8);
        }
        Matcher matcher = DETAIL_ARGUMENTS.matcher(href);
        if (!matcher.find()) {
            return null;
        }
        int queryIndex = listUrl.indexOf('?');
        String base = queryIndex < 0 ? listUrl : listUrl.substring(0, queryIndex);
        if (base.endsWith("/headquarters.do")) {
            base =
                    base.substring(0, base.length() - "/headquarters.do".length())
                            + "/headquarters/boardView.do";
        } else {
            base = base.replace("boardList.do", "boardView.do");
        }
        return base
                + "?boardMngNo="
                + URLEncoder.encode(matcher.group(1).trim(), StandardCharsets.UTF_8)
                + "&boardNo="
                + URLEncoder.encode(matcher.group(2).trim(), StandardCharsets.UTF_8);
    }

    private String firstTitleText(Page page, String selector) {
        for (String candidate : selectorCandidates(selector)) {
            Locator locator = page.locator(candidate);
            int count = locator.count();
            for (int i = 0; i < count; i++) {
                String text = locator.nth(i).innerText();
                if (text != null && !text.isBlank()) {
                    return oneLineText(text);
                }
            }
        }
        return "";
    }

    private String firstContentText(Page page, String selector, String title) {
        List<String> extracted = new ArrayList<>();
        for (String candidate : selectorCandidates(selector)) {
            Locator locator = page.locator(candidate);
            int count = locator.count();
            for (int i = 0; i < count; i++) {
                String text =
                        (String)
                                locator.nth(i)
                                        .evaluate(
                                                "el => {"
                                                        + "const clone = el.cloneNode(true);"
                                                        + "clone.querySelectorAll('script,style,noscript,header,footer,nav,"
                                                        + ".pagination,.paging,[class*=pagination],[class*=paging],"
                                                        + "[class*=satisfaction],[class*=satis],"
                                                        + "[class*=charge],[class*=manager],[class*=department],"
                                                        + "[class*=logout],[class*=session],[id*=logout],[id*=session],"
                                                        + ".breadcrumb,.sub-title-block,.tab-list-box').forEach(e => e.remove());"
                                                        + "clone.querySelectorAll('br').forEach(e => e.replaceWith('\\n'));"
                                                        + "clone.querySelectorAll('p,div,li,tr,td,th,h1,h2,h3,h4,h5,h6,section,article,blockquote,pre').forEach(e => e.append('\\n'));"
                                                        + "return clone.textContent || '';"
                                                        + "}");
                text = multilineText(text);
                if (!text.isBlank()) {
                    extracted.add(text);
                }
            }
        }
        String best = bestContentText(extracted, title);
        return best.isBlank() ? sparseDetailText(page) : best;
    }

    private String sparseDetailText(Page page) {
        Locator details = page.locator("main .board-detail");
        int count = details.count();
        for (int index = 0; index < count; index++) {
            String text = details.nth(index).innerText();
            if (text == null || !text.contains("등록된 파일이 없습니다")) {
                continue;
            }
            StringBuilder normalized = new StringBuilder();
            for (String rawLine : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
                String line = rawLine.replaceAll("[\\t\\x0B\\f ]+", " ").trim();
                if (line.isBlank()) {
                    continue;
                }
                if (!normalized.isEmpty()) {
                    normalized.append('\n');
                }
                normalized.append(line);
            }
            return normalized.toString();
        }
        return "";
    }

    static String bestContentText(List<String> candidates, String title) {
        String normalizedTitle = oneLineText(title == null ? "" : title);
        List<String> useful =
                candidates.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .filter(value -> !oneLineText(value).equals(normalizedTitle))
                        .toList();
        return useful.stream()
                .filter(value -> value.length() >= 80)
                .findFirst()
                .orElseGet(
                        () ->
                                useful.stream()
                                        .max(java.util.Comparator.comparingInt(String::length))
                                        .orElse(""));
    }

    private List<String> selectorCandidates(String selector) {
        List<String> candidates = new ArrayList<>();
        for (String candidate : selector.split(",")) {
            String trimmed = candidate.trim();
            if (!trimmed.isBlank()) {
                candidates.add(trimmed);
            }
        }
        return candidates.isEmpty() ? List.of(selector) : candidates;
    }

    private static String oneLineText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String multilineText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        text =
                text.replace('\u00A0', ' ')
                        .replaceAll("\\?\\s*\\?\\s*-\\s*", "\n- ")
                        .replaceAll("\\?\\s*(?=(\\d+[.)]|[□<\\-]))", "\n")
                        .replaceAll("\\?\\s*$", "");
        StringBuilder normalized = new StringBuilder();
        for (String rawLine : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = rawLine.replaceAll("[\\t\\x0B\\f ]+", " ").trim();
            if (isNoiseLine(line)) {
                continue;
            }
            if (!normalized.isEmpty()) {
                normalized.append('\n');
            }
            normalized.append(line);
        }
        String result = normalized.toString().replaceAll("\\n{3,}", "\n\n").trim();
        if (!result.contains("\n")) {
            result =
                    result.replaceAll("\\s+(?=\\d+[.)]\\s)", "\n")
                            .replaceAll("\\s+(?=[□<])", "\n")
                            .replaceAll("\\s+(?=-\\s)", "\n")
                            .replaceAll("\\n{3,}", "\n\n")
                            .trim();
        }
        return result;
    }

    private boolean isNoiseLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String compact = line.replaceAll("\\s+", "");
        if (compact.matches(".*(만족하셨습니까|매우만족|만족도|페이지에서제공하는정보).*")) {
            return true;
        }
        if (compact.matches(".*(담당부서|담당자|연락처|최종업데이트|페이지번호입력|다음페이지|이전페이지).*")) {
            return true;
        }
        if (compact.matches(".*(자동로그아웃|로그아웃됩니다|로그인연장|세션).*")) {
            return true;
        }
        if (compact.matches(".*(등록일|작성일|조회수|첨부파일|다운로드|미리보기|점자로보기|Loading).*")) {
            return true;
        }
        return compact.matches("(홈|목록|이전|다음|이전글|다음글|맨위|TOP|검색|닫기|열기)");
    }

    private boolean clickNext(Page page) {
        String currentRows = visibleRowSignature(page);
        Boolean pageParameterResult = navigateNextByPageParameter(page, currentRows);
        if (pageParameterResult != null) {
            return pageParameterResult;
        }
        Locator next = page.locator(properties.getNextSelector());
        int candidateCount = next.count();
        for (int index = 0; index < candidateCount; index++) {
            Locator candidate = next.nth(index);
            try {
                if (!candidate.isVisible() || candidate.isDisabled()) {
                    continue;
                }
                candidate.click(
                        new Locator.ClickOptions().setTimeout(shortTimeoutMs()).setForce(true));
                waitForReady(page);
                page.waitForTimeout(300);
                long deadline =
                        System.nanoTime() + (long) (Math.min(shortTimeoutMs(), 8_000) * 1_000_000);
                String candidateRows = null;
                int stableSamples = 0;
                while (System.nanoTime() < deadline) {
                    String nextRows = visibleRowSignature(page);
                    if (!nextRows.isBlank()
                            && !nextRows.equals(currentRows)
                            && countVisible(resolveRows(page).rows()) > 0) {
                        if (nextRows.equals(candidateRows)) {
                            stableSamples++;
                        } else {
                            candidateRows = nextRows;
                            stableSamples = 0;
                        }
                        if (stableSamples >= 2) {
                            return true;
                        }
                    } else {
                        candidateRows = null;
                        stableSamples = 0;
                    }
                    page.waitForTimeout(100);
                }
            } catch (RuntimeException ignored) {
                // Responsive markup can contain hidden or stale duplicate paging controls.
                // Try another visible candidate and preserve documents already collected when
                // none can be used.
            }
        }
        return false;
    }

    /**
     * Uses KEPCO's stable {@code ?page=N} contract when its numbered pagination input exists. A
     * non-null result means that numbered pagination was present, so callers must not fall back to
     * the AJAX next button while the document may still be replacing its execution context.
     */
    private Boolean navigateNextByPageParameter(Page page, String currentRows) {
        Locator input = page.locator("#paginationNum[max]");
        if (input.count() == 0 || !input.first().isVisible()) {
            return null;
        }
        int currentPage;
        int lastPage;
        try {
            currentPage = Integer.parseInt(input.first().inputValue().trim());
            lastPage = Integer.parseInt(input.first().getAttribute("max").trim());
        } catch (RuntimeException e) {
            return false;
        }
        if (currentPage >= lastPage) {
            return false;
        }

        String nextUrl = withPageParameter(page.url(), currentPage + 1);
        try {
            navigate(page, nextUrl);
        } catch (RuntimeException e) {
            // Chromium can report that the old execution context was destroyed even though the
            // requested GET navigation is already completing. Validate the replacement document
            // instead of querying an AJAX control in the transient old context.
            waitForReady(page);
        }
        if (!waitForRows(page)) {
            return false;
        }
        String nextRows = visibleRowSignature(page);
        return !nextRows.isBlank() && !nextRows.equals(currentRows);
    }

    static String withPageParameter(String url, int pageNo) {
        URI uri = URI.create(url);
        List<String> parameters = new ArrayList<>();
        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            for (String parameter : uri.getRawQuery().split("&")) {
                if (!parameter.equals("page") && !parameter.startsWith("page=")) {
                    parameters.add(parameter);
                }
            }
        }
        parameters.add("page=" + pageNo);
        try {
            return new URI(
                            uri.getScheme(),
                            uri.getRawAuthority(),
                            uri.getRawPath(),
                            String.join("&", parameters),
                            null)
                    .toASCIIString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid pagination URL: " + url, e);
        }
    }

    private String visibleRowSignature(Page page) {
        Locator rows = resolveRows(page).rows();
        StringBuilder signature = new StringBuilder();
        int count = rows.count();
        for (int index = 0; index < count; index++) {
            Locator row = rows.nth(index);
            try {
                if (row.isVisible()) {
                    signature
                            .append(
                                    row.evaluate(
                                            "el => { const attrs = Array.from(el.querySelectorAll('a[href]'))"
                                                    + ".map(a => a.getAttribute('href')).join('|');"
                                                    + " return (el.innerText || '') + '|' + attrs; }"))
                            .append('\n');
                }
            } catch (RuntimeException ignored) {
                // A page transition can replace the list while its signature is sampled.
            }
        }
        return signature.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RowSelection(Locator rows, boolean inline) {}
}
