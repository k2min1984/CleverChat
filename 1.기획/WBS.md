# CLEVERCHAT 작업 분해 구조 (WBS)

### M3 Runtime Implementation Notes (2026-05-27)
- [x] Session expiration handling: expired sessions are marked `EXPIRED` and runtime APIs return `410 SESSION_EXPIRED`.
- [x] PII blocking: free-text input detects email, phone, and resident registration number patterns before saving user messages.
- [x] IP/User-Agent hashing: chat sessions persist SHA-256 hashes only.
- [x] Rate limit: per anonymous/session memory mode records `RATE_LIMITED` failures, and optional Redis store is available for multi-instance request limiting.
- [x] Retention jobs: `@Scheduled` expiration and 90-day chat session cleanup implemented.
- [x] Admin failure queue: `/admin/chat/failures`, `GET /admin/api/chat/failures`, and review API implemented.
- [x] Admin feedback list: `/admin/chat/feedback` and `GET /admin/api/chat/feedback` implemented.
- [x] Admin recommendation operations: `/admin/chat/recommendations` and recommendation list/create/update/delete APIs implemented.
> 출처: `9.참고/기능요청.md`
> 작성일: 2026-05-07
> 목표: 시나리오 챗봇 시스템 — Spring Boot + PostgreSQL + Thymeleaf

---

## 0. 마일스톤 개요

| 단계 | 명칭 | 주요 산출물 | 예상 비중 |
|------|------|-------------|-----------|
| M0 | 프로젝트 셋업 | 골격 코드, CI 베이스, 환경 분리 | 90% |
| M1 | 인증·관리자 기반 | 로그인, 권한, 감사 로그 | 90% |
| M2 | 시나리오 도메인 | 시나리오 CRUD, 노드 구조 | 95% |
| M3 | 챗봇 런타임 | 시나리오 진행 엔진, 대화 이력 | 95% |
| M4 | 검색 | 키워드/유사어/우선순위 | 95% |
| M5 | 크롤링 | URL 등록, 자동 수집, 파싱 | 95% |
| M6 | 운영·모니터링 | 로그, 대시보드, 백업 | 90% |
| M7 | 보안·접근성 강화 | OWASP, WA 준수, HTTPS | 90% |

---

## 1. M0 — 프로젝트 셋업

### 1.1 환경 구성
- [x] JDK / Maven 버전 확정 — JDK 17, Maven Wrapper, Spring Boot 3.3.5 기준
- [x] Spring Boot 프로젝트 생성 (Web, Thymeleaf, Validation, Actuator, MyBatis)
- [x] PostgreSQL 로컬/통합 테스트 인스턴스 준비 — Testcontainers PostgreSQL 16 기준 구성 완료. Windows Docker named pipe 이슈 시 CI/Linux Docker `-Pit test` 통과를 대체 게이트로 인정
- [x] `application.yml` 프로파일 설정 정리 — dev/stage/prod placeholder와 운영 전제 문서화
- [x] 프로파일별 시크릿 외부화 정책 정의 — prod 자동 시딩 차단, OS env/외부 properties 기준 문서화

### 1.2 형상 관리
- [x] `git init` 및 `.gitignore` (Java, Maven, IDE, OS, secrets)
- [x] 브랜치 전략 기본값 합의 — trunk 기반 + 기능 단위 PR 전제
- [x] 커밋 컨벤션 합의 — `feat`/`test`/`docs`/`chore` 단위 분리 기준

### 1.3 품질 베이스라인
- [x] 린터/포매터 (Checkstyle / Spotless) — Spotless check-only 기본형과 Java baseline 정리 완료, 향후 대량 `spotless:apply`는 별도 PR 원칙
- [x] 테스트 프레임워크 (JUnit5 + Mockito + Testcontainers)
- [x] CI 파이프라인 초안 (build → test → static analysis) — GitHub Actions 초안 작성, `clean test`/`spotless:check`/`git diff --check` 기준 반영, runner/secrets/branch protection 적용은 후속

---

## 2. M1 — 인증 / 권한 / 관리자 기반

### 2.1 사용자·권한 모델
- [x] `User`, `Role` 모델 및 MyBatis 매퍼 구현
- [x] 사용자/운영자/관리자 권한 분리 베이스 설계

### 2.2 인증
- [x] HandlerInterceptor + 세션 VO 기반 폼 로그인 (관리자)
- [x] 비밀번호 해시 (BCrypt) 저장
- [x] 세션 만료/동시 로그인 정책
- [x] 로그인 성공/실패 이력 저장
- [x] 로그인 실패 5회/30분 잠금 기본 정책
- [x] 초기 관리자 시딩 Flyway 전환 완료: `V4_1__seed_default_admin.sql` + `default-admin-seed-enabled` placeholder 토글(dev/stage `true`, prod 기본 `false`) 기준

### 2.3 인가
- [x] URL 기반 접근 제어 (HandlerInterceptor 매핑)
- [x] 서비스 계층 권한 검증 헬퍼 — `RoleGuard`/`@RequireRole` AOP로 관리자 mutation 서비스 진입점 2차 권한 확인
- [x] 권한별 메뉴 노출 제어 (세션 권한/모델 속성) 베이스

### 2.4 감사
- [x] 관리자 작업 감사 로그 인터셉터/AOP 베이스
- [x] 데이터 수정 이력 — `audit_log`와 `AuditTrailRecorder`/AOP 기반 1차 구현

---

## 3. M2 — 시나리오 도메인

### 3.1 데이터 모델
- [x] `Scenario`, `ScenarioVersion`, `Node`, `NodeOption(Edge)`, `Category`, `Keyword`, `Synonym`
- [x] 노드 타입: 질문 / 답변 / 분기 / 종료
- [x] 활성화/비활성화 플래그, 버전 보관 정책
- [x] M2 ERD/인덱스 초안 작성 (`2.설계/02.DB설계서/M2_시나리오ERD.md`, `인덱스설계.md`)

### 3.2 관리 화면
- [x] 시나리오 목록/검색
- [x] 시나리오 등록/수정/삭제
- [x] 노드 트리 편집 UI — P0 graph 편집 화면 기준 구현, 시각 canvas editor는 후속 고도화
- [x] 키워드/유사어 관리
  - [x] 키워드 replace 저장 성공 시 매칭 캐시 무효화 훅 적용 (`ScenarioMatchingCacheInvalidator.onScenarioChanged(scenarioId)`, 2026-05-21)
- [x] 미리보기 — 버전 상태별 문구와 빈 graph 상태 처리 구현
- [x] M2 API 초안 작성 (`2.설계/03.API설계서/M2_시나리오API.md`)
- [x] 1.A-⑤ 8소스 CSS `url()`/`@font-face` 종속 자산 실측 반영 (`3.개발/cleverchat/docs/Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md` 부록 B-1)
- [x] 1.A-⑥ CleverChat scenario 화면 4건 정적 참조 실측 매트릭스 반영 (`3.개발/cleverchat/docs/Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md` §4)
- [x] 1.A-⑨ 최종 복사 대상 확정 (도입 확정 2 / 보류 4 / 추가 확인 1 / 복사 금지 4그룹 11건) (2026-05-14, `3.개발/cleverchat/docs/Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md` §5) — 후속 절차: `3.개발/cleverchat/docs/Phase1A_D_OFL_Acquire_Inter_NotoSansKR_작업지시.md` §7
  - 후속 절차(연계): `3.개발/cleverchat/docs/Phase1A_E_Static_Asset_Blocker_B3_B4_B5_작업지시.md` §3.2 B4 최종 배치 경로 1안(`static/asset/admmgr/style2/{css,font}/`) 채택 완료 결과 동시 참조 (2026-05-19)
- [x] 1.A-E B4 최종 배치 경로 1안 채택 완료: `static/asset/admmgr/style2/{css,font}/` (Phase1A_E §3.2 권고안 1안). Phase1A_C §5.2·§5.4의 B4 경로 표기 정합화 완료(2026-05-19). B5 Inter/NotoSansKR 복사 금지 해제는 본 경로 기준을 따른다. (`3.개발/cleverchat/docs/Phase1A_E_Static_Asset_Blocker_B3_B4_B5_작업지시.md` §3.2/§3.3)
  - 역링크: `3.개발/cleverchat/docs/Phase1A_E_Static_Asset_Blocker_B3_B4_B5_작업지시.md` §3.2 점검란 '최종 배치 경로 1안 채택 기록 [x] 완료'
  - 역링크: `3.개발/cleverchat/docs/Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md` §5.2(`sub.css` 보류) `static/asset/admmgr/style2/{css,font}/`, §5.4(폰트 복사 금지) `static/asset/admmgr/style2/font/` 표기 정합화 완료

### 3.3 구현 지시
- [x] M2 작업지시서 작성 (`3.개발/작업지시서/M2_시나리오_작업지시서.md`)

### 3.4 테스트 분리 정책
- [x] M2 테스트 분리 정책 문서화 (`4.테스트/01.테스트계획서/M2_테스트계획서.md` 갱신, `3.개발/작업지시서/M2_테스트분리정책_작업지시서.md` 신설)

---

## 4. M3 — 챗봇 런타임

### 4.1 사용자 화면
- [x] 챗봇 전용 페이지 구현 (반응형) — `/chat` 전용 화면과 `asset/chat/chat.js`, `asset/chat/chat.css` 기본 연결 완료(2026-05-27)
- [x] 버튼 선택형 진행 구현 — `POST /chat/api/sessions/{sessionId}/select-option` 기반 기본 진행 구현 완료
- [x] 자유 텍스트 입력 → 검색 fallback 구현 — 기존 매칭 서비스 기반 free-text/no-match fallback 기본 흐름 구현 완료
- [x] 추천 질문 노출 구현 — `GET /chat/api/recommendations`와 `/chat` 추천 질문 영역 연결 완료
- [x] M3 사용자 화면/접근성 설계 완료 (`2.설계/01.화면설계서/M3_사용자화면목록.md`, `M3_접근성체크리스트.md`)

### 4.2 진행 엔진
- [x] 세션별 진행 상태 관리 구현 (DB 테이블) — `chat_session` 기반 시작/조회/진행 상태 저장 구현 완료
- [x] 노드 전이 로직, 분기 평가 구현 — 옵션 선택 기반 단순 전이 구현 완료, 조건식은 M3 제외 정책 유지
- [x] 이전 대화 이력 조회 구현 — `/chat/history`, `GET /chat/api/history`, `GET /chat/api/sessions/{sessionId}/history` 구현
- [x] M3 ERD/API/상태전이/운영 설계 완료 (`M3_챗봇런타임ERD.md`, `M3_챗봇런타임API.md`, `M3_상태전이도.md`, `M3_운영메모.md`)

### 4.3 피드백
- [x] 답변 만족도 평가 구현 (thumbs + 코멘트) — BOT 메시지 단위 `POST /chat/api/messages/{messageId}/feedback` 구현
- [x] 답변 실패 시 관리자 확인 플래그 구현 — 실패 큐 reviewed/reviewed_by/reviewed_at/review_comment 및 관리자 review API 구현
- [x] `chat_failure.detail` 표준 키 사전 설계 완료 (`2.설계/02.DB설계서/M3_챗봇런타임ERD.md`)

### 4.4 보안·운영·테스트
- [x] `anonymous_id` 쿠키, CSRF, 본인 세션 검증 구현 — `/chat` 토큰 발급, runtime API owner 검증, anonymous cookie 흐름 구현
- [x] PII 차단/마스킹, IP/UA 해시, rate limit 구현 — M7.3 PII 정책, 해시 저장, memory 기본 + optional Redis rate limit 구현
- [x] 만료 배치와 90일 파기 배치 구현 — 세션 만료 처리와 90일 보존 정리 service/scheduler 구현
- [x] M3 단위/통합/화면/보안 회귀 테스트 구현 — runtime/admin/security 회귀 테스트 추가
- [x] 관리자 세션/실패 큐/피드백/추천 질문 화면 구현 — 세션 목록/상세, 실패 큐, 피드백, 추천 질문 운영 화면과 API 구현

### 4.5 시나리오 게시 UX 개선

> 분석 근거: `3.개발/cleverchat/docs/Scenario_Publish_UX_분석.md`,
>            `3.개발/cleverchat/docs/Scenario_Activate_Button_조건분석.md`,
>            `3.개발/cleverchat/docs/Scenario_Registration_Workflow_재검토.md` (2026-05-22)
> 대상 화면: `src/main/resources/templates/admmgr/scenario/scenarioView.html`,
>            컨트롤러: `AdmScenarioController`, `AdmScenarioApiController`
> 정책 전제: `1.기획/결정사항.md` §7의 `ACTIVE` 시나리오 개정 정책은 B안(게시 성공 시 즉시 `active_version_id`를 새 `PUBLISHED`로 교체)을 따른다.

#### P0 (ACTIVE 무중단 개정 선행 필수)

> 작업 순서: 그래프 편집 → 새 초안 중복 방지 → 게시 실패 Flash → 미리보기 분기

- [x] P0-01 그래프 편집 화면: DRAFT 버전 전용 그래프 편집/저장 화면 신설(또는 상세 화면 연결), `GET /admin/api/scenarios/versions/{versionId}/graph` JSON API 신규 추가, PUBLISHED/ARCHIVED 편집 진입은 409 `STATE_CONFLICT`로 차단. (구현/검증: 2026-05-27, `.\mvnw.cmd clean test` 69건 통과)
- [x] 그래프 편집 화면: `scenarioView.html`에서 DRAFT 편집 링크를 제공하고 `scenarioGraphEdit.html`/`ADM.ScenarioGraphEdit.js`에서 노드/엣지 편집·저장 UI와 백엔드 저장 API 연결
- [x] 새 초안 중복 방지: 동일 시나리오의 다중 `DRAFT` 생성을 금지하고 기존 `DRAFT` 편집/미리보기로 안내
- [x] 게시 실패 Flash: form POST 게시 실패 시 상세 화면으로 redirect하고 `RedirectAttributes` flash로 원인 안내, 화면/JSON 응답 경계 분리
- [x] 미리보기 분기: `DRAFT`는 초안 미리보기, `PUBLISHED`/`ARCHIVED`는 게시본 미리보기로 문구 분리

#### 단기 (관리자 화면 즉시 안내 강화)
- [x] DRAFT 게시 버튼 비활성화: 게시 가능 여부 DTO의 `publishable=false`인 경우 `th:disabled` 적용
- [x] 비활성 버튼 tooltip: 게시 가능 여부 DTO의 `reason`을 `title` + `aria-describedby`로 병행
- [x] 게시 버튼 인접 help text: 게시 가능 여부 DTO의 `reason`으로 선행조건 안내
- [x] 실패 메시지 표준화: `저장된 그래프가 없습니다.` 외 사용자 행동 중심 문구 매핑

#### 중기 (응답 구조 분리·재사용)
- [x] form POST 실패 시 상세 화면 redirect + Flash 메시지 흐름 도입
- [x] 화면/API 응답 구조 분리 (Controller redirect / ApiController JSON)
- [x] 게시 가능 여부 DTO 도입 (`hasGraph`, `hasStartNode`, `publishable`, `reason`)
- [x] 관리자 form POST UX 공통화 (`activate`/`deactivate`/`new draft`/`publish`) (범위: P0 외 activate/deactivate/new draft 공통화)

> 추적성(역링크): 본 절 항목의 근거/배경은 `3.개발/cleverchat/docs/Scenario_Publish_UX_분석.md`(게시 실패 UX), `3.개발/cleverchat/docs/Scenario_Activate_Button_조건분석.md`(활성 버튼 노출 조건), `3.개발/cleverchat/docs/Scenario_Registration_Workflow_재검토.md`(P0 순서 출처, 2026-05-22) 참조. 위 분석 문서에서 본 절(`1.기획/WBS.md` §4.5)을 역참조해 양방향 동기화.

---

## 5. M4 — 검색

- [x] 제목/본문/키워드/유사어 검색 — PostgreSQL FTS/`pg_trgm` 기반 `SearchService`와 관리자 테스트 API 구현
- [x] 부분 일치 (PostgreSQL `pg_trgm`) — V5 baseline migration 및 mapper 검색 쿼리 반영
- [x] 우선순위 가중치 (제목 > 키워드/유사어 > 본문) — 검색 score 산식에 제목/본문/키워드/노드 가중치 반영
- [x] 인기 검색어 집계 — 일자별 재집계 API와 관리자 운영 화면 기본형 구현
- [x] 검색 로그 저장 — 정상 검색/PII 차단 로그 저장과 90일 보존 삭제 API 구현
- [x] 검색 품질 1차 고도화 — `websearch_to_tsquery`, exact/prefix boost, keyword/title 우선순위, crawl document 검색 인덱스 보강
- [x] 향후 Elasticsearch 전환 가능 추상화 — 현재는 `SearchService`/mapper 경계 확보, 외부 엔진 전환은 후속
- [x] M4 검색 설계지시서/API/ERD/운영/테스트 초안 작성

---

## 6. M5 — 크롤링·데이터 수집

### 6.1 수집
- [x] URL 등록·관리 — 관리자 대상 목록/등록 화면과 API 기본형 구현
- [x] 로컬/내부망 크롤 테스트 허용 — 기본 SSRF 차단 유지, dev/test exact host allowlist로 공개 URL과 내부 검증 경로 병행 지원
- [x] 수동/자동(Quartz/Spring Scheduling) 실행 — 수동 실행 API와 Spring `@Scheduled` interval/cron 자동 실행 구현
- [x] 주기 설정 — 분 단위 interval과 Spring 6-field cron 표현식 고급 설정 UI/API 구현 완료
- [x] robots.txt 준수 — `CleverChatCrawler` user-agent 기준 allow/disallow 기본 판정 구현, origin 단위 10분 TTL 캐시 적용
- [x] 실패 로그 저장 + 알림 placeholder — 실패 로그 저장/관리자 조회/확인 처리/보존 정리 구현, webhook 외부 알림 기본형 연동
- [x] 크롤링 실패 분류 — `crawl_run_log.failure_code` 저장, 관리자 실행 이력/API `failureCode` 필터, 운영 코드표 반영

### 6.2 파싱
- [x] HTML 파서 (Jsoup) — 제목/본문 추출 기본형 구현
- [x] 광고/불필요 문구 제거 규칙 — script/style/nav/header/footer/aside 등 기본 제거
- [x] 중복 제거 (URL 해시 + 본문 해시) — 본문 해시 기준 중복 실행 처리 구현
- [x] 파싱 결과 미리보기 — 관리자 수집 문서 목록/본문 preview 화면 구현
- [x] 챗봇 백데이터 저장 — 수집 문서를 M4 검색 fallback 대상에 포함

---

## 7. M6 — 운영·모니터링

- [x] 로그 표준화 (JSON / Logback) — 내부 JSON writer와 `app.log`, `api.log`, `security.log`, `error.log` 분리 rolling appender 구현
- [x] 시스템 오류 / API 요청 / 보안 이벤트 / 이상 행위 분리 채널 — `/admin/api/**`, `/chat/api/**` access log filter와 인증/CSRF/로그인 보안 이벤트 기록 구현. 로그인 실패 spike 기본 룰 구현, 추가 탐지 룰은 후속
- [x] 통계 대시보드 (질문 수, 만족도, 실패율) — `/admin/statistics`, `/admin/api/statistics/summary` 1차 구현. 채팅/검색/크롤링/활성 시나리오 운영 지표를 오늘/최근 7일 기준으로 표시
- [x] 감사 로그 조회 — 기존 `audit_log` 기반 `/admin/audit-logs`, `/admin/api/audit-logs` 1차 구현. actor/action/targetType/기간 필터 제공
- [x] 공지사항 관리 - `/admin/notices`, `/admin/api/notices`, `/chat` 노출 공지 기본형 구현
- [x] 외부 알림 webhook/email 기본형 - `/admin/notifications`, `/admin/api/notifications/**`, 크롤링 실패 event 생성/발송/확인 처리 구현. Slack-compatible webhook adapter, env key rotation, SMTP email adapter 기본형 구현
- [x] 자동 백업 정책 (DB) - 앱 자동 백업은 후속, 온프레미스 PostgreSQL `pg_dump`/복구 리허설 운영 절차 문서화
- [x] 배포 롤백 절차 문서 - Flyway 실패/배포 전 백업/애플리케이션 롤백 판단 기준 운영 메모 반영

---

## 8. M7 — 보안·접근성·성능

### 8.1 보안 (OWASP / ASVS / 전자정부)
- [x] CSRF (인터셉터 기반 폼/AJAX 토큰 처리)
  - [x] 관리자 native POST form CSRF 스캔: `templates/admin/**`, `templates/admmgr/**`의 `method="post"` form과 `ADM.Form.submit` 호출 점검. `/logout` form은 `_csrfHidden` 적용, `/login` form은 로그인 전 진입점으로 적용 제외 정책 문서화 (2026-05-22)
  - [x] 신규 관리자 화면 PR 체크: `method="post"` form은 `_csrfHidden` fragment 포함, `ADM.Form.submit`/AJAX 화면은 CSRF header 제출 여부 확인
- [x] XSS (출력 이스케이프, CSP 헤더) - `SecurityHeadersFilter` 공통 CSP 적용, Thymeleaf escape 기준 유지
- [x] SQL Injection (파라미터 바인딩 강제, MyBatis `${}` 금지) - mapper XML `${}` 금지 정적 테스트 추가, API 오류 응답 SQL/stack trace 미노출 유지
- [x] 파일 업로드 검증 (MIME, 확장자, 크기, 백신 스캔 권장) - 현재 업로드 기능 없음. 도입 시 필수 검증으로 후속 처리 문서화
- [x] 입력값 Bean Validation - M4/M5/M6 API query/body 경계에 size/range/allow-list 검증 보강
- [x] 보안 헤더 (HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy) - HTML/API/health 응답 공통 적용, HSTS는 prod profile에서만 적용
- [x] HTTPS 강제 + 리다이렉트 - 앱은 forwarded header와 prod HSTS를 적용하고, redirect 강제는 nginx 운영 설정 기준으로 문서화
- [x] 세션 고정 방지 - 관리자 로그인 성공 시 세션 ID 재발급(`changeSessionId`)과 회귀 테스트 적용
- [x] 민감정보 암호화 (AES-256 또는 KMS) — OS env 키 기반 AES-256-GCM 필드 암호화 1차 구현. `chat_message.content`, `chat_feedback.comment`, `chat_failure.review_comment` 신규 저장분은 ciphertext 컬럼 저장, legacy plaintext fallback 유지
- [x] 개인정보 마스킹 처리 - 챗봇 free-text는 RRN/CARD 차단, EMAIL/PHONE 마스킹 저장. 검색어 PII는 전체 차단 및 원문 미저장

### 8.2 웹 접근성
- [x] 키보드 내비게이션 - `/chat` 및 관리자 운영 화면의 주요 조작을 native link/button/form 흐름으로 유지
- [x] 스크린리더(시맨틱 태그, ARIA) - 상태/오류 live region, 테이블 caption, `th scope` 반영
- [x] 이미지 alt - 현재 대상 화면에 신규 이미지 없음, 이미지 추가 시 필수 점검 기준 문서화
- [x] 명도 대비(WCAG AA 4.5:1) - `/chat` 및 관리자 공통 CSS에 visible focus 기준 보강, 브라우저 시각 QA 기준 문서화. 자동 axe/Playwright 점검은 후속
- [x] 폼 label 연결 - 입력/선택/동적 graph editor control에 label 또는 accessible name 반영
- [x] 접근성 있는 오류 메시지 - AJAX 성공/실패 메시지를 `role="status"`/`role="alert"`로 유지

### 8.3 성능
- [x] DB 인덱스 설계 검토 - M7.4에서 운영 조회/보존/통계 경로 기준 V9 보강
- [x] 캐시 (Caffeine / Redis) 적용 포인트 - M3 매칭 데이터 Caffeine TTL 300초 적용, 챗봇 rate limit은 optional Redis store 구현. 세션/매칭 캐시/클러스터 lock Redis 확대는 다중 노드 후속
- [x] 슬로우 쿼리 로깅 - MyBatis interceptor + `slow-query.log`, 기본 1000ms
- [x] 부하 테스트 (k6 또는 JMeter) - k6 기준 스크립트/운영 전 수동 점검 절차 반영

---

## 9. 향후 확장 (백로그)

- AI 답변 추천 연동 (RAG)
- 다국어 (i18n)
- 외부 REST API 공개
- 모바일 앱 대응
- Elasticsearch / 벡터 검색 도입
- 외부 알림 provider별 payload/template mapping
- 필드 암호화 대량 자동화/감사 리포트 고도화
- Redis 기반 세션/매칭 캐시/클러스터 lock 확대 검토. Rate limit optional Redis 기본형은 구현 완료
- ELK/외부 로그 수집 대시보드/알림 룰, 외부 스토리지 백업 연동

---

## 10. 우선순위 매트릭스 (상위 10개)

| 우선 | 항목 | 이유 |
|------|------|------|
| 1 | M4 검색 품질 고도화 | PostgreSQL FTS 품질 개선 후 ES 전환 판단 |
| 2 | M6 알림 adapter 고도화 | webhook/Slack/email/retry worker 기본형 이후 provider별 payload/template 확장 |
| 3 | Redis 기반 다중 인스턴스 전환 검토 | rate limit optional Redis 기본형 완료, cache/세션/cluster lock 전환 검토 |
| 4 | ELK/외부 로그 수집 검토 | Filebeat 샘플/로그 manifest 기본형 이후 대시보드와 알림 룰 연계 |
| 5 | 백업 자동화 에이전트 검토 | pg_dump 스크립트 샘플과 복구 검증 절차 완료, 외부 스토리지 연동은 후속 |
| 6 | 필드 암호화 운영 고도화 | backfill/rotate 감사 로그와 운영 검증 기본형 이후 자동화/대량 처리 개선 |
| 7 | 접근성 자동화 점검 도입 | Maven/JUnit 정적 검사 기본형 완료, axe/Playwright는 후속 |
| 8 | 챗봇 임베드 위젯 설계 | launcher-only 보안 설계와 샘플 완료, iframe/JS 위젯 런타임은 후속 |
| 9 | RAG/AI 답변 추천 설계 | provider-neutral scaffold 완료, 실제 provider/pgvector는 후속 |
| 10 | 외부 REST API 설계 | external API boundary + disabled stub 완료, 실제 business API는 후속 |

---

## 11. 결정 완료 (2026-05-07~2026-05-08)

상세는 `1.기획/결정사항.md` 참고.

- [x] ORM: **MyBatis 단독** (사용자 확정)
- [x] 빌드 도구: **Maven** (사용자 확정)
- [x] 배포 환경: **온프레미스** (외부 KMS/컨테이너 미가정, nginx 종단 HTTPS)
- [x] 세션 저장소: **Spring Session JDBC** (추가 인프라 불필요)
- [x] 캐시: **Caffeine** (다중 노드 시 Redis 전환)
- [x] 스케줄러: **Spring `@Scheduled`** (클러스터 시 Quartz JDBC 전환)
- [x] 검색 엔진: **PostgreSQL FTS + pg_trgm** (필요 시 ES 도입)
- [x] JDK 17 / Spring Boot 3.3.5 / PostgreSQL 16 / Flyway
- [x] M2 즉시 결정 기본값 채택: OPERATOR 운영 기본 계정 비활성 시딩과 `must_change_password=true`, `users.must_change_password` V3.1 분리, 서버 렌더 미리보기, 단순 폼/JSON 그래프 편집, 게시 버전 직접 수정 금지
- [x] M3 설계 결정 완료: HttpOnly `anonymous_id`, `chat_session` DB 상태 저장, M3 fallback stub, 운영자 고정 추천, 90일 보존, thumbs+코멘트, 조건식 M3 제외, 전용 페이지 우선
- [x] 쿠키 보안 속성: `SameSite=Lax`, `Secure=true`, `HttpOnly=true` 기본 적용. CSRF 토큰 쿠키는 `HttpOnly=false` 예외
- [x] 프록시 헤더: `server.forward-headers-strategy=framework`, 운영 프로파일에서만 부팅 경고 로그
- [x] 매칭 캐시 TTL: Caffeine 기본값 300초
- [x] 매칭 캐시 무효화 훅: 키워드 replace 저장 성공 시 scenario 단위 무효화 적용 완료. 게시/상태변경 훅은 M2/M3 후속 구현 범위에서 계속 추적
- [x] 초기 관리자 시딩: Flyway `V4_1__seed_default_admin.sql` + placeholder 토글로 단일화. dev/stage는 기본 관리자 생성, prod는 자동 생성 차단
- [x] M2 테스트 분리 정책 확정: 기본 `mvn test`는 순수 단위 테스트만 실행(Docker 불요), PIT(Testcontainers 통합 테스트)는 `@Tag("integration")` 부착 후 `mvn -Pit test`로 실행(Docker Desktop 기동 필수). 상세는 `3.개발/작업지시서/M2_테스트분리정책_작업지시서.md` 및 `4.테스트/01.테스트계획서/M2_테스트계획서.md` 참조.

### 추후 재검토 트리거
- 다중 노드 운영 → Redis(세션/캐시/cluster lock), Quartz JDBC, nginx `limit_req` 병행 여부 검토. Rate limit optional Redis store는 기본형 구현 완료
- 백데이터 100만건↑ 또는 검색 품질 이슈 → Elasticsearch
- AI 답변 추천 확정 → pgvector 기반 RAG
- 챗봇 임베드 위젯 제공 → CSP/CORS/frame-ancestors 별도 설계

---

## 12. 미결 결정/이월 항목

| # | 영역 | 미결 항목 | 책임 마일스톤 후보 | 참조 |
|---|---|---|---|---|
| 1 | M3 / M6 | Redis 확대 적용 결정 (세션 저장소, 매칭 캐시, scheduler/cluster lock, nginx `limit_req` 병행 여부) | 다중 노드 전환 트리거 시 | `1.기획/결정사항.md` §10, `3.개발/작업지시서/남은작업_완성로드맵_작업지시서.md` §11 |
