# WORKORDER-crawler-worker 결과보고서

## 변경/신규 파일
- `pom.xml`: Playwright 의존성 유지, HtmlUnit 의존성/소스 없음 확인
- `src/main/resources/db/migration/V31__crawl_browser.sql`: `tb_crawl_job`, `tb_crawl_coverage` 추가
- `src/main/resources/db/migration/V33__crawl_native_lib_registry.sql`: 네이티브 lib 번들 레지스트리 추가
- `src/main/resources/application.yml`: `cleverchat.crawl.browser.*` 기능 플래그, 런타임 경로, 프록시/CA, 동시성, 네이티브 lib 업로드 설정 추가
- `src/main/java/kr/co/cleverchat/domain/crawl/browser/*`: Playwright provider, KEPCO 보드 크롤러, 잡 러너, 네이티브 lib 스테이징/검증/적용 서비스와 컨트롤러 추가
- `src/main/resources/mapper/crawl/CrawlMapper.xml`, `src/main/java/kr/co/cleverchat/domain/crawl/mapper/CrawlMapper.java`: 잡/커버리지 enqueue, claim, 조회 추가
- `src/main/resources/mapper/crawl/CrawlNativeLibMapper.xml`, `CrawlNativeLibMapper.java`: 네이티브 lib 레지스트리 매퍼 추가
- `src/main/java/kr/co/cleverchat/domain/crawl/service/CrawlService.java`, `CrawlScheduler.java`: 수동/스케줄 실행을 동기 fetch가 아닌 `tb_crawl_job` enqueue로 전환
- `src/main/java/kr/co/cleverchat/domain/crawl/controller/*`, `src/main/resources/templates/admmgr/crawl/*`: 잡/커버리지 관제 및 네이티브 lib 관리 화면/API 추가
- `src/test/java/kr/co/cleverchat/domain/crawl/browser/*`, `src/test/java/kr/co/cleverchat/domain/crawl/service/CrawlServiceTest.java`: enqueue, 잡 실행, 네이티브 lib 안전장치 테스트 추가/갱신

## 주요 설계 결정
- 착수 시 최신 마이그레이션은 `V32__admin_ip_whitelist.sql`로 확인했다. 기존 `V31__crawl_browser.sql`은 이미 존재하므로 네이티브 lib 레지스트리는 가산 마이그레이션 `V33`으로 추가했다.
- 브라우저 크롤러는 `domain/crawl/browser`에 격리했고 `cleverchat.crawl.browser.enabled=false`가 기본값이다. flag off 상태에서는 Playwright provider/BoardCrawler가 생성되지 않는다.
- 관리자 수동 실행과 스케줄 실행은 `tb_crawl_job`에 `PENDING` 잡을 enqueue하고 즉시 응답한다. 같은 target의 `PENDING`/`RUNNING` 잡은 중복 enqueue하지 않는다.
- 잡 클레임은 `FOR UPDATE SKIP LOCKED` 기반으로 원자화했고, 실행 결과는 `tb_crawl_run_log`, `tb_crawl_coverage`, `tb_crawl_document.content_tokens`에 적재한다.
- 네이티브 lib 업로드는 기본 비활성이다. 활성화해도 zip 번들은 스테이징 디렉터리에만 저장되고, SHA-256 체크섬, 신뢰 서명 목록, SONAME 화이트리스트, zip slip/용량/확장자 검증을 통과해야 `VERIFIED`로 등록된다.
- 네이티브 lib 활성 경로 반영은 별도 `activate` 액션에서만 수행한다. 업로드 파일은 즉시 `LD_LIBRARY_PATH`로 들어가지 않으며, 적용 후 다음 Chromium 프로세스부터 유효하다.
- 업로드/적용 서비스 메서드에는 `@RequireRole("ADMIN")`와 `@Audited`를 적용해 기존 최고권한/감사 인프라를 재사용했다.
- 기존 챗봇/검색/시나리오/세션 로직, 검색 품질 로직, 기존 크롤 필터링 로직은 변경하지 않았다.

## 작업지시서 대비 이탈
- 설계 이탈 없음.
- 정적 fetch 구현은 파일에서 물리 삭제하지 않고 private 메서드로 남아 있다. 운영 진입점(`run`, `runScheduled`)은 잡 enqueue로 전환되어 정적 fetch 경로를 타지 않는다.

## 전체 테스트 결과
- 포맷: `$env:MAVEN_OPTS="-Xshare:off"; .\mvnw.cmd -q spotless:apply` 성공
- 전체 테스트: `$env:MAVEN_OPTS="-Xshare:off"; .\mvnw.cmd "-Dspotless.check.skip=true" test`
  - Tests run: 365
  - Failures: 3
  - Errors: 0
  - Skipped: 0
  - 실패 3건은 사전 존재로 안내된 `AccessibilityTemplateTest` CSS 관련 실패:
    - `chatAndAdminStylesExposeVisibleKeyboardFocus`
    - `chatPageProvidesLiveRegionsAndNamedInputAreas`
    - `staticColorPairsMeetWcagContrastBaseline`
  - 신규 크롤러/네이티브 lib 관련 테스트 실패: 0 (`CrawlJobRunnerTest`, `CrawlNativeLibServiceTest`, `CrawlServiceTest` 통과)

## 미완 항목
- KEPCO 실수집은 `integration` 태그 테스트로 분리되어 기본 빌드에서 제외된다. 이 환경에서는 Playwright/Chromium 런타임 및 라이브 네트워크 검증을 실행하지 않았다.
