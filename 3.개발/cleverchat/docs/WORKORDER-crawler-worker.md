# 작업지시서 — 앱 내장형 Playwright 크롤러 (헤드리스 Chromium, 비동기·격리·제거 용이)

> 대상 실행자: Codex
> **이 문서는 이전 "별도 워커/별도 머신" 설계를 대체한다.** 최종 결정 = **기존 챗봇 앱에 내장**(별도 모듈/프로세스/머신 아님), 단 **격리된 패키지 + 기능 플래그로 "문제 시 제거"가 쉽게**.
> 성격: 크롤링은 **초기 세팅/주기적 데이터 적재용**(상시 기능 아님). JS/CSRF/POST 사이트(KEPCO 게시판)를 실제 브라우저로 수집해 공유 DB에 적재.

---

## 0. 한 줄 목표
정적 크롤러로는 안 보이는 JS/AJAX/CSRF/POST 게시판(KEPCO `boardList.do`/`boardView.do`)을 **앱에 내장한 헤드리스 Chromium(Playwright Java)** 으로 수집해 `tb_crawl_document`(형태소 토큰 포함)에 적재한다. 관리자는 기존 화면에서 관제(타깃/스케줄/실행/결과), 실제 수집은 **비동기 백그라운드**로 처리해 챗봇 응답을 막지 않는다.

---

## 1. 배경 / 결정 사항 (반드시 숙지)

### 1.1 검증된 사실
- 현재 정적 크롤러(`CrawlService.discoverUrls` = host BFS + Jsoup + `<a href>`)는 **KEPCO 게시판 목록/상세를 원천적으로 못 봄**. 목록=AJAX(POST+CSRF), 상세=POST 폼 `frm`(`boardNo`,`boardMngNo`,`mode`,`csrfToken`), GET 경로 없음, 서버 HTML `<tr>`=0.
- HtmlUnit POC 실패(`gsap is not defined`). → **실제 Chromium 엔진(Playwright)만 해결.**

### 1.2 아키텍처 결정 (이번 작업)
- **앱 내장**: 기존 Spring Boot 앱 안에 크롤러 서비스/컨트롤러로 탑재. 별도 모듈/jar/머신 아님.
- **비동기 백그라운드**: 관리자 "실행"은 즉시 응답하고 실제 크롤은 백그라운드 실행(챗봇 요청 경로 비차단). Playwright는 Chromium을 **별도 자식 프로세스**로 띄우므로 브라우저 메모리는 JVM 힙과 분리됨.
- **격리 + 제거 용이성(중요)**: 크롤러 코드는 **독립 패키지**(`domain/crawl/browser` 등)에 모으고, **기능 플래그**(`cleverchat.crawl.browser.enabled`, 기본 false)로 통째 on/off. 챗봇/검색/시나리오 런타임과 **하드 커플링 금지** → 컴플라이언스 문제 시 의존성·패키지·플래그만 제거하면 빠지게.
- **Chromium 런타임 외부화/핫스왑**: 브라우저 바이너리·네이티브 라이브러리는 **앱 jar 밖 설정 경로**에서 로드(아래 3.5). 폐쇄망/버전불일치 대비.

### 1.3 기존 자산(재사용)
- 스키마: `tb_crawl_target`, `tb_crawl_document`(**content_tokens**), `tb_crawl_run_log`. 적재 시 `common/search/KoreanMorphAnalyzer.tokenize(title+content)`로 형태소 컬럼 채움(검색 P2가 사용).
- 도메인: `domain/crawl`(`CrawlService`,`AdmCrawlController`,`CrawlMapper(.xml)`,`CrawlScheduler`) — 타깃 CRUD/스케줄/결과 조회 유지. 정적 fetch만 대체.
- 최신 마이그레이션 = V30(착수 시 재확인 후 +n).
- 참고: `CrawlFetcher` 인터페이스(`FetchedPage fetch(String url)`)가 깔끔한 확장점.

---

## 2. 목표 아키텍처
```
[기존 챗봇 앱 (단일 배포)]
 ├─ 챗봇/검색 (그대로, 영향 없음)
 ├─ 관제(관리자 화면): 타깃/스케줄/실행/결과·커버리지   ── DB ──┐
 └─ [격리 패키지] 브라우저 크롤러 (flag로 on/off)               │
     ├─ 비동기 실행기: tb_crawl_job 소비 → 백그라운드 수행      │
     ├─ Playwright(Java) → Chromium(자식 프로세스, 헤드리스)    │
     │     ├─ boardList 렌더 → 페이지네이션 → 상세 진입(POST 자동) │
     │     └─ 제목/본문 추출 → CrawlDocument(content_tokens) 적재 │
     └─ 커버리지 리포트(tb_crawl_coverage) 기록 ─────────────────┘
[Chromium 런타임] PLAYWRIGHT_BROWSERS_PATH / LD_LIBRARY_PATH = 앱 밖 설정 경로(운영자 관리, 읽기전용)
```

---

## 2.1 배포 토폴로지 / DMZ 네트워킹 (가정: 챗봇 WAS가 DMZ)
> **전제(확인 필요)**: 챗봇 WAS(JEUS)가 **DMZ**에 위치, **DB는 내부망**. 공개 홈페이지+챗봇이라 내·외부 모두 접근 → DMZ 앱 tier가 표준. *만약 "DMZ엔 웹(아파치/엔진엑스)만, WAS는 내부망"이면 크롤러만 DMZ 별도 컴포넌트로 분리해야 하므로 이 가정을 먼저 확인.*

```
외부 사용자 ─(외부FW)─▶ [DMZ: 챗봇 WAS + (내장)크롤러] ◀─(내부FW)─ 내부 사용자
                              │ 챗봇이 이미 가진 통제된 DB 연결(특정 호스트/포트)
                              ▼
                      [내부망: PostgreSQL]
```
- **데이터 반입 문제 없음**: 크롤러는 챗봇과 **같은 DMZ에서 동일한 DMZ→내부 DB 연결을 재사용**해 적재. 별도 망연계 불필요.
- 코드/런타임에 반영할 DMZ 항목:
  1. **아웃바운드 프록시**: 크롤러 Chromium이 기업 프록시 경유하도록 **프록시 설정값화**(Playwright `LaunchOptions.setProxy`). 대상 도메인(예: `www.kepco.co.kr`) 허용목록은 운영 방화벽/프록시 측 등록.
  2. **기업 CA 신뢰**: 프록시가 TLS 가로채면 **기업 루트 CA를 브라우저가 신뢰**하도록 설정값(인증서 경로) 지원. (무분별한 `ignoreHTTPSErrors`는 지양 — 가능하면 CA 신뢰.)
  3. **크롤 전용 최소권한 DB 계정**: 크롤러 쓰기는 **크롤 테이블만 INSERT/UPDATE 가능한 계정**으로(설정값). DMZ 침해 시 내부 DB 피해를 크롤 영역으로 한정. (배포/DB 권한 설정 사항이나 datasource 분리 가능하게.)
  4. **저비수 크롤 + 동시성 1~소수**: 챗봇이 사용자 대면(DMZ)이므로 스케줄은 야간/주기, 동시 실행 제한(설정값).
- 보안 주의: DMZ 호스트의 풀 Chromium은 공격표면 → 패치/모니터링 운영 필요. 크롤 데이터는 *데이터*로만 소비(코드 실행 아님).

## 3. 변경/신규 상세

### 3.1 의존성
- `com.microsoft.playwright:playwright` 추가. **운영 빌드에 항상 포함되더라도**, 코드 경로는 기능 플래그로 비활성 시 Playwright를 로드하지 않게(지연 초기화) 한다. (제거 시 의존성 1줄 + 패키지 삭제로 빠지게.)
- POC용 `org.htmlunit:htmlunit`(test) 및 `src/test/java/kr/co/cleverchat/poc/HtmlUnitKepcoPocTest.java`는 **제거**.

### 3.2 DB (앱 소유 마이그레이션 `V31__crawl_browser.sql`)
- `tb_crawl_job`(잡 가시화/영속): crawl_job_no PK, target_no FK, trigger_type('MANUAL'|'SCHEDULE'), status('PENDING'|'RUNNING'|'SUCCESS'|'FAILED'|'CANCELED'), requested_by, requested_at, started_at, finished_at, attempt_count, message. 인덱스(status,requested_at).
  - 같은 프로세스라 **크로스프로세스 락 불필요**. 단 다중 인스턴스 대비 클레임은 `UPDATE ... WHERE status='PENDING' ... RETURNING`(또는 `FOR UPDATE SKIP LOCKED`)로 원자화.
- `tb_crawl_coverage`(누락 검증, **필수**): coverage_no PK, target_no, run_log_no?, list_pages, list_items_found, details_fetched, details_failed, truncated_yn, frst_reg_dt.
- (선택) `tb_crawl_native_lib_registry`(거버넌스): 번들 버전/파일목록/체크섬/서명자/배포일시 — **파일을 서빙·리네임하지 않음. 메타데이터 추적용만.**
- 모든 변경 가산적. 데이터 삭제 금지.

### 3.3 크롤러 패키지 (격리, 제거 용이)
신규 패키지 예: `domain/crawl/browser/`
- `PlaywrightBrowserProvider`: WebClient/Browser 수명주기. 헤드리스, `setEnv`/시스템 프로퍼티로 `PLAYWRIGHT_BROWSERS_PATH`·`LD_LIBRARY_PATH` 적용. 기능 플래그 off면 미초기화.
- `BoardCrawler`(KEPCO 패턴): 타깃 boardList → `navigate` → `waitForLoadState(NETWORKIDLE)`/행 셀렉터 대기 → **페이지네이션 순회**(`page` 파라미터 또는 다음버튼) → 각 상세 진입(행 클릭/폼 제출, CSRF/POST 자동) → 제목/본문 추출 → `CrawlDocument` 생성(content≤200KB, content_tokens, url/합성키, sha256 해시, status). 중복(content_hash)·실패 처리. 셀렉터·페이지상한은 **설정값**(하드코딩 최소화, 사이트 변동 대비).
- `CrawlJobRunner`(비동기): `tb_crawl_job` PENDING 소비(인앱 스케줄러/`@Async`/bounded executor). 동시성 제한(설정). 실행→문서/런로그/커버리지 적재→잡 상태 갱신. 무한 RUNNING 회수(타임아웃/attempt 상한).

### 3.4 관제(기존 앱) 연동
- **"즉시 실행"**: 기존 동기 run 대신 `tb_crawl_job`에 MANUAL PENDING INSERT 후 즉시 응답(비동기). 화면은 잡/런/커버리지 상태 폴링·표시.
- **스케줄러**(`CrawlScheduler`): due 타깃 산정 유지하되 직접 크롤 금지 → SCHEDULE 잡 enqueue(해당 target에 PENDING/RUNNING 있으면 skip). **스케줄 소유권 = CMS/관리자**(타깃별 `schedule_*`/`next_run_at`). 워커 자체 스케줄 없음.
- **정적 fetch 비활성화**: `HttpCrawlFetcher`/`discoverUrls` 기반 동기 수집을 운영 실행 경로에서 제거/차단. 타깃·문서·로그·스케줄 **관리/조회는 유지**.

### 3.5 Chromium 런타임 (외부화 / 핫스왑 / 보안)
- 브라우저 바이너리: `PLAYWRIGHT_BROWSERS_PATH`(설정값) 우선. **인터넷 가능 시 자동 다운로드, 폐쇄망이면 동봉 경로 사용**(둘 다 지원, 기본 모드는 설정).
- 네이티브 라이브러리(Linux): 크롤러가 Chromium 띄울 때 **고정 설정 경로**를 `LD_LIBRARY_PATH`로 사용 → 운영자가 그 폴더에 **올바른 SONAME 파일(번들 아카이브)** 을 보안 전송으로 넣으면 **다음 크롤부터 자동 적용(앱 재시작 불필요)**. *시스템 1회 install-deps 또는 자가 번들 — 배포 시 택일, 코드는 동일.*
- **네이티브 라이브러리 업로드 기능(관리자 화면) — 안전 설계로 구현**: 핵심 원칙은 **"업로드 ≠ 즉시 적용/실행"**. 절대 업로드 파일이 곧바로 활성 LD_LIBRARY_PATH로 들어가게 하지 말 것(그게 RCE).
  1. **업로드 → 스테이징 디렉터리**(활성 경로 아님). 업로드만으론 아무 효과 없음.
  2. **검증**: 번들 **체크섬/서명** 확인 + **허용 SONAME 화이트리스트**만 통과 + 경로조작(zip slip)·용량·확장자 검증.
  3. **별도 "적용(activate)" 액션**(슈퍼관리자): 스테이징 → 활성 경로로 **원자적 교체**(심링크/포인터 갱신). 이 단계에서만 반영, **다음 크롤부터** 유효(새 Chromium 프로세스).
  4. 접근통제: **슈퍼관리자(최고권한) 한정** + **IP 화이트리스트(별도 작업지시서) 뒤** + 업로드/적용 모두 **`@Audited`** 기록.
  5. 활성 lib 디렉터리는 **앱 프로세스 읽기전용**, 교체는 적용 액션이 통제. 파일 실명(SONAME) 유지(리네임/암호화 금지 — 링커가 못 찾음).
  6. (권장) 레지스트리 테이블: 번들 버전/체크섬/올린이/적용일시 거버넌스.
  - 보안 정책이 더 빡센 환경이면 이 업로드 기능을 비활성(설정 플래그)하고 운영자 SCP 전송으로 대체 가능하게.
- **DMZ 네트워킹(설정값)**: Chromium 런치에 **프록시**(`LaunchOptions.setProxy`) 및 **기업 CA 신뢰**(인증서 경로) 설정 지원(2.1 참조). 미설정 시 직접 연결. 프록시/CA/허용목록은 운영 환경값.

### 3.6 형태소 적재
- 문서 저장 시 `content_tokens = KoreanMorphAnalyzer.tokenize(title + " " + content)`. → 검색(P2)에 즉시 반영, 별도 백필 불필요.

---

## 4. KEPCO 실측 contract (구현 근거)
- 타깃: 메뉴=게시판 1개 = `.../boardList.do`(여러 개). 예: `.../disclosure/regulations/revision/revision/boardList.do`, `.../customer/library/electricity-statistics/kepco-stats/boardList.do`.
- 목록: 로드 후 **AJAX 렌더**(정적 HTML 행 없음), jQuery 3.6. 페이지네이션 후보 파라미터 `page`/`listCount`/`pageTp`.
- 상세: `boardView.do`로 **POST 폼 `frm`**(`boardNo`,`boardMngNo`,`mode`,`gMenuNo`,`csrfToken`...). Chromium으로 행 클릭 시 CSRF/POST **자동 처리**.

---

## 5. 동작 요구 (수용 기준)
1. 기능 플래그 off(기본) → 크롤러 코드/Playwright 미초기화, 앱은 기존과 동일.
2. on + 관리자 "실행" → MANUAL 잡 enqueue, 즉시 응답, 백그라운드 수행(챗봇 비차단).
3. 스케줄 도래 → SCHEDULE 잡 enqueue(중복 skip).
4. 워커가 헤드리스 Chromium으로 KEPCO 보드 **목록 전 페이지 + 모든 상세** 수집, content_tokens 적재.
5. 커버리지(tb_crawl_coverage)로 **수집 페이지/상세/실패/누락(truncated)** 확인 가능.
6. Chromium 런타임 경로 미설정/누락 시 명확한 오류 + 잡 FAILED(앱 기동은 정상).
7. (네이티브 lib 업로드 기능) 업로드는 **스테이징에만** 저장(즉시 적용 안 됨) → 검증 통과 후 **슈퍼관리자 "적용"** 시에만 활성 경로 반영 → 다음 크롤부터 유효. 업로드/적용 모두 감사 기록.
8. 기존 챗봇/검색/시나리오/세션(앞선 b안) 및 기존 테스트 **불변**.

---

## 6. 테스트
- 잡 enqueue(관리자 실행→동기 크롤 아님), 스케줄 enqueue/중복 skip, 동시성 클레임 원자성(단위/매퍼 mock).
- 문서 적재 시 content_tokens 채워짐, 실패 시 잡 FAILED.
- 기능 플래그 off 시 크롤러 빈 동작(Playwright 미로드).
- KEPCO 실수집은 **integration 태그**(라이브 네트워크, 기본 제외).
- 기존 `CrawlServiceTest` 등은 enqueue 구조로 갱신하되 회귀 0.

---

## 7. 빌드 / 검증 / 프로비저닝
- 이 머신 빌드 JVM CDS 손상 → **모든 maven 호출 전 `$env:MAVEN_OPTS="-Xshare:off"`**.
- 포맷: `.\mvnw.cmd -q spotless:apply`.
- 단위테스트: `.\mvnw.cmd "-Dspotless.check.skip=true" test` — 사전 존재 `AccessibilityTemplateTest`(3건, CSS) 외 신규 실패 0.
- 실수집 검증: 크롤 머신에서 브라우저 런타임 준비(자동 다운로드 또는 동봉 + libs install/bundle) 후 KEPCO 타깃 1개 end-to-end → 커버리지로 목록/상세 확인.

---

## 8. 완료 기준 (DoD)
- [ ] V31: tb_crawl_job + tb_crawl_coverage(+선택 registry).
- [ ] 격리 패키지 + 기능 플래그(기본 off)로 크롤러 on/off, 제거 용이.
- [ ] 관리자 즉시 실행=비동기 enqueue, 스케줄=enqueue.
- [ ] 헤드리스 Chromium으로 KEPCO 목록 전 페이지+상세 수집, content_tokens 적재.
- [ ] 커버리지로 누락 검증 가능.
- [ ] Chromium 런타임 경로 설정값화(PLAYWRIGHT_BROWSERS_PATH/LD_LIBRARY_PATH).
- [ ] 네이티브 lib 업로드 기능: 스테이징 업로드 + 검증(체크섬/SONAME 화이트리스트/zip slip) + 슈퍼관리자 별도 "적용" + 감사. (즉시 활성화 금지)
- [ ] DMZ: 아웃바운드 프록시·기업 CA 설정값 지원, 크롤 동시성/스케줄 제한.
- [ ] 기존 기능/테스트 불변, spotless 적용, 신규 실패 0.

---

## 9. 하지 말 것 / 주의
- 네이티브 lib 업로드는 **즉시 활성화 금지**(업로드=스테이징만, 검증 후 슈퍼관리자 "적용" 별도 단계). 업로드 파일을 곧바로 활성 LD_LIBRARY_PATH로 넣지 말 것(RCE). 활성 경로는 앱 읽기전용.
- 네이티브 파일 실명(SONAME) 유지 — 리네임/암호화 금지(링커가 못 찾음).
- 크롤러를 챗봇/검색/시나리오 런타임에 **하드 커플링 금지**(제거 용이성 유지). 기능 플래그 off가 기본.
- 관리자 "즉시 실행"을 동기 블로킹으로 만들지 말 것(비동기 필수).
- 기존 챗봇/검색/시나리오/세션(b안) 로직 건드리지 말 것.
- 마이그레이션 가산적만, 데이터 파괴/삭제 금지.
- 커밋/푸시는 사용자 지시가 있을 때만.
