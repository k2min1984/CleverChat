# CLEVERCHAT 작업 분해 구조 (WBS)

> 출처: `9.참고/기능요청.md`
> 작성일: 2026-05-07
> 목표: 시나리오 챗봇 시스템 — Spring Boot + PostgreSQL + Thymeleaf

---

## 0. 마일스톤 개요

| 단계 | 명칭 | 주요 산출물 | 예상 비중 |
|------|------|-------------|-----------|
| M0 | 프로젝트 셋업 | 골격 코드, CI 베이스, 환경 분리 | 5% |
| M1 | 인증·관리자 기반 | 로그인, 권한, 감사 로그 | 10% |
| M2 | 시나리오 도메인 | 시나리오 CRUD, 노드 구조 | 20% |
| M3 | 챗봇 런타임 | 시나리오 진행 엔진, 대화 이력 | 20% |
| M4 | 검색 | 키워드/유사어/우선순위 | 10% |
| M5 | 크롤링 | URL 등록, 자동 수집, 파싱 | 15% |
| M6 | 운영·모니터링 | 로그, 대시보드, 백업 | 10% |
| M7 | 보안·접근성 강화 | OWASP, WA 준수, HTTPS | 10% |

---

## 1. M0 — 프로젝트 셋업

### 1.1 환경 구성
- [ ] JDK / Gradle(or Maven) 버전 확정
- [ ] Spring Boot 프로젝트 생성 (Web, Thymeleaf, Validation, Actuator, MyBatis)
- [ ] PostgreSQL 로컬 인스턴스 준비 (Docker Compose 권장)
- [ ] `application.yml` → `application-dev.yml` / `application-stage.yml` / `application-prod.yml` 분리
- [ ] 프로파일별 시크릿 외부화 정책 정의 (env var / Vault)

### 1.2 형상 관리
- [ ] `git init` 및 `.gitignore` (Java, Gradle, IDE, OS, secrets)
- [ ] 브랜치 전략 (예: trunk-based 또는 git-flow) 합의
- [ ] 커밋 컨벤션 합의 (Conventional Commits 권장)

### 1.3 품질 베이스라인
- [ ] 린터/포매터 (Checkstyle / Spotless)
- [ ] 테스트 프레임워크 (JUnit5 + Mockito + Testcontainers)
- [ ] CI 파이프라인 초안 (build → test → static analysis)

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

### 2.3 인가
- [x] URL 기반 접근 제어 (HandlerInterceptor 매핑)
- [ ] 서비스 계층 권한 검증 헬퍼
- [x] 권한별 메뉴 노출 제어 (세션 권한/모델 속성) 베이스

### 2.4 감사
- [x] 관리자 작업 감사 로그 인터셉터/AOP 베이스
- [ ] 데이터 수정 이력 (감사 테이블 또는 자체 트리거)

---

## 3. M2 — 시나리오 도메인

### 3.1 데이터 모델
- [ ] `Scenario`, `ScenarioVersion`, `Node`, `NodeOption(Edge)`, `Category`, `Keyword`, `Synonym`
- [ ] 노드 타입: 질문 / 답변 / 분기 / 종료
- [ ] 활성화/비활성화 플래그, 버전 보관 정책
- [x] M2 ERD/인덱스 초안 작성 (`2.설계/02.DB설계서/M2_시나리오ERD.md`, `인덱스설계.md`)

### 3.2 관리 화면
- [ ] 시나리오 목록/검색
- [ ] 시나리오 등록/수정/삭제
- [ ] 노드 트리 편집 UI
- [ ] 키워드/유사어 관리
  - [x] 키워드 replace 저장 성공 시 매칭 캐시 무효화 훅 적용 (`ScenarioMatchingCacheInvalidator.onScenarioChanged(scenarioId)`, 2026-05-21)
- [ ] 미리보기 (실제 챗봇 화면 재사용)
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
- [ ] 챗봇 전용 페이지 구현 (반응형) — 설계 완료: `2.설계/01.화면설계서/M3_사용자화면목록.md`
- [ ] 버튼 선택형 진행 구현 — 설계 완료: `2.설계/03.API설계서/M3_챗봇런타임API.md`
- [ ] 자유 텍스트 입력 → 검색 fallback 구현 — 설계 완료: `2.설계/04.아키텍처/M3_상태전이도.md`
- [ ] 추천 질문 노출 구현 — 설계 완료: `2.설계/02.DB설계서/M3_챗봇런타임ERD.md`
- [x] M3 사용자 화면/접근성 설계 완료 (`2.설계/01.화면설계서/M3_사용자화면목록.md`, `M3_접근성체크리스트.md`)

### 4.2 진행 엔진
- [ ] 세션별 진행 상태 관리 구현 (DB 테이블) — 설계 완료: `2.설계/02.DB설계서/M3_챗봇런타임ERD.md`
- [ ] 노드 전이 로직, 분기 평가 구현 — 설계 완료: `2.설계/04.아키텍처/M3_상태전이도.md`
- [ ] 이전 대화 이력 조회 구현 — 설계 완료: `2.설계/03.API설계서/M3_챗봇런타임API.md`
- [x] M3 ERD/API/상태전이/운영 설계 완료 (`M3_챗봇런타임ERD.md`, `M3_챗봇런타임API.md`, `M3_상태전이도.md`, `M3_운영메모.md`)

### 4.3 피드백
- [ ] 답변 만족도 평가 구현 (thumbs + 코멘트) — 설계 완료: `2.설계/03.API설계서/M3_챗봇런타임API.md`
- [ ] 답변 실패 시 관리자 확인 플래그 구현 — 설계 완료: `2.설계/06.운영메모/M3_운영메모.md`
- [x] `chat_failure.detail` 표준 키 사전 설계 완료 (`2.설계/02.DB설계서/M3_챗봇런타임ERD.md`)

### 4.4 보안·운영·테스트
- [ ] `anonymous_id` 쿠키, CSRF, 본인 세션 검증 구현 — 설계 완료: `2.설계/05.보안설계서/M3_사용자화면보안노트.md`
- [ ] PII 차단/마스킹, IP/UA 해시, rate limit 구현 — 설계 완료: `3.개발/작업지시서/M3_챗봇런타임_설계지시서.md`
- [ ] 만료 배치와 90일 파기 배치 구현 — 설계 완료: `2.설계/06.운영메모/M3_운영메모.md`
- [ ] M3 단위/통합/화면/보안 회귀 테스트 구현 — 설계 완료: `4.테스트/02.테스트케이스/M3_챗봇런타임_테스트케이스.md`
- [ ] 관리자 세션/실패 큐/피드백/추천 질문 화면 구현 — 설계 완료: `2.설계/01.화면설계서/M3_운영자화면목록.md`

### 4.5 시나리오 게시 UX 개선

> 분석 근거: `3.개발/cleverchat/docs/Scenario_Publish_UX_분석.md`,
>            `3.개발/cleverchat/docs/Scenario_Activate_Button_조건분석.md` (2026-05-22)
> 대상 화면: `src/main/resources/templates/admmgr/scenario/scenarioView.html`,
>            컨트롤러: `AdmScenarioController`, `AdmScenarioApiController`

#### 단기 (관리자 화면 즉시 안내 강화)
- [ ] DRAFT 게시 버튼 비활성화: `version.startNodeId == null`인 경우 `th:disabled` 적용
- [ ] 비활성 버튼 tooltip: 상황별 문구 (`title` + `aria-describedby` 병행)
- [ ] 게시 버튼 인접 help text: 선행조건 안내
- [ ] 실패 메시지 표준화: 5종 (`저장된 그래프가 없습니다.` 외) 사용자 행동 중심 문구 매핑

#### 중기 (응답 구조 분리·재사용)
- [ ] form POST 실패 시 상세 화면 redirect + Flash 메시지 흐름 도입
- [ ] 화면/API 응답 구조 분리 (Controller redirect / ApiController JSON)
- [ ] 게시 가능 여부 DTO 도입 (`hasGraph`, `hasStartNode`, `publishable`, `reason`)
- [ ] 관리자 form POST UX 공통화 (`activate`/`deactivate`/`new draft`/`publish`)

---

## 5. M4 — 검색

- [ ] 제목/본문/키워드/유사어 검색 — 설계 완료: `2.설계/03.API설계서/M4_검색API.md`
- [ ] 부분 일치 (PostgreSQL `pg_trgm`) — 설계 완료: `3.개발/작업지시서/M4_검색_설계지시서.md`
- [ ] 우선순위 가중치 (제목 > 키워드/유사어 > 본문) — 설계 완료: `2.설계/02.DB설계서/M4_검색ERD.md`
- [ ] 인기 검색어 집계 — 설계 완료: `2.설계/06.운영메모/M4_검색_운영메모.md`
- [ ] 검색 로그 저장 — 설계 완료: `2.설계/02.DB설계서/M4_검색ERD.md`
- [ ] 향후 Elasticsearch 전환 가능 추상화 — 설계 완료: `3.개발/작업지시서/M4_검색_설계지시서.md`
- [x] M4 검색 설계지시서/API/ERD/운영/테스트 초안 작성

---

## 6. M5 — 크롤링·데이터 수집

### 6.1 수집
- [ ] URL 등록·관리
- [ ] 수동/자동(Quartz/Spring Scheduling) 실행
- [ ] 주기 설정 (cron 표현식)
- [ ] robots.txt 준수
- [ ] 실패 로그 저장 + 알림

### 6.2 파싱
- [ ] HTML 파서 (Jsoup)
- [ ] 광고/불필요 문구 제거 규칙
- [ ] 중복 제거 (URL 해시 + 본문 해시)
- [ ] 파싱 결과 미리보기
- [ ] 챗봇 백데이터 저장

---

## 7. M6 — 운영·모니터링

- [ ] 로그 표준화 (JSON / Logback)
- [ ] 시스템 오류 / API 요청 / 보안 이벤트 / 이상 행위 분리 채널
- [ ] 통계 대시보드 (질문 수, 만족도, 실패율)
- [ ] 공지사항 관리
- [ ] 자동 백업 정책 (DB)
- [ ] 배포 롤백 절차 문서

---

## 8. M7 — 보안·접근성·성능

### 8.1 보안 (OWASP / ASVS / 전자정부)
- [ ] CSRF (인터셉터 기반 폼/AJAX 토큰 처리)
- [ ] XSS (출력 이스케이프, CSP 헤더)
- [ ] SQL Injection (파라미터 바인딩 강제, MyBatis `${}` 금지)
- [ ] 파일 업로드 검증 (MIME, 확장자, 크기, 백신 스캔 권장)
- [ ] 입력값 Bean Validation
- [ ] 보안 헤더 (HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy)
- [ ] HTTPS 강제 + 리다이렉트
- [ ] 민감정보 암호화 (AES-256 또는 KMS)
- [ ] 개인정보 마스킹 처리

### 8.2 웹 접근성
- [ ] 키보드 내비게이션
- [ ] 스크린리더 (적절한 시맨틱 태그, ARIA)
- [ ] 이미지 alt
- [ ] 명도 대비 (WCAG AA 4.5:1)
- [ ] 폼 label 연결
- [ ] 접근성 있는 오류 메시지

### 8.3 성능
- [ ] DB 인덱스 설계 검토
- [ ] 캐시 (Caffeine / Redis) 적용 포인트
- [ ] 슬로우 쿼리 로깅
- [ ] 부하 테스트 (k6 또는 JMeter)

---

## 9. 향후 확장 (백로그)

- AI 답변 추천 연동 (RAG)
- 다국어 (i18n)
- 외부 REST API 공개
- 모바일 앱 대응
- Elasticsearch / 벡터 검색 도입

---

## 10. 우선순위 매트릭스 (상위 10개)

| 우선 | 항목 | 이유 |
|------|------|------|
| 1 | M0 전체 | 다른 작업의 전제 |
| 2 | M1 인증/권한 | 모든 관리 기능의 게이트 |
| 3 | M2 시나리오 모델 | 챗봇의 데이터 근간 |
| 4 | M3 챗봇 런타임 (기본 흐름) | 사용자 가시 핵심 |
| 5 | M7.1 보안 기본기 (CSRF/XSS/SQLi) | 코드 작성과 동시에 적용 |
| 6 | M2 관리 UI (시나리오 CRUD) | 운영자 입력 채널 |
| 7 | M4 검색 (키워드/부분일치) | 자유 입력 fallback |
| 8 | M3 대화 이력 / 만족도 | 운영 피드백 루프 |
| 9 | M5 크롤링 기본 (수동) | 데이터 공급 |
| 10 | M6 로그/모니터링 | 안정 운영 진입 |

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
- [x] M2 테스트 분리 정책 확정: 기본 `mvn test`는 순수 단위 테스트만 실행(Docker 불요), PIT(Testcontainers 통합 테스트)는 `@Tag("integration")` 부착 후 `mvn -Pit test`로 실행(Docker Desktop 기동 필수). 상세는 `3.개발/작업지시서/M2_테스트분리정책_작업지시서.md` 및 `4.테스트/01.테스트계획서/M2_테스트계획서.md` 참조.

### 추후 재검토 트리거
- 다중 노드 운영 → Redis(세션/캐시), Quartz JDBC, Rate limit 분산 저장소
- 백데이터 100만건↑ 또는 검색 품질 이슈 → Elasticsearch
- AI 답변 추천 확정 → pgvector 기반 RAG
- 챗봇 임베드 위젯 제공 → CSP/CORS/frame-ancestors 별도 설계

---

## 12. 미결 결정/이월 항목

| # | 영역 | 미결 항목 | 책임 마일스톤 후보 | 참조 |
|---|---|---|---|---|
| 1 | M3 / M6 | Rate limit 멀티 인스턴스 저장소 결정 (Redis 토큰버킷 vs PostgreSQL 슬라이딩 윈도우 vs nginx `limit_req`) | M6 (운영·모니터링) 또는 다중 노드 전환 트리거 시 | `1.기획/결정사항.md` §10, `3.개발/작업지시서/M3_챗봇런타임_설계지시서.md` §1.9 |
