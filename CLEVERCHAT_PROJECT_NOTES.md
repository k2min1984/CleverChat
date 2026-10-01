# CleverChat project notes

Date: 2026-06-10

## Project shape

- Spring Boot + MyBatis + Flyway + PostgreSQL.
- No JPA. No Spring Security. Admin auth, CSRF, session, and role checks are local interceptor/session/aspect code.
- Main app path: `3.개발/cleverchat`.
- Domain modules: `auth`, `admin`, `scenario`, `chatbot`, `search`, `crawl`, `ops`.

## Crawling path

- Admin views:
  - `/admin/crawl-targets`
  - `/admin/crawl-targets/new`
  - `/admin/crawl-targets/{id}`
  - `/admin/crawl-targets/{id}/edit`
  - legacy `/admin/crawl-documents` and `/admin/crawl-runs` currently redirect back to crawl target pages.
- Admin APIs:
  - `/admin/api/crawl-targets`
  - `/admin/api/crawl-targets/{id}/run`
  - `/admin/api/crawl-targets/schedule/preview`
  - `/admin/api/crawl-documents`
  - `/admin/api/crawl-runs`
  - `/admin/api/crawl-runs/failures`
  - `/admin/api/crawl-runs/{id}/review`
  - `/admin/api/crawl-runs/expired`
- Service:
  - `CrawlService` validates URL, checks robots, fetches HTML, parses text with Jsoup, stores `tb_crawl_document`, and writes `tb_crawl_run_log`.
  - `CrawlScheduler` runs due targets every 60 seconds and retention cleanup daily at 03:45.
  - `CrawlUrlPolicy` blocks localhost/private/link-local/multicast/userinfo URLs unless exact host allowlist is configured.
  - `CrawlRobotsService` caches robots.txt per origin for 10 minutes.
  - `HttpCrawlFetcher` currently fetches one URL, does not follow redirects, and only accepts HTML responses.

## Data flow to chatbot

- Crawled documents are searched through `SearchMapper.searchScenarios`.
- Search results include `CRAWL_DOCUMENT` items from `tb_crawl_document` where `status = 'SUCCESS'`.
- Chat free text first tries scenario matching.
- If there is no scenario match, `ChatRuntimeService` calls `SearchService.search(..., "CHAT_FALLBACK", 3, ...)`.
- If fallback results exist, `AiAnswerSuggestionService` may generate a response when enabled.
- AI answer suggestion is scaffold-only by default: `cleverchat.ai.answer-suggestion.enabled=false`.

## Important design notes

- M5 crawling operation policy is in `2.설계/06.운영메모/M5_크롤링_운영메모.md`.
- Search fallback policy is in `2.설계/03.API설계서/M4_검색API.md`.
- AI/RAG scaffold policy is in `2.설계/04.아키텍처/M9_RAG_AI답변추천_Scaffold.md`.
- Latest DB naming uses `tb_crawl_target`, `tb_crawl_document`, `tb_crawl_run_log` and `*_no` columns after V20 naming alignment.

## Current working tree cautions

- There are many existing uncommitted changes, especially admin/crawl/chat/search/ops templates and CSS/JS.
- Do not overwrite or revert these changes without checking them first.
- `CrawlService` only has a small added `target(Long id)` method in the current diff.
- `AdmCrawlController` has a larger pending refactor from separate list pages into target-focused create/detail/edit/run flows.

## Likely next work for real crawling

- Decide whether "proper crawling" means single page fetch, same-host link discovery, sitemap crawl, or configured URL queue depth.
- Extend fetcher/service to support redirects safely if required, revalidating final URLs against SSRF policy.
- Add per-target crawl limits: max pages, max depth, max bytes, timeout, same-host constraint, and duplicate URL/content handling.
- Store page-level documents for every discovered URL, not only one target URL.
- Keep robots checks per discovered URL path.
- Add tests for redirect handling, final URL blocking, multi-page discovery, duplicate documents, and crawl limits.

## Verification note

- Tried to run crawl-related tests on 2026-06-10, but Maven could not resolve `spring-boot-starter-parent:3.3.5` because network access to Maven Central was blocked in the current environment.
