# RESULT

작성일: 2026-05-29

## 1. 수행 요약

`docs` 내 작업지시서와 설계서를 현재 저장소 기준으로 검토하고, 사용자 결정 없이 바로 진행 가능한 항목을 우선 처리했다.

## 2. 구현/수정 완료

### 2.1 Maven wrapper 복구

- 파일: `3.개발/cleverchat/mvnw.cmd`
- 내용:
  - PowerShell wrapper가 `.m2` 경로의 `Target[0]`을 null 상태에서 접근해 실패하던 문제를 방어적으로 수정했다.
- 확인:
  - `.\mvnw.cmd -version` 성공

### 2.2 M1 서비스 계층 권한 헬퍼

- 추가:
  - `RoleGuard`
  - `RequireRole`
  - `RoleGuardAspect`
  - `RoleGuardTest`
- 적용:
  - 시나리오, 카테고리, 키워드, 크롤링, 검색 운영, 챗봇 운영, 필드 암호화 유지보수, 공지/알림 mutation 서비스 진입점에 `@RequireRole("OPERATOR")` 적용
- 문서:
  - `1.기획/WBS.md`의 서비스 계층 권한 검증 헬퍼 항목을 완료 상태로 갱신

### 2.3 백업/PITR 복구 산출물

- 추가:
  - `5.배포/05.백업복구절차서/백업정책.md`
  - `5.배포/05.백업복구절차서/PITR_복구절차.md`
  - `5.배포/05.백업복구절차서/복구리허설_체크리스트.md`
- 보강:
  - `2.설계/06.운영메모/M6_운영관측_운영메모.md`에 정식 백업/복구 산출물 위치 반영

### 2.4 Testcontainers 환경 복구 문서

- 추가:
  - `3.개발/cleverchat/docs/Testcontainers_환경복구.md`
- 내용:
  - Windows Docker named pipe 점검 순서
  - CI/Linux Docker 대체 검증 기준
  - 실패 기록 항목

### 2.5 릴리즈 마감 체크리스트

- 추가:
  - `5.배포/01.배포계획서/릴리즈마감_체크리스트.md`
- 내용:
  - RC 검증 명령
  - 운영 smoke
  - 배포 전 백업
  - 태그/롤백 기준

### 2.6 사용자 결정 필요 항목 분리

- 추가:
  - `PENDING.md`
- 분리 항목:
  - Redis 확대 적용 범위
  - M9 RAG/AI 실제 provider
  - M10 외부 REST API 공개 범위
  - CSP nonce 전환
  - 백업 외부 스토리지/PITR 운영 수준
  - 외부 알림 provider 고도화
  - M6 이상행위 탐지 고도화 기준

### 2.7 M6 알림 retry worker

- 수정:
  - `OpsNotificationService.retryDueEvents`
  - `OpsMapper.findDueNotificationEvents`
  - `OpsNotificationServiceTest.retryDueEventsDeliversOnlyMapperSelectedEvents`
- 내용:
  - `notification_event`의 `RETRY` 상태 중 `next_retry_at <= now`, `attempt_count < 3`인 이벤트만 60초 기본 주기로 재전송한다.
  - 재전송 대상 채널은 enabled channel만 사용한다.
  - 기존 open event 재사용, 최대 3회 재시도, 실패 시 업무 트랜잭션 비차단 정책은 유지했다.
- 문서:
  - M6 운영메모와 남은작업 로드맵에서 retry worker 기본형 완료 상태를 반영했다.

### 2.8 M5 cron schedule preview

- 추가:
  - `POST /admin/api/crawl-targets/schedule/preview`
  - `CrawlService.previewSchedule`
  - `/admin/crawl-targets` Preview 버튼과 `ADM.CrawlTargets.js` AJAX 연결
- 내용:
  - 저장 전 `INTERVAL`/`CRON` 설정의 다음 5회 실행 시각을 확인할 수 있게 했다.
  - 기존 cron 최소 5분 검증, disabled schedule의 `nextRunAt=null`, 저장 API 흐름은 유지했다.
- 문서:
  - M5 운영메모와 남은작업 로드맵에서 cron 미리보기 기본형 완료 상태를 반영했다.

### 2.9 M6 이상행위 탐지 기본 룰

- 수정:
  - `LoginAuditService`
  - `LoginAuditServiceTest`
- 내용:
  - 관리자 로그인 실패 횟수가 잠금 임계값 직전이면 `LOGIN_FAILURE_SPIKE` security 이벤트를 기록한다.
  - 임계값 도달 시 기존 `LOGIN_LOCKED` 이벤트와 계정 잠금 흐름은 유지한다.
  - 별도 DB/Flyway 없이 기존 `OpsEventLogger`/`security.log` 채널을 사용한다.
- 문서:
  - M6 운영메모, WBS, 남은작업 로드맵에서 로그인 실패 spike 기본 룰 완료와 추가 탐지 룰 후속 범위를 분리했다.

### 2.10 M5 크롤링 실패 분류

- 추가:
  - `V17__crawl_failure_code.sql`
  - `crawl_run_log.failure_code`
- 수정:
  - `CrawlRunLog`
  - `CrawlService`
  - `CrawlMapper`
  - `/admin/crawl-runs`, `/admin/api/crawl-runs`, `/admin/api/crawl-runs/failures`
- 내용:
  - 크롤링 실행 결과에 `ROBOTS_BLOCKED`, `HTTP_ERROR`, `TIMEOUT`, `PARSE_ERROR`, `DUP_HASH`, `FETCH_ERROR`, `NOT_HTML`, `URL_BLOCKED`, `SCHEDULE_INVALID`, `SYSTEM_ERROR` 분류 코드를 저장한다.
  - 관리자 실행 이력 화면과 API에서 `failureCode` 필터를 지원한다.
- 문서:
  - M5 운영메모, API 목록, WBS, 남은작업 로드맵에 실패 분류 코드표와 필터 상태를 반영했다.

### 2.11 M5 robots.txt TTL 캐시

- 수정:
  - `CrawlRobotsService`
  - `CrawlRobotsServiceTest`
- 내용:
  - `robots.txt` 조회 결과를 origin 단위로 10분 동안 캐시한다.
  - 같은 origin에서는 robots 본문을 재사용하되, 요청 URL path별 allow/disallow 판정은 매번 다시 계산한다.
  - 404, 조회 실패, 명시 차단 등 기존 fail-open/fail-close 정책은 유지했다.
- 문서:
  - M5 운영메모, WBS, 남은작업 로드맵에 robots TTL 캐시 기본형 완료 상태를 반영했다.

### 2.12 문서 stale 표현 정리

- 수정:
  - `1.기획/WBS.md`
  - `1.기획/결정사항.md`
  - `3.개발/작업지시서/남은작업_완성로드맵_작업지시서.md`
- 내용:
  - 서비스 계층 권한 헬퍼, optional Redis rate limit, CI 초안, Testcontainers 대체 게이트 표현을 현재 저장소 구현 상태에 맞게 정리했다.
  - 사용자가 결정해야 하는 Redis 확대, CSP nonce, AI provider, 외부 API 공개 범위 등은 `PENDING.md`에 유지했다.

### 2.13 M7 세션 고정 방지 보강

- 수정:
  - `LoginController`
  - `LoginControllerTest`
- 내용:
  - 관리자 로그인 성공 시 `request.changeSessionId()`를 호출해 기존 세션을 유지하되 세션 ID를 재발급한다.
  - 기존 CSRF 재발급과 관리자 세션 속성 교체 흐름은 유지했다.

### 2.14 PENDING 판단 원칙 정리

- 수정:
  - `PENDING.md`
- 내용:
  - "온프레미스 전용"이 아니라 "온프레미스 환경에서도 동일 동작 가능한 기본 경로 + optional 외부 adapter" 원칙을 명시했다.
  - Redis, RAG/AI, 외부 REST API, CSP nonce, 백업/PITR, 외부 알림, 이상행위 탐지 항목마다 기본 경로, optional 확장, 권장 작업을 분리했다.

## 3. 검증 결과

| 단계 | 명령 | 결과 |
| --- | --- | --- |
| Maven wrapper 확인 | `.\mvnw.cmd -version` | 성공 |
| 빌드 1차 | `.\mvnw.cmd clean test` | 실패: Maven parent POM 다운로드 차단 |
| 오프라인 compile 확인 | `.\mvnw.cmd -o -DskipTests compile` | 실패: parent POM이 로컬 캐시에 없음 |
| 빌드 2차 | `.\mvnw.cmd clean test` | 성공: Tests run 306, Failures 0, Errors 0, Skipped 0 |
| 빌드 3차 | `.\mvnw.cmd clean test` | 성공: Tests run 309, Failures 0, Errors 0, Skipped 0 |
| 빌드 4차 | `.\mvnw.cmd clean test` | 성공: Tests run 311, Failures 0, Errors 0, Skipped 0 |
| 빌드 5차 | `.\mvnw.cmd clean test` | 성공: Tests run 312, Failures 0, Errors 0, Skipped 0 |
| 빌드 6차 | `.\mvnw.cmd clean test` | 성공: Tests run 314, Failures 0, Errors 0, Skipped 0 |
| 빌드 7차 | `.\mvnw.cmd clean test` | 성공: Tests run 314, Failures 0, Errors 0, Skipped 0 |
| Spotless 검사 | `.\mvnw.cmd spotless:check` | 성공 |
| diff 공백 검사 | `git diff --check` | 성공, CRLF 경고만 존재 |
| Docker 감지 | `docker version` | 실패: Docker API named pipe 접근 권한 없음 |

초기 빌드 실패:

```text
Non-resolvable parent POM for kr.co.cleverchat:cleverchat:0.0.1-SNAPSHOT:
Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:3.3.5
from/to central (https://repo.maven.apache.org/maven2): Permission denied: getsockopt
```

해석:

- Maven wrapper 자체 오류는 수정 완료.
- 초기 실패 원인은 애플리케이션 코드가 아니라 dependency 다운로드 제한이었다.
- 네트워크 접근이 허용된 Maven 실행에서 `.\mvnw.cmd clean test`가 통과했다.
- Docker/Testcontainers `.\mvnw.cmd -Pit test`는 Docker named pipe 접근 권한 복구 후 별도 재검증 대상으로 남긴다. 현재 `docker version`도 `permission denied while trying to connect to the docker API at npipe:////./pipe/docker_engine`로 실패한다.

## 4. 남은 작업

현재 즉시 코드로 진행하지 않고 `PENDING.md`에 사용자 결정이 필요한 항목을 분리했다. 다음 구현 후보는 결정이 필요 없는 범위부터 계속 진행한다.

- M7 CSP nonce 적용 검토 결과에 따른 실제 전환
- Docker/Testcontainers `-Pit test` 환경 복구 후 통합 테스트 재실행

## 5. 커밋 상태

- staging/commit은 수행하지 않았다.
- 기존 누적 워킹트리 변경은 보존했다.
