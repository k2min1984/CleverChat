# WORKORDER-crawl-display-fix 결과

## 변경 파일

- `src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java`
  - 검색 결과 선택지 라벨을 `제목 - 본문발췌`에서 `제목`만 표시하도록 변경.
- `src/test/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeServiceTest.java`
  - 검색 결과 라벨 기대값을 제목 단독으로 갱신.
- `src/main/java/kr/co/cleverchat/domain/crawl/browser/CrawlBrowserProperties.java`
  - `contentSelector`에서 `main`, `body` 전체 폴백 제거.
- `src/main/resources/application.yml`
  - 기본 `cleverchat.crawl.browser.content-selector` 동일 갱신.
- `src/main/java/kr/co/cleverchat/domain/crawl/browser/BoardCrawler.java`
  - 본문 추출 시 개행 보존.
  - 설정 셀렉터 후보를 설정 순서대로 평가.
  - 추출 전/후 잡 요소 및 잡 라인 제거.
- `src/main/java/kr/co/cleverchat/domain/crawl/service/CrawlService.java`
  - 정적 경로로 저장되는 KEPCO 상세/본문 문서에도 동일 잡문 제거 보조 적용.
  - 만족도/담당부서/자동 로그아웃 블록이 정적 페이지 본문 끝에 붙는 케이스 제거.
- `src/main/java/kr/co/cleverchat/domain/chatbot/controller/ChatPageController.java`
  - 표시단 `cleanDocumentContent` 보조 필터 유지/보강.
- `src/test/java/kr/co/cleverchat/domain/chatbot/controller/ChatPageControllerTest.java`
  - 표시단 보조 정리 테스트 갱신.
- `src/test/java/kr/co/cleverchat/domain/crawl/browser/KepcoBoardCrawlerIntegrationTest.java`
  - 상세 본문 개행/잡문 제거 검증 추가.

## 확정 셀렉터

- `rowSelector`
  - `main .board-list-tbody .board-title a[href^='javascript:fn_Detail'], main .card.board a.title[href^='javascript:fn_Detail']`
- `titleSelector`
  - `main .board-detail .sub-component-title, main .board-detail .detail-top h4, main h1, main h2`
- `contentSelector`
  - `main .detail-content, main .board-detail .detail-content, main .board-detail article, main .board-detail`

`KepcoDomProbeTest` 기준 대표 보드에서 `tbody tr=0`, 확정 row selector `10`, 상세 진입 후 `main .detail-content`가 실제 본문 래퍼로 확인됨.

## 본문 제외 규칙

- DOM 제거: `script`, `style`, `noscript`, `header`, `footer`, `nav`, 페이지네이션, 만족도/설문, 담당/부서, 로그아웃/세션, breadcrumb, 탭/서브타이틀 영역.
- 라인 제거: 만족도 설문, 자동 로그아웃/로그인 연장, 페이지 번호 입력, 담당부서/담당자/연락처/최종업데이트, 등록일/작성일/조회수/첨부파일/다운로드/미리보기/점자로보기/Loading.
- 본문 정규화: `\n` 유지, 줄 내부 탭/연속 공백만 단일 공백으로 정리. KEPCO 상세 본문에서 `?`로 깨져 붙는 번호/불릿 구분은 줄바꿈으로 복원.
- 정적 KEPCO 문서 보조 정리: 본문 말미의 만족도 설문, 자동 로그아웃, 담당부서~최종업데이트 블록 제거.

## Before / After 예시

Before:

```text
1. 기준 명칭 : 송변전 기자재공급자 품질평가 기준(5차) 2. 개정 이유 : 제개정 지침 반영 3. 주요 개정내용 1) 위원회 운영지침 반영 2) 신뢰품목 관리지침 34차 개정사항 반영 - 지침명 변경 및 정의 구체화 ...
```

After:

```text
1. 기준 명칭 : 송변전 기자재공급자 품질평가 기준(5차)
2. 개정 이유 : 제개정 지침 반영
3. 주요 개정내용
1) 위원회 운영지침 반영
2) 신뢰품목 관리지침 34차 개정사항 반영
- 지침명 변경 및 정의 구체화
- 용어정비
- 공급자 관리방식 개선
4. 의견 제출이 개정안에 대하여 의견이 있는 기관, 단체 또는 개인은 2026년 6월 30일까지(도착기준) ...
```

## 클린 슬레이트 및 재크롤

- 1차 정리: `tb_crawl_document=1547`, `tb_crawl_run_log=747`, `tb_crawl_coverage=8`, `tb_crawl_job=8` -> 모두 `0`.
- 중간 보정 후 재정리: `tb_crawl_document=263`, `tb_crawl_run_log=99`, `tb_crawl_coverage=2`, `tb_crawl_job=1` -> 모두 `0`.
- 블루스크린/중단 복구 후 최종 정리: `tb_crawl_document=12`, `tb_crawl_run_log=13`, `tb_crawl_coverage=0`, `tb_crawl_job=1` -> 모두 `0`.
- 최종 재크롤 잡: `crawl_job_no=12`, `SUCCESS`, `attempt=1`
  - 시작: `2026-06-18 16:09:41.494945+09`
  - 종료: `2026-06-18 16:29:42.644161+09`
  - 메시지: `Hybrid crawl visited 120 URL(s), 23 board list(s): static 96 success, 1 duplicate; board 1 success, 0 duplicate; 22 failed.`
- 최종 문서 수
  - `tb_crawl_document=106`
  - `tb_crawl_run_log=98`
  - `tb_crawl_coverage=1`
  - `tb_crawl_job=1`
  - `boardView.do` 문서 `15`, 그중 개행 포함 `11`
- 최종 보드 coverage
  - `list_pages=5`
  - `list_items_found=50`
  - `details_fetched=37`
  - `details_failed=13`
  - `truncated_yn=N`

## 검증

- `spotless:apply`: 성공.
- `ChatRuntimeServiceTest,ChatPageControllerTest`: `39`건 성공.
- `KepcoDomProbeTest,KepcoBoardCrawlerIntegrationTest` (`-P it`, KEPCO URL env): `2`건 성공.
- 전체 단위 테스트: `370`건 실행, 기존 허용 항목인 `AccessibilityTemplateTest` CSS 3건만 실패, 그 외 오류 `0`.

## 미완 / 주의

- 최종 재크롤에서 23개 보드 목록 후보 중 22개는 현재 공통 row selector와 다른 템플릿이어서 실패로 집계됨. 이번 작업 범위는 KEPCO 공통 셀렉터/본문 품질 보정이므로 보드 템플릿별 확장은 하지 않음.
- 지정 잡문 검색어 중 `담당부서`는 개인정보처리방침/영상정보 처리기기 방침/정보공개절차 같은 실제 본문에도 등장한다. 본문 훼손 방지를 위해 실제 내용 문맥의 `담당부서`는 남김.
- 서버는 `C:\cleverchat-run-codex`에서 `mvnw.cmd spring-boot:run`으로 기동한 상태로 유지함. jar 실행은 사용하지 않음.
