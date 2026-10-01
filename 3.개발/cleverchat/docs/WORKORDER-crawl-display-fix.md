# 작업지시서 — 크롤 검색 UX 품질 수정 2건 (버튼 라벨 / 상세 본문)

> 대상 실행자: Codex
> 성격: 앞선 KEPCO 튜닝의 "Follow-up"에서 추가된 두 동작의 **품질 버그만** 수정. **새 기능·다른 동작 변경 금지.**
> 강한 제약: 이번 작업은 **아래 2개 이슈에 한정.** 검색세션/시나리오/매칭/잡큐/보안/크롤 아키텍처를 건드리지 말 것. 스코프 확장 금지.

---

## 이슈 1 — 검색결과 버튼 라벨이 지저분(제목+본문발췌)

### 현상
검색결과가 여러 건일 때 버튼 라벨에 본문 일부가 길게 붙어 "쓸데없는 내용이 너무 많이" 들어감.
예: `공지사항 - 계약업무 처리기준 개정 안내...`

### 근본 원인
`ChatRuntimeService.searchOptionLabel(SearchResultItem)`(대략 815~830줄)이 `제목 - snippet` 형태로 라벨을 조립:
```java
return abbreviate(title, 48) + " - " + abbreviate(hint, 72);
```

### 수정
- **버튼 라벨은 제목만**(정리된 형태)으로. `searchOptionLabel`이 본문 발췌(hint/snippet) 부분을 붙이지 않게 하고, 제목을 적당히 abbreviate(예: 72자)만 해서 반환.
- 제목이 비어있을 때만 최소 폴백(예: URL/“검색 결과”).
- snippet 자체는 **버튼 클릭 후 답변 본문/내부 상세 페이지**에서 보이므로 라벨에서 빼도 정보 손실 없음.
- 관련 테스트(`ChatRuntimeServiceTest`)의 라벨 기대값을 제목-only로 갱신.

---

## 이슈 2 — 내부 상세 페이지(`/chat/crawl-documents/{id}`) 본문 품질

### 현상
1) 본문이 **줄바꿈 없이 한 덩어리**로 빽빽함.
2) 본문과 무관한 **페이지 잡내용**이 섞임: 페이지네이션(`1 2 3 … 다음 페이지 번호 입력 / 이동`), 만족도 설문(`이 페이지에서 제공하는 정보에 만족하셨습니까? 매우 만족 …`), 담당부서/연락처/최종업데이트, 자동 로그아웃 안내(`잠시후 자동 로그아웃됩니다 …`) 등.

### 근본 원인 (둘 다 크롤 추출 단계 문제)
- **줄바꿈 소실**: `BoardCrawler.firstText()`가 `text.replaceAll("\\s+", " ")`로 **개행까지 단일 공백으로 뭉갬** → DB content가 한 줄로 저장됨. 표시단 `ChatPageController.cleanDocumentContent`는 `\n` 기준으로 줄을 나눠 청소하는데, 개행이 없으니 한 줄로 처리되어 청소·가독성 둘 다 무력.
- **잡내용 혼입**: `CrawlBrowserProperties.contentSelector = "main .board-detail .detail-content, main .board-detail, article, main, body"`. 일부 게시판은 `.detail-content`/`.board-detail`가 안 맞아 **`main`/`body`로 폴백**되면서 헤더·메뉴·만족도설문·담당부서·자동로그아웃·페이지네이션 등 페이지 전체를 본문으로 긁음.

### 수정 (크롤 추출 보정 + 재크롤)
1. **개행 보존**: `BoardCrawler`의 본문 추출에서 개행을 살릴 것.
   - 줄 단위로 `innerText`(개행 포함)를 받아, **각 줄 내부의 연속 공백/탭만** 단일 공백으로 정리하고 **줄바꿈(\n)은 유지**. (현재의 `\\s+ → " "` 전면 치환을 본문에는 쓰지 말 것. 제목은 한 줄이라 무방.)
2. **본문 영역 정밀화 + 잡블록 제외**:
   - `contentSelector`의 `main`/`body` 같은 **전체 폴백을 제거하거나 최후수단으로만** 두고, KEPCO 상세의 실제 본문 래퍼를 `KepcoDomProbeTest`로 재확인해 정밀 셀렉터 확정(현재 폴백 타는 게시판 템플릿 식별).
   - 본문 추출 전에 **알려진 잡 요소를 제거**: 헤더/푸터/네비, 만족도 설문 영역, 페이지네이션, 담당부서/연락처/최종업데이트, 자동 로그아웃/세션 안내, script/style. (Playwright에서 본문 컨테이너 기준으로 해당 하위요소 제거 후 텍스트 추출, 또는 본문 래퍼를 잡내용 형제와 분리.)
   - 여러 KEPCO 게시판 템플릿에서 공통 동작하도록(셀렉터는 설정값 유지).
3. **클린 슬레이트 재크롤 (중요)**: 기존 `tb_crawl_document`에는 이미 뭉개진/잡내용 본문이 저장돼 있음. 추출을 고치면 새 본문은 **content_hash가 달라져 중복판정에 안 걸리고 새 행으로 INSERT** → 옛 나쁜 문서가 `SUCCESS`로 남아 **같은 게시물이 2벌 공존 → 검색에 중복/오답 노출**.
   - 따라서 **재크롤 전에 기존 크롤 문서를 비울 것**(클린 슬레이트). 범위: `tb_crawl_document`(검색이 읽는 대상) 전체 비우기 + 관련 `tb_crawl_run_log`/`tb_crawl_coverage`/`tb_crawl_job` 정리(런 이력 보존 원하면 run_log는 유지 가능). **크롤 테이블 한정.** 다른 도메인 테이블(시나리오/세션/감사/사용자 등) 데이터는 절대 건드리지 말 것.
   - 비우는 방식: 일회성 통제 작업(관리자 purge 액션 또는 크롤 테이블 한정 DELETE/TRUNCATE). **마이그레이션으로 데이터 삭제 금지**(다른 환경에서 재실행되면 안 됨).
   - 비운 뒤 보정된 추출로 **대상(KEPCO) 재크롤** → 깨끗한 본문만 검색에 노출되게.
   - 주의: 과거 챗 세션에 남은 `/chat/crawl-documents/{옛id}` 링크는 404가 될 수 있음(허용 — 옛 테스트 세션).
4. **표시단 청소는 보조로 유지/개선**: `ChatPageController.cleanDocumentContent`는 개행이 생기면 다시 효과를 가지므로 유지하되, 만족도/로그아웃/페이지네이션 같은 **블록 패턴**도 거를 수 있으면 추가(단, 과도 삭제로 본문 손실 주의). 핵심 수정은 크롤 추출 쪽.

### 검증
- `/chat/crawl-documents/{id}`에서 본문이 **문단/줄바꿈이 살아있고**, 만족도설문·로그아웃·페이지네이션·담당부서 잡내용이 **빠진** 상태로 보일 것.
- `KepcoBoardCrawlerIntegrationTest` 및 본문 추출 단위테스트로 개행 보존·잡블록 제외 확인. `ChatPageControllerTest`/`ChatRuntimeServiceTest` 갱신.

---

## 공통 제약 / 하지 말 것
- **이 2개 이슈 외 어떤 것도 바꾸지 말 것.** 새 엔드포인트/새 기능/리팩터링/다른 UX 변경 금지(지난 Follow-up처럼 스코프 확장 금지).
- 검색세션/시나리오/매칭/검색품질/잡큐/네이티브lib/보안 로직 불변.
- 데이터 삭제는 **크롤 테이블 한정 클린 슬레이트만 허용**(이슈2 §3). `tb_crawl_document`(+선택 run_log/coverage/job) 비우고 재크롤하는 건 OK. **그 외 도메인(시나리오/세션/감사/사용자/검색로그 등) 데이터는 절대 삭제·수정 금지.** 마이그레이션으로 데이터 삭제 금지.
- 셀렉터는 설정값 유지(하드코딩 최소화). 기존 챗봇/검색 동작 회귀 0.
- 빌드 전 `$env:MAVEN_OPTS="-Xshare:off"`. 변경 후 `spotless:apply` + **전체 단위테스트**(기존 `AccessibilityTemplateTest` 3건 CSS만 실패 허용, 그 외 0).
- KEPCO 실측은 `KepcoDomProbeTest`/`KepcoBoardCrawlerIntegrationTest`(integration, env `CLEVERCHAT_KEPCO_BOARD_URL`)로. Chromium은 이미 설치됨.
- 커밋/푸시는 사용자 지시 시에만.

## 완료 기준 (DoD)
- [ ] 이슈1: 버튼 라벨 = 제목만(정리), 테스트 갱신.
- [ ] 이슈2-개행: 본문 추출이 개행 보존, 상세 페이지에서 가독성 있는 줄바꿈.
- [ ] 이슈2-잡내용: 만족도/로그아웃/페이지네이션/담당부서 등 페이지 chrome 제외(크롤 추출 단계).
- [ ] 클린 슬레이트: 기존 크롤 문서 비우고 재크롤 → 검색에 옛 나쁜 데이터/중복 미노출(검증).
- [ ] 전체 단위테스트 회귀 0(CSS 3건 제외), spotless 적용.
- [ ] 결과보고서 `docs/WORKORDER-crawl-display-fix-result.md`: 변경 파일, 확정 본문 셀렉터/제외 규칙, before/after 본문 예시, 재크롤 결과, 미완 항목.
