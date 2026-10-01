# 소스 점검 및 크롤링 JSON 저장 작업 결과

작성일: 2026-09-21
대상: `3.개발/cleverchat`

## 점검 범위

변경 전 활성 `src/main`의 Java 192개, XML 23개, HTML 43개, JavaScript 20개, CSS 4개, 설정 4개를 대상으로 파일 동일성, 반복 코드, 참조와 주요 실행 경로를 점검했다. 크롤링·검색 연계·관리자 폼·인증 경계와 전체 자동 테스트를 중점 확인했다. `8.소스` 참고 자료, 빌드 산출물, 제3자 자산은 리팩터링 대상에서 제외했다. 기존 미커밋 변경을 보존하고 이번 작업 시작 시점의 소스를 `target/qa-baseline-source.zip`에 보관했다.

## 수정한 사항

| 사항 | 기존 문제 | 변경 |
|---|---|---|
| 크롤링 작업 실행 | 브라우저 활성화가 꺼지면 일반 HTML 작업도 대기열에서 처리되지 않음 | 작업 실행 설정 `cleverchat.crawl.worker.enabled`를 분리, 기본 활성화 |
| 등록/수정 체크박스 | 같은 이름의 hidden/checkbox 값이 동시에 전달되어 선택값이 잘못 해석될 수 있음 | 단일 checkbox와 서버 기본값 사용, 입력 DTO 검증 적용 |
| 문서 저장 중복 | 정적 수집과 브라우저 수집이 해시·형태소·저장을 별도 구현 | `CrawlDocumentStore`로 공통화 |
| 같은 URL의 변경된 본문 | 정적 수집 시 새로운 문서로 쌓여 기존 본문도 검색될 수 있음 | 기존 URL 문서를 갱신, 재수집 시 최신 제목·수집 시각 반영 |
| 중복 게시판 수집 | 작업 테이블에 허용되지 않은 `DUPLICATE` 상태 저장 시도 | 실행 이력은 `DUPLICATE`, 작업 완료 상태는 `SUCCESS` |
| 실패 이력 | 정적 페이지 수집 예외로 실패 로그도 트랜잭션 롤백될 수 있음 | 업무상 수집 실패의 이력을 보존하도록 트랜잭션 설정 보완 |
| 작업 중복 접수 | 동시에 실행을 요청하면 `NOT EXISTS` 확인을 둘 다 통과할 수 있음 | 대상 행을 잠근 후 작업 접수, 실제 DB 동시 호출 검증 |
| 자동 보존기간 정리 | 스케줄러가 관리자 전용 서비스를 호출해 권한 오류 발생 | 관리자용 호출과 내부 스케줄 호출 분리, 관리자 API 권한 유지 |
| HTTP 수집 | 리다이렉트 미지원, 인코딩 오해석 가능, 응답 크기 제한 없음 | 최대 5회 리다이렉트, 각 목적지 URL/robots 검증, charset/HTML 메타 인코딩 처리, 5 MiB 제한 |
| 검색 테스트 | 제목 부분 일치 가중치가 이전 SQL 값에 고정됨 | 현재 검색 정책과 기대값 정렬 |
| 실패 필터 | 브라우저 오류를 기록하지만 API 필터가 거부함 | 실패 코드 규격 통합, `BROWSER_ERROR`·`EXPORT_ERROR` 포함 |
| 불필요한 코드 | 호출되지 않는 정적 수집 일괄 실행 경로와 텍스트 정규화 함수 존재 | 사용처 확인 후 제거, 등록/수정 화면의 반복 처리 통합 |
| 통합 테스트 종료 | DB 컨테이너가 종료된 뒤 Spring 스케줄러/커넥션풀이 남음 | 테스트 클래스 종료 시 애플리케이션 컨텍스트 정리 |

## JSON 저장 기능

1. 관리자 → 크롤링 대상 → 등록 또는 수정으로 이동한다.
2. **수집한 데이터를 JSON 파일로 저장**을 선택한다.
3. **JSON 저장 폴더**에 서버의 절대 폴더 경로를 입력한다.
   - Windows 예: `C:/cleverchat/crawl-json`
   - Linux 예: `/data/cleverchat/crawl-json`
4. 저장 후 직접 실행하거나 자동 실행을 사용한다.
5. 대상 상세 화면의 실행 이력에서 저장 성공/실패와 경로를 확인한다.

경로는 브라우저를 실행하는 PC가 아니라 **CLEVERCHAT 서버의 파일 시스템** 기준이다. 서버 실행 계정에 폴더 생성·쓰기 권한이 있어야 한다. 상대 경로와 파일 경로는 허용하지 않는다. 기존 대상은 기본적으로 JSON 저장이 꺼져 있다.

저장 구조:

```text
<지정한 폴더>/target-<대상번호>/<수집일>/UUID.json
```

정적 HTML·브라우저 게시판 모두 페이지별 UTF-8 JSON 파일을 생성한다. DB상 중복 문서도 수집 시점별 새 파일로 남긴다. 파일명에 URL이나 제목을 사용하지 않는다. 임시 파일에 작성한 뒤 최종 파일명으로 이동하며, 저장 실패 시 실행 이력에 `EXPORT_ERROR`를 기록하고 수집한 DB 데이터는 보존한다. 여러 게시글 중 일부만 파일 저장에 실패해도 실행 결과에 실패를 표시한다.

JSON 필드: `schemaVersion`, `crawledAt`, `targetId`, `targetLabel`, `documentId`, `url`, `title`, `content`, `contentHash`, `httpStatus`.

`content`는 챗봇 검색에 사용하는 정제 본문이다. HTML 원문·첨부파일 바이너리는 포함하지 않는다. JSON 파일은 별도 아카이브이며 기존 DB 보존기간 정리 작업이 삭제하지 않는다. 파일과 DB의 커밋을 하나의 분산 트랜잭션으로 보장하지는 않는다.

API의 기존 대상 등록/수정 JSON에 다음 필드를 추가한다.

```json
{
  "url": "https://example.com/help",
  "label": "도움말",
  "useYn": "Y",
  "scheduleEnabled": false,
  "scheduleMode": "INTERVAL",
  "scheduleIntervalMinutes": 1440,
  "jsonExportEnabled": true,
  "jsonExportDirectory": "C:/cleverchat/crawl-json"
}
```

이 두 새 필드를 생략하는 기존 JSON 클라이언트의 수정 요청은 저장 설정을 유지한다. 관리자 HTML 폼에서 체크 해제하면 저장 기능이 꺼진다.

## 적용 방법

`V42__crawl_json_export.sql`은 대상 설정과 실행 이력의 JSON 저장 컬럼 및 실패 코드를 추가한다. 기존 마이그레이션은 수정하지 않았다. 새 빌드로 애플리케이션을 시작할 때 Flyway가 적용한다. 별도 테스트용 PostgreSQL 16 컨테이너에서 초기 스키마부터 V42까지 적용해 확인한다. 실제 사용 중인 PostgreSQL에는 이번 검증 데이터를 넣지 않았다.

- 일반 HTML 작업: `CLEVERCHAT_CRAWL_WORKER_ENABLED=true` (기본값)
- 브라우저 게시판 작업: 위 설정에 더해 `CLEVERCHAT_CRAWL_BROWSER_ENABLED=true` 및 Playwright 실행 환경 필요
- 전체 작업 처리를 잠시 중지하려면 `CLEVERCHAT_CRAWL_WORKER_ENABLED=false`

## 추가 검토 사항

아래 항목은 이번 변경으로 모두 해결됐다고 보지 않는다.

1. **브라우저 네트워크 정책**: `BoardCrawler`는 정적 HTTP 수집과 달리 브라우저 요청 전체에 URL/robots 정책을 일관되게 적용하는 처리가 없다. 클릭·팝업·리다이렉트·하위 리소스까지 포함한 별도 보완이 필요하다.
2. **게시판 범용성**: 현재 브라우저 선택자와 URL 분기는 한전 사이트 구조를 중심으로 작성되어 있다. 다른 사이트의 게시판은 실제 구조에 맞춘 선택자와 페이지 이동 검증이 필요하다.
3. **대규모 수집**: 기본 탐색/게시판 제한값 0은 무제한을 뜻한다. 작업 실행은 동기 루프이므로 `max-concurrent`를 높이는 것만으로 병렬 작업이 생기지 않는다. 긴 작업의 잠금·실행시간·재시도 설계는 별도 개선 대상이다.
4. **콘텐츠 중복 정책**: 기존 DB의 대상별 본문 해시 유일성 정책을 유지한다. 서로 다른 URL의 본문이 같으면 DB에서는 한 문서로 취급하며 JSON에는 실제 수집 URL을 남긴다. URL별 별도 문서 보존이 필요하면 스키마/검색 정책 변경이 필요하다.
5. **미사용 화면 후보**: `documentList.html`, `runList.html`, `ADM.CrawlRuns.js`, `ADM.CrawlTargets.js` 등은 현재 통합된 대상 관리 흐름에서 사용하지 않는 후보이다. 레거시 링크/화면 재사용 여부를 정한 뒤 정리할 수 있다. 임의 삭제하지 않았다.
6. **자바 모델의 반복 접근자**: 대화 본문/피드백/실패의 저장 모델과 조회 모델에 공통 필드 접근자 반복이 있다. 용도가 다른 조회 모델이므로 단순 중복만을 이유로 합치지 않았다.

## 검증 기록

- 변경 전 `mvn -B test`: 400개 중 검색 SQL 기대값 불일치 1건 실패.
- 최종 `mvn -B -Pit verify`: **429개 중 423개 통과, 6개 조건부 테스트 건너뜀, 실패/오류 0건**, 실행 가능 JAR 생성 성공.
- JSON/크롤링 통합 테스트 8개: 실제 HTTP 응답 수집 → Flyway V42 적용 PostgreSQL → 문서 검색 → JSON 저장, 중복/변경 본문 재수집, 저장 실패, robots 실패 이력, 저장 비활성화, 동시 작업 접수, 브라우저 결과 저장, 보존기간 정리, 관리자 화면 렌더링/CSRF 폼 저장을 각 테스트 시나리오에서 검증.
- 실제 외부 사이트 별도 검증: `KepcoBoardCrawlerIntegrationTest#crawlsConfiguredKepcoBoard` **1개 통과**. 한전 공지사항 `https://www.kepco.co.kr/home/media/newsroom/notice/boardList.do`에서 목록 1페이지/상세 최대 1건 제한으로 실제 Chromium 수집 수행. 한글 본문, 줄바꿈, 주요 화면 잡음 제거 확인.
- 전체 JavaScript 20개 `node --check`: 구문 오류 0건.
- 변경분의 공백 오류: 0건. 변경 Java 파일에 프로젝트 Spotless 포맷 적용.
- JSON 샘플은 [target/qa-artifacts/crawl-sample.json](../target/qa-artifacts/crawl-sample.json)에 보관. 로컬 테스트 페이지에서 실제 수집한 결과이며 외부 한전 데이터와 구분한다.
- 주요 로그: `target/qa-final-verify.log`, `target/qa-kepco-browser.log`, `target/qa-format.log`.
- 이번 작업만의 소스 비교: `target/qa-task.diff`, 변경 파일 목록: `target/qa-changed-files.json`.

조건부로 건너뛴 6개는 외부 사이트 환경변수를 요구하는 기존 테스트다. 한전 전체 메뉴/모든 게시판 전수 수집, 장시간 부하, 모든 외부 사이트 호환성까지 검증한 것은 아니다. 실제 화면의 브라우저 시각 검수는 별도로 수행하지 않았으며, 서버 템플릿 렌더링과 폼의 실제 저장 동작은 통합 테스트로 검증했다.
