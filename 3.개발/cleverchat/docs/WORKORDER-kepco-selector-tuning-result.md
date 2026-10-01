# KEPCO selector tuning result

## Confirmed selectors

- `rowSelector`: `main .board-list-tbody .board-title a[href^='javascript:fn_Detail'], main .card.board a.title[href^='javascript:fn_Detail']`
- `titleSelector`: `main .board-detail .sub-component-title, main .board-detail .detail-top h4, main h1, main h2`
- `contentSelector`: `main .board-detail .detail-content, main .board-detail, article, main, body`
- `nextSelector`: `.pagination .arrow-box.next a[onclick], a[rel=next], a.next`

Detail entry is an anchor click on `href="javascript:fn_Detail(...)"`. KEPCO fills hidden form values such as `boardMngNo`, `boardNo`, and `csrfToken`, then submits to `boardView.do`.

## Before / After counts

### Revision notice board

- URL: `https://www.kepco.co.kr/home/disclosure/regulations/revision/revision/boardList.do`
- Before: `tbody tr = 0`, `table tr = 0`
- After: confirmed `rowSelector = 10`
- Detail body: first detail entered `boardView.do`; `main .board-detail .detail-content` produced non-blank body text.

### Electricity statistics board

- URL: `https://www.kepco.co.kr/home/customer/library/electricity-statistics/kepco-stats/boardList.do`
- Before: table row selectors did not match the list.
- After: card board selector matched the visible list entries.
- Cross-check: `BoardCrawler` extracted detail body text with the same shared selectors.

## Manual root crawl verification

The admin target URL is the KEPCO root, `https://www.kepco.co.kr/`, not a direct board URL. I verified the manual queue path with a real job after routing root/menu pages through static discovery and discovered `boardList.do` pages through `BoardCrawler`.

- Test job: `tb_crawl_job.crawl_job_no = 8`
- Started: `2026-06-18 13:55:56 KST`
- Finished: `2026-06-18 14:14:05 KST`
- Final status: `SUCCESS`
- Final message: `Hybrid crawl visited 120 URL(s), 23 board list(s): static 4 success, 93 duplicate; board 2 success, 0 duplicate; 21 failed.`
- New documents during job #8: `439`
- Coverage rows during job #8: `2`
- Coverage totals: `list_pages = 40`, `list_items_found = 440`, `details_fetched = 435`, `details_failed = 5`

This confirms the crawler is not doing only one page or one detail. It crawled discovered menu/static pages, entered board lists, paged through board lists, and extracted hundreds of detail bodies. Some discovered `boardList.do` URLs are not the same KEPCO board layout and are counted as failed board pages in the final message.

## Changes

- `src/main/java/kr/co/cleverchat/domain/crawl/browser/CrawlBrowserProperties.java`
  - KEPCO row/title/content/next selectors.
  - Default browser `maxDetails` reduced to `250` to prevent oversized boards from holding one manual job too long.
- `src/main/resources/application.yml`
  - Same selector defaults and `max-details` default.
- `src/main/java/kr/co/cleverchat/domain/crawl/browser/BoardCrawler.java`
  - Waits for the row selector instead of sleeping.
  - Uses `DOMContentLOADED` instead of `NETWORKIDLE`.
  - Restores the list with `goBack(DOMCONTENTLOADED)` first, falling back to list re-navigation if needed.
  - Adds bounded next-page click timeout.
- `src/main/java/kr/co/cleverchat/domain/crawl/browser/CrawlJobRunner.java`
  - Direct `boardList.do` targets use `BoardCrawler`.
  - Root/menu targets use static URL discovery, then route discovered `boardList.do` URLs through `BoardCrawler`.
  - Hybrid jobs finish `SUCCESS` when at least one static or board crawl result was stored, while still reporting failed discovered board URLs in the message.
- `src/main/java/kr/co/cleverchat/domain/crawl/service/CrawlService.java`
  - Exposes job-safe static discovery and single-page static crawl helpers for the hybrid runner.
- `src/test/java/kr/co/cleverchat/domain/crawl/browser/CrawlJobRunnerTest.java`
  - Covers static root routing, discovered board routing, and partial-failure hybrid success.

## Follow-up: KEPCO source link UX

### Problem

KEPCO detail pages are opened by site JavaScript that submits hidden form values and a CSRF token to `boardView.do`. A saved `boardView.do` URL cannot be opened directly with a normal browser GET request; KEPCO shows the alert `부정한 접근입니다 정상적인 방법으로 이용해주세요`.

Changing the chatbot source link to `boardList.do` avoided the alert, but it created another usability problem: if the matched post is on a lower page or near the bottom of the list, the user lands at the top of the board and cannot easily find the exact post.

### Applied approach

The chatbot now opens an internal cached crawl-document view for crawl search results:

- Primary chatbot source link: `/chat/crawl-documents/{crawlDocumentNo}`
- Internal view source: `tb_crawl_document`
- External KEPCO link remains secondary and is normalized from `boardView.do` to `boardList.do`.

This does not bypass KEPCO CSRF. It shows the exact crawled title/body already stored by CleverChat, then lets the user optionally open the original board list.

### Readability cleanup

The internal crawl-document page uses display-only content cleanup. The raw crawled document remains unchanged in the DB.

Filtered display noise includes short navigation/header lines such as:

- `로그인`, `로그아웃`, `회원가입`, `마이페이지`
- `사이트맵`, `본문 바로가기`, `주메뉴 바로가기`
- `화면크기`, `글자크기`, `검색어 입력`

If a specific KEPCO board still shows too much menu/footer text, the next better fix is to narrow that board's body selector in the browser crawler rather than deleting stored raw content.

### Search result button labels

When multiple crawl documents are returned, buttons no longer use only the title. Crawl-document choices are labeled with title plus a short snippet:

- Before: `공지사항`
- After: `공지사항 - 계약업무 처리기준 개정 안내...`

This makes repeated KEPCO titles easier to distinguish in the chatbot result buttons.

### Follow-up changed files

- `src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java`
  - Crawl result links now point to `/chat/crawl-documents/{crawlDocumentNo}`.
  - Crawl search option labels use `title - snippet` style labels.
  - `boardView.do` remains normalized to `boardList.do` only for external fallback/source links.
- `src/main/java/kr/co/cleverchat/domain/chatbot/controller/ChatPageController.java`
  - Adds `GET /chat/crawl-documents/{id}`.
  - Adds display-only crawl content cleanup.
- `src/main/resources/templates/chat/crawlDocument.html`
  - Adds public cached crawl-document detail page.
- `src/main/java/kr/co/cleverchat/domain/crawl/mapper/CrawlMapper.java`
  - Adds `findDocumentById`.
- `src/main/resources/mapper/crawl/CrawlMapper.xml`
  - Adds successful crawl-document lookup by id.
- `src/main/java/kr/co/cleverchat/domain/crawl/service/CrawlService.java`
  - Adds read-only document lookup service.
- `src/test/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeServiceTest.java`
  - Verifies internal crawl-document links and improved crawl option labels.
- `src/test/java/kr/co/cleverchat/domain/chatbot/controller/ChatPageControllerTest.java`
  - Verifies cached document page, external source normalization, and display cleanup.

### Follow-up verification

- Date: `2026-06-18 15:00 KST`
- `spotless:apply`: passed.
- Targeted tests:
  - Command: `.\mvnw.cmd "-Dspotless.check.skip=true" "-DforkCount=0" "-Dtest=ChatRuntimeServiceTest,ChatPageControllerTest" test`
  - Result: `Tests run: 39, Failures: 0, Errors: 0, Skipped: 0`
- Runtime smoke check:
  - Server run mode: `spring-boot:run`
  - `http://localhost:8080/chat/crawl-documents/77`: `200`
- Full unit regression from the real repo path:
  - Result: `Tests run: 370, Failures: 3, Errors: 0, Skipped: 0`
  - Remaining failures are the known existing `AccessibilityTemplateTest` CSS checks documented in the work order constraints.

## Verification

- `spotless:apply`: passed.
- `KepcoBoardCrawlerIntegrationTest`: passed with real KEPCO URL.
- `CrawlJobRunnerTest`: passed, 6 tests.
- Full unit regression from the real repo path:
  - Command: `.\mvnw.cmd "-Dspotless.check.skip=true" "-Dtest=!AccessibilityTemplateTest" test`
  - Result: `Tests run: 360, Failures: 0, Errors: 0, Skipped: 0`

## Remaining items

- No code blocker remains for the KEPCO selector tuning work.
- The final job message still reports unsupported discovered KEPCO `boardList.do` layouts. Those are different board templates and can be tuned separately if they need first-class browser detail extraction.
