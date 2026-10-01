# 작업지시서 — KEPCO BoardCrawler 셀렉터 실측 튜닝

> 대상 실행자: Codex
> 성격: 앞서 구현한 브라우저 크롤러(`domain/crawl/browser`)의 **KEPCO용 셀렉터/네비게이션을 실제 사이트에 맞게 튜닝**. 코드 구조 변경이 아니라 셀렉터·진입 로직 보정 + 실측 검증.
> 전제: 크롤러 코드/통합테스트는 이미 존재. Chromium 런타임은 이 머신 기본 경로에 설치됨(`C:\Users\C2R\AppData\Local\ms-playwright`). 다른 머신이면 `playwright install chromium` 1회.

---

## 0. 문제 (실측 확인됨)
`KepcoBoardCrawlerIntegrationTest` 실행 결과 **`listItemsFound = 0`**. 원인은 기본 셀렉터가 KEPCO 실제 DOM과 불일치.

### 실측 진단 (실제 Chromium 렌더 후, `KepcoDomProbeTest` 출력)
- 페이지 정상 로드: title = "제ㆍ개정 예고 | 내부규정 | 정보공개 | 한국전력공사".
- **`tbody tr = 0`, `table tr = 0`** → **목록이 테이블이 아님.** 현재 기본 `rowSelector="tbody tr"`가 0건인 근본 원인.
- **`ul li = 375`**(단, GNB/네비 메뉴 li 포함), `[class*=list] a = 139`, `[class*=board] a = 10`.
- **상세 진입 = onclick `fn_` (POST 폼) 기반**: `[onclick*=fn_] = 87`, `a[onclick] = 180`, `a[href*=boardView] = 2`(거의 href 없음).
- 즉 **렌더는 정상**, 셀렉터/진입방식만 안 맞음.

---

## 1. 목표
1. KEPCO 게시판의 **실제 목록 항목 셀렉터**(네비 메뉴 제외, 게시물 li/a만)와 **상세 진입 방식**, **상세 페이지 본문/제목 셀렉터**, **다음 페이지(페이지네이션) 셀렉터**를 실측으로 확정.
2. 그 값으로 `CrawlBrowserProperties` 기본 셀렉터를 보정(또는 필요 시 `BoardCrawler`의 클릭/대기 로직 보정).
3. **`KepcoBoardCrawlerIntegrationTest`가 통과**(`listItemsFound > 0`)하고, 추가로 **상세 1건 이상 본문 추출**(detailsFetched ≥ 1)되게.

---

## 2. 도구 (이미 있음)
- 진단: `src/test/java/kr/co/cleverchat/poc/KepcoDomProbeTest.java` — DOM 구조/셀렉터 카운트 덤프. 필요 시 셀렉터 후보를 추가해 재실행하며 정확한 목록 컨테이너를 좁혀라.
- 검증: `src/test/java/kr/co/cleverchat/domain/crawl/browser/KepcoBoardCrawlerIntegrationTest.java` — env `CLEVERCHAT_KEPCO_BOARD_URL` 받아 `listItemsFound > 0` 검증.
- 대상 셀렉터 설정 위치: `domain/crawl/browser/CrawlBrowserProperties.java`의 기본값 `rowSelector/titleSelector/contentSelector/nextSelector`, 그리고 `application.yml`의 `cleverchat.crawl.browser.*`(설정 오버라이드).
- 클릭/네비게이션 로직: `domain/crawl/browser/BoardCrawler.java`.

---

## 3. 작업 순서 (실측 루프)
1. **프로브로 정확한 목록 컨테이너 식별**: `KepcoDomProbeTest`를 돌려(아래 실행법), GNB/푸터/네비 li를 제외한 **게시물 목록 ul/li(또는 div) 컨테이너의 고유 class**를 찾아라. 컨테이너 후보를 프로브에 추가해 innerHTML을 출력하며 좁힐 것. 게시물 행 1건의 실제 HTML(제목 a, onclick, 날짜 등)을 확인.
2. **rowSelector 확정**: 게시물 **행의 클릭 대상**(보통 제목 `<a>`)을 정확히 가리키게. 예: `.board-list li a.subject` 류(실제 class로). 네비/메뉴가 안 걸리게 컨테이너로 한정. (`tbody tr` 제거)
3. **상세 진입 방식 확인**: 행 `<a>`의 onclick `fn_*`가 POST 폼(`frm`)을 제출하는지 확인. `BoardCrawler`는 현재 `row.click()` 후 `waitForLoadState(NETWORKIDLE)` → `goBack()` 구조. 클릭 대상이 onclick 가진 `<a>`면 실브라우저가 POST/CSRF 자동 처리하므로 동작해야 함. 안 되면(예: 클릭이 li로 가서 onclick 미발화) **클릭 대상을 내부 anchor로** 조정.
4. **상세 페이지 titleSelector/contentSelector 확정**: 상세(boardView) 렌더 후 제목/본문 영역의 실제 셀렉터를 프로브로 확인해 보정. 본문이 비면 detailsFailed로 잡히니 정확히.
5. **nextSelector(페이지네이션) 확정**: KEPCO 페이지네이션의 "다음" 요소(숫자/화살표) 실제 셀렉터로. (목록 AJAX 페이지 이동이 클릭으로 되는지 확인.)
6. **대기 보정(필요 시)**: 목록이 AJAX라 `navigate→NETWORKIDLE` 직후 0건이면, `page.waitForSelector(rowSelector, timeout)` 또는 짧은 대기를 추가(BoardCrawler). 단 과도한 고정 sleep 지양.
7. **검증**: `KepcoBoardCrawlerIntegrationTest` 통과(`listItemsFound > 0`). 가능하면 maxDetails 작게 둔 채 **detailsFetched ≥ 1 + 본문 텍스트 비어있지 않음**까지 확인(테스트에 assert 추가 가능).

---

## 4. 실행법 (이 머신)
```powershell
$env:MAVEN_OPTS="-Xshare:off"
$env:CLEVERCHAT_KEPCO_BOARD_URL="https://www.kepco.co.kr/home/disclosure/regulations/revision/revision/boardList.do"
# 진단:
.\mvnw.cmd "-Dspotless.check.skip=true" "-P" "it" "-Dtest=KepcoDomProbeTest" test
#   출력 확인: target/surefire-reports/*KepcoDomProbeTest* 의 [PROBE] 라인(XML system-out)
# 검증:
.\mvnw.cmd "-Dspotless.check.skip=true" "-P" "it" "-Dtest=KepcoBoardCrawlerIntegrationTest" test
```
- 다른 KEPCO 보드도 같은 구조인지 2번째 URL로 교차 확인 권장: `https://www.kepco.co.kr/home/customer/library/electricity-statistics/kepco-stats/boardList.do`
- 셀렉터는 가능한 **여러 KEPCO 보드에 공통**되게(게시판 CMS 공통 class). 보드마다 다르면 `application.yml`/타깃 설정으로 오버라이드 가능함을 유지(하드코딩 최소화).

---

## 5. 제약 / 하지 말 것
- 크롤러 **아키텍처·기능 플래그·잡큐·DB·보안(네이티브lib 업로드) 구조는 건드리지 말 것.** 이번 건은 **셀렉터/진입/대기 보정 + 검증**에 한정.
- 기존 챗봇/검색/시나리오/세션 로직, 검색 품질 로직 변경 금지.
- 공유 dev DB 데이터 삭제/수정 금지(이 작업은 통합테스트만으로 검증되며 DB 불필요).
- 셀렉터는 설정값 유지(과한 하드코딩 금지). 기본값만 KEPCO에 맞게 보정.
- 빌드 전 `$env:MAVEN_OPTS="-Xshare:off"`. 변경 후 `spotless:apply`. 일반 단위테스트(전체) 회귀 0(기존 `AccessibilityTemplateTest` 3건 CSS 제외).
- 마무리 시 `KepcoDomProbeTest`는 진단용 throwaway이므로 **제거하거나 유지 결정**을 결과보고서에 명시(integration 태그라 기본 빌드엔 영향 없음).
- 커밋/푸시는 사용자 지시 시에만.

---

## 6. 완료 기준 (DoD)
- [ ] KEPCO 목록 항목 셀렉터/상세 진입/본문·제목/페이지네이션 셀렉터 실측 확정, `CrawlBrowserProperties` 기본값 보정.
- [ ] `KepcoBoardCrawlerIntegrationTest` 통과(`listItemsFound > 0`), detailsFetched ≥ 1 + 본문 비어있지 않음 확인.
- [ ] 2개 이상 KEPCO 보드에서 동작(공통 셀렉터) 교차 확인.
- [ ] 전체 단위테스트 회귀 0, spotless 적용.
- [ ] 결과보고서 `docs/WORKORDER-kepco-selector-tuning-result.md`: 확정 셀렉터, before/after 카운트, 교차확인 결과, 변경 파일, 미완 항목.
