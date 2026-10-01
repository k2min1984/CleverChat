# 작업지시서 — 검색 경험 정비(라벨 정제 + 적합도 개선 + 시나리오/문서 그룹) + 보편 뒤로가기

> 대상 실행자: Codex (자동 코딩 에이전트)
> 전제: 직전 작업지시서 `docs/WORKORDER-search-display-and-back-navigation.md`의 **Part A(검색 노출 정책) / Part B(시나리오 노드 한정 뒤로가기)는 이미 구현 완료**되어 있다. 본 문서는 그 위에 4개 과제를 얹는다.
> 본 문서의 파일:라인은 조사 시점(2026-06-18) 기준 — **착수 전 실제 파일을 열어 재확인**하라.
> 모든 변경은 기존 흐름·테스트를 깨지 않아야 한다. 마이그레이션은 가산적으로만. 커밋/푸시 금지.

---

## 0. 한 줄 목표 / 배경

실DB 검색 테스트("전력")에서 드러난 4가지 문제를 해결한다.

1. **라벨이 메뉴 breadcrumb으로 노출** — 크롤 문서 라벨이 페이지 `<title>`("탄소중립 | 환경E | ESG경영 | 한국전력공사")을 그대로 써서 선택지가 메뉴구조처럼 보임.
2. **적합도 홍수** — "전력"처럼 코퍼스 대부분에 존재하는 흔한 단어가 거의 모든 크롤 문서에 매칭되어 무의미한 결과가 쏟아짐(상대 임계값만으론 못 막음).
3. **시나리오/크롤문서 혼재** — 큐레이션된 시나리오와 원문 문서가 한 목록에 구분 없이 섞여 혼란.
4. **검색 경로 뒤로가기 부재** — 뒤로가기가 "같은 시나리오 노드 내부"로만 작동. 검색 세션, 그리고 검색결과 클릭→시나리오 전환 직후에는 되돌아갈 길이 없음.

### 현재 구현 상태(재사용/수정 대상)
- 설정 `ChatSearchProperties`(`chat.search.*`): `poolLimit=10, displayMax=5, relativeThreshold=0.5, moreEnabled=true` — [ChatSearchProperties.java](../src/main/java/kr/co/cleverchat/domain/chatbot/config/ChatSearchProperties.java).
- 노출 선별 `selectForDisplay(results)` → `SearchDisplaySelection(shown, hidden)` — [ChatRuntimeService.java:955](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L955).
- 검색옵션 저장 `insertSearchOptionsMessage`/`insertSearchOptionsPayloadMessage`: payload에 `searchOptions`+`overflowOptions`+`more` — [:903,917](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L903). 옵션 맵 생성 `searchOptionToMap` [:933], 라벨 `searchOptionLabel` [:1060].
- "더 보기" `searchMore(...)` + `latestBotOverflowOptions` — [:444,834](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L444). 컨트롤러 `POST /sessions/{id}/search-more`.
- 뒤로가기 `goBack(...)` [:476], `canGoBack(...)` [:773], `previousNodeNo(...)`(메시지 로그로 노드 스택 재구성, NODE_BACK 마커 인식) [:857]. 컨트롤러 `POST /sessions/{id}/back`. 감사 `insertNodeBackEvent`(event_type='NODE_BACK').
- 시나리오 전환 `switchScenarioInCurrentSession` [:522]: `updateScenarioContext(... 'SCENARIO' ...)` + `insertScenarioSwitchEvent`(event_type='SCENARIO_SWITCH', from/to scenario, trigger_message). 검색결과 클릭 진입점 `selectSearchResult`.
- 검색 SQL `crawl_document_matches`: 라벨=`coalesce(d.title, d.url)`, OR 매칭(`tsq OR token_tsq OR title LIKE ...`), `content_tokens` ts_rank*3 가중 — [SearchMapper.xml:159-198](../src/main/resources/mapper/search/SearchMapper.xml#L159). 최종 `ORDER BY score DESC, field_priority, matched_field LIMIT #{limit}` [:231]. `SearchResultItem.matchedField` ∈ {SCENARIO, KEYWORD, NODE, CRAWL_DOCUMENT}, `score` 보유.
- DTO `SearchOptionResponse(crawlDocumentNo, scenarioNo, label, matchedField)` — [ChatRuntimeDtos.java:49](../src/main/java/kr/co/cleverchat/domain/chatbot/dto/ChatRuntimeDtos.java#L49). `SessionResponse`에 `searchMoreCount, canGoBack` 포함.
- 크롤 제목 출처 = 페이지 `<title>`: `CrawlJobRunner.java:275` `document.setTitle(page.title())`, `CrawlService.java:837`.
- 프론트 `chat.js`: `backButton`/`searchMoreButton`/`inlineSessionActions`/`renderMessages` — [chat.js:246-360](../src/main/resources/static/asset/chat/chat.js#L246).
- 마이그레이션 최고 = **V34**(`ck_chat_session_event_type CHECK IN ('SCENARIO_SWITCH','NODE_BACK')`). 신규 = **V35**.

> ⚠️ 착수 전: 위 메서드/라인/마이그레이션 번호를 직접 재확인하라.

---

## 과제 1 — 크롤 문서 라벨 정제

### 목표 동작
- 크롤 문서(`matchedField == 'CRAWL_DOCUMENT'`) 라벨에서 **breadcrumb을 정제해 가장 구체적인 한 조각만** 노출.
  - 예: `"탄소중립 | 환경E | ESG경영 | 한국전력공사"` → `"탄소중립"`. `"한국전력공사 | 경영연구원"` → `"경영연구원"`(아래 규칙 참조).
- 시나리오/키워드/노드 결과 라벨은 **현행 유지**(큐레이션된 제목).

### 구현
- 신설 헬퍼 `String normalizeCrawlLabel(String rawTitle)` (in `ChatRuntimeService` 또는 작은 유틸 클래스):
  1. 구분자 분할: `|`, `>`, `·`, ` - `(공백-하이픈-공백), `/` 등(설정 가능하게). 빈 토큰 제거.
  2. **사이트명 꼬리 제거**: 설정 목록(`chat.search.label.site-suffixes`, 기본 `["한국전력공사","KEPCO"]`)에 속하는 세그먼트 제거.
  3. 남은 세그먼트 중 **가장 구체적인 것 선택**. KEPCO `<title>`은 "구체 → 상위 → 사이트명" 순서가 일반적이므로 **첫 세그먼트 우선**. 단, 첫 세그먼트가 사이트명이면 다음 것. (규칙은 설정 `label.prefer = FIRST|LAST`로 토글, 기본 FIRST.)
  4. 모두 제거되어 비면 원본 title 또는 기존 fallback(`크롤 문서 #id`) 유지.
- `searchOptionToMap`([:933])에서 `matchedField=='CRAWL_DOCUMENT'`일 때 `searchOptionLabel` 대신 정제 라벨 사용. 비크롤은 그대로.
- **중복 라벨 완화**(선택): 같은 메시지 내 정제 후 동일 라벨이 2개+면, 뒤쪽에 다음 상위 세그먼트나 snippet 앞부분을 괄호로 덧붙임. (v1 필수 아님. 우선 단순 정제만.)
- 설정: `ChatSearchProperties`에 중첩 `Label`(delimiters, siteSuffixes, prefer) 추가 또는 별도 `chat.search.label.*`. 하드코딩 금지, 기본값 위와 같이.

### 주의
- 정제는 **표시 라벨에만** 적용. 검색 매칭/score/SQL은 건드리지 않는다.
- `overflowOptions`로 저장돼 "더 보기"로 재노출되는 항목도 동일 정제가 적용돼야 함 → `searchOptionToMap` 한 곳에서 처리하면 자동 충족(저장 시점 정제). `searchOptionNodeToMap`([:944])은 이미 저장된 라벨을 그대로 읽으므로, **저장 시점에 정제된 라벨이 들어가면 됨**(이중 정제 불필요).

---

## 과제 2 — 검색 적합도 개선 (흔한 단어 홍수 차단)

> 방향(합의됨): **제목/다중텀 우선 + 흔한 단일토큰 페널티 + 절대 점수 하한**. SQL 대수술보다 **앱 측 정형화 + 설정화**를 우선(안전·튜닝 용이). SQL 가중 조정은 보조.

### 2-1. 앱 측 노출 정형화 — `selectForDisplay` 확장
`ChatSearchProperties`에 추가:
- `minScore`(double, 기본 0.0 → 튜닝값 권장 예: 1.0) — **절대 점수 하한**. 이 미만은 노출 후보에서 제외.
- `maxCrawlDocuments`(int, 기본 3) — 한 응답에 노출할 **크롤 문서 최대 수**(시나리오는 별도). 노이즈 억제.

`selectForDisplay`(또는 그 직전 단계) 로직 보강(순수 함수 유지, 단위테스트 필수):
1. **절대 하한 컷**: `score < minScore` 후보 제거. 단 결과가 전부 잘리면 **최상위 1건은 보존**(기존 "최소 1 보장" 정신 유지).
2. **상대 임계값 컷**: 기존 `relativeThreshold` 유지.
3. **크롤 문서 캡**: `matchedField=='CRAWL_DOCUMENT'` 후보는 점수순 상위 `maxCrawlDocuments`개까지만 통과. 초과분은 `hidden`(overflow)로.
4. 그 후 `displayMax` 상한 적용 → `shown`/`hidden` 분리(기존 인터페이스 유지).
- 잘려나간 후보는 기존대로 `overflowOptions`에 저장돼 "더 보기"로 접근 가능(완전 폐기 아님).

### 2-2. SQL 가중 조정 (보조, 신중히) — `SearchMapper.xml`
- `crawl_document_matches`에서 **`content_tokens` ts_rank 가중(현재 *3)을 낮추고**(예: *1.5) **제목/다중텀 커버리지 가중을 상대적으로 높임**([:174-182](../src/main/resources/mapper/search/SearchMapper.xml#L174)). 흔한 단어가 본문에만 있을 때 점수가 과대평가되는 걸 완화.
- **변경은 가역적·보수적으로**. 기존 검색 단위테스트(`SearchServiceTest` 등)와 P2 형태소 검색 회귀가 없어야 함. 자신 없으면 2-2는 생략하고 2-1만으로 1차 배포 후 데이터 보며 튜닝(로그의 pool/shown 활용).
- 절대 하한(minScore)을 SQL에 넣지 말 것(앱 측 처리; score 스케일이 가중 변경에 민감).

### 관측
- 기존 `log.info("chat search display query=... pool=... shown=... hidden=...")`([:334])에 `crawlShown`, `cutByMinScore` 등 보강(선택). 운영자가 minScore/maxCrawlDocuments 튜닝 근거로 사용.

### 기본값 결정
- `minScore`는 실데이터 score 분포를 모르면 **0.0(무효)으로 시작**하고, 기동 후 로그로 분포 확인 → 운영자가 yml로 상향. (작업지시서엔 0.0 기본 + 주석으로 "튜닝 대상" 명시.) `maxCrawlDocuments=3`, 나머지 기존 유지.

---

## 과제 3 — 시나리오 / 문서 그룹·정렬

### 목표 동작
- 검색 선택지를 **두 묶음으로 구분 노출**: 위에 **"상담 주제"(시나리오류)**, 아래 **"관련 문서"(크롤 문서)**. 각 묶음에 작은 헤딩.
- 동점/정렬: 시나리오류를 항상 문서류보다 위에. 묶음 내부는 score 순.

### 구현 (백엔드)
- `SearchOptionResponse`에 **`String optionType`** 추가(`"SCENARIO"` | `"DOCUMENT"`). 파생 규칙: `matchedField=='CRAWL_DOCUMENT'` → `DOCUMENT`, 그 외(SCENARIO/KEYWORD/NODE) → `SCENARIO`.
  - ⚠️ record 인자 추가 → `extractSearchOptions`([:744 부근])의 생성자 호출, 관련 테스트 정합 수정.
- 저장 payload 옵션 맵(`searchOptionToMap`)에 `optionType` 키 추가, `searchOptionNodeToMap`에서 읽어 복원("더 보기" 일관).
- 노출 정렬: `selectForDisplay` 결과(또는 메시지 생성 직전)에서 **시나리오류 우선 → 문서류** 순으로 재정렬(각 그룹 내 score 유지). 크롤 캡(과제2)은 문서 그룹에만 적용.

### 구현 (프론트 `chat.js`)
- `inlineSearchOptions`(현재 단일 묶음)를 **optionType별 2 그룹 렌더**로 변경: `optionType=='SCENARIO'`들 위 그룹("상담 주제" 헤딩), `DOCUMENT`들 아래 그룹("관련 문서" 헤딩). 빈 그룹은 헤딩 미표시.
- 기존 `searchOptionButton` 재사용. 헤딩은 작은 텍스트 라벨(접근성 위해 `role`/`aria-label` 유지).

---

## 과제 4 — 보편 뒤로가기 (검색 세션 + 전환 직후 포함)

> 현재 `goBack`은 "같은 시나리오 노드 스택"만 처리. 아래 두 경로를 추가해 **어디서든 직전 상태로 복귀** 가능하게 한다.

### 4-A. 마이그레이션 `V35__chat_search_back_event.sql` (가산적)
```sql
ALTER TABLE tb_chat_session_event DROP CONSTRAINT ck_chat_session_event_type;
ALTER TABLE tb_chat_session_event ADD CONSTRAINT ck_chat_session_event_type
    CHECK (event_type IN ('SCENARIO_SWITCH','NODE_BACK','SEARCH_BACK'));
```

### 4-B. 되돌리기 우선순위 (goBack 일반화)
`goBack`을 다음 순서로 분기(첫 번째 매칭 적용):
1. **시나리오 노드 백(기존)**: 시나리오 세션이고 `previousNodeNo` 존재 → 현행 로직 그대로.
2. **전환 직후 → 검색결과 복귀(신규)**: 시나리오 세션이지만 직전 노드가 없고(=전환 직후 시작 노드), 이 세션이 **검색결과 클릭으로 진입**(가장 최근 `SCENARIO_SWITCH` 이벤트 존재 + 그 직전 BOT 메시지에 `searchOptions` payload 존재) → **검색 세션 상태로 복원**:
   - `currentNodeNo=null, scenarioNo=null, versionNo=null, sessionType='SEARCH', state='ACTIVE'`로 세션 갱신(신규 매퍼 `restoreSearchSession(sid, expiresAt)` 또는 `updateScenarioContext`에 null 허용 경로).
   - 전환 직전 검색옵션 메시지의 payload(`searchOptions`+`overflowOptions`)를 **복사해 새 BOT 메시지로 재노출**(anti-IDOR: 반드시 새 최신 payload에 기록). 라벨/그룹/더보기 그대로 복원.
   - 감사: `SEARCH_BACK` 이벤트(from_scenario_no=떠난 시나리오, to=null, detail에 복원 출처 메시지no).
3. **검색 세션 백(신규)**: 검색 세션(`scenarioNo==null`)이고 메시지 로그에 **직전 검색결과 메시지**(현재 최신 검색옵션 메시지보다 이전의 `searchOptions` 보유 BOT 메시지)가 존재 → 그 payload를 복사해 새 BOT 메시지로 재노출(위와 동일 방식). 감사: `SEARCH_BACK`.
4. 그 외 → 400 "이전 단계가 없습니다."

> 핵심 불변식: 어떤 경우든 **기존 메시지를 삭제/변조하지 않고**, 직전 상태를 **새 메시지로 재구성**(append-only)한다. 재노출되는 검색옵션은 새 최신 BOT payload에 들어가야 클릭 가능(`findOfferedSearchOption`/`latestBotOverflowOptions` 정합).

### 4-C. `canGoBack` 일반화
`canGoBack`이 위 1·2·3 중 하나라도 가능하면 true:
- 시나리오 세션: `previousNodeNo` 존재 **또는** (직전 노드 없음 + 최근 SCENARIO_SWITCH로 진입 + 전환 직전 검색옵션 존재).
- 검색 세션: 직전 검색결과 메시지 존재.
- 상태 ACTIVE/COMPLETED 한정(기존).

### 4-D. 헬퍼 (메시지 로그 기반, 추가 쿼리 최소화)
- `Optional<JsonNode> previousSearchOptionsBeforeLatest(messages)`: 최신 검색옵션 메시지를 제외하고 그 이전의 `searchOptions` 보유 BOT 메시지 payload 반환(검색 세션 백용).
- `Optional<JsonNode> searchOptionsBeforeLastSwitch(messages, events?)`: 가장 최근 SCENARIO_SWITCH의 `trigger_message_no`/시점 이전, `searchOptions` 보유 BOT 메시지 payload 반환(전환 직후 복귀용). 이벤트 조회가 필요하면 `ChatSessionMapper`에 `findLatestScenarioSwitch(sid)` 추가, 또는 메시지 payload의 전환 흔적으로 판별.
- 세션 복원 매퍼: `restoreSearchSession(sid, expiresAt)`(scenario/version/node=null, type=SEARCH, state=ACTIVE) — `ChatSessionMapper.xml`.
- 검색옵션 재노출: 복사 payload로 `insertSearchOptionsPayloadMessage` 재사용(라벨 이미 정제 저장됨).

### 4-E. 프론트
- 이미 `canGoBack`이면 "이전" 버튼 렌더([chat.js:284](../src/main/resources/static/asset/chat/chat.js#L284)). **추가 작업 거의 없음** — 백엔드가 `canGoBack=true`를 검색/전환 상황에서도 내려주면 버튼이 자동 노출되고 `/back` 호출이 동일하게 동작. 검색 세션에서도 "이전"이 `inlineSessionActions`에 뜨도록 조건 확인(현재 searchOptions 있을 때 액션 렌더되는 경로 점검).

---

## 5. 테스트 요구사항 (Mockito 스타일, `src/test/.../chatbot/`)

### 과제1
1. `normalizeCrawlLabel`: `"A | B | 한국전력공사"`→`"A"`, `"한국전력공사 | 경영연구원"`→`"경영연구원"`, 사이트명만 있으면 fallback, 빈/널 안전.
2. `searchOptionToMap`이 CRAWL_DOCUMENT에만 정제 적용, 시나리오 라벨 불변.

### 과제2
3. `selectForDisplay`: minScore 미만 컷 + 최상위 1 보존, maxCrawlDocuments 캡(크롤만), displayMax 상한, hidden 정확.
4. score 전부 동일/0 → 안전(division/empty 없음).
5. (2-2 적용 시) `SearchServiceTest`/형태소 검색 회귀 없음.

### 과제3
6. `optionType` 파생(CRAWL_DOCUMENT→DOCUMENT, 그 외→SCENARIO), 시나리오 우선 정렬, 그룹 분리.
7. `SearchOptionResponse` 인자 추가에 따른 호출부/`ChatRuntimeApiControllerTest` 정합.

### 과제4
8. 시나리오 노드 백(기존) 유지.
9. 검색결과 클릭→전환 직후 `goBack` → 검색 세션 복원 + 검색옵션 재노출(새 payload에 기록, 기존 메시지 미삭제), `SEARCH_BACK` 이벤트.
10. 검색 세션에서 2회 질의 후 `goBack` → 직전 검색결과 재노출.
11. `canGoBack`이 검색 세션/전환 직후에 true.
12. 되돌릴 단계 전무 → 400.

- **기존 테스트 전부 통과 유지**(특히 검색 폴백/AI/노드 백 관련, `SessionResponse`/`SearchOptionResponse` 생성자 인자 정합).

---

## 6. 빌드 & 검증
- maven 호출 전 PowerShell: `$env:MAVEN_OPTS="-Xshare:off"`
- 포맷: `.\mvnw.cmd -q spotless:apply`
- 테스트: `.\mvnw.cmd "-Dspotless.check.skip=true" test` (사전 존재 `AccessibilityTemplateTest` 외 신규 실패 0)
- 실DB 기동(한글 경로 → fat jar): `.\mvnw.cmd "-DskipTests" "-Dspotless.check.skip=true" package` → `java -jar target\cleverchat.jar` (dev.c2r.co.kr, Flyway V35 적용)
- 수동 시나리오("전력" 재현):
  - 과제1: 크롤 결과 라벨이 breadcrumb이 아닌 단일 의미 조각으로 노출.
  - 과제2: 무의미한 크롤 결과가 줄고(크롤 캡), 로그 pool/shown 확인.
  - 과제3: "상담 주제"/"관련 문서" 두 그룹으로 분리.
  - 과제4: 검색→문서/시나리오 진입 후 "이전" 노출·클릭 → 검색결과 목록 복귀. 검색 세션 추가질의 후 "이전"으로 직전 결과 복귀.

---

## 7. 완료 기준 (DoD)
- [ ] 크롤 라벨 breadcrumb 정제(설정화), 시나리오 라벨 불변.
- [ ] `selectForDisplay`에 minScore 절대하한(+최상위 보존) & maxCrawlDocuments 캡, 설정화. (선택) SQL 가중 보수 조정 + 회귀 없음.
- [ ] `optionType` 도입, 시나리오/문서 2그룹 정렬·렌더, DTO 인자 정합.
- [ ] V35로 `SEARCH_BACK` 허용. `goBack`/`canGoBack` 일반화(노드백/전환직후 복귀/검색세션백), append-only·anti-IDOR 준수, 감사 이벤트 기록.
- [ ] 프론트 2그룹 렌더 + 검색/전환 상황에서 "이전" 버튼 노출·동작.
- [ ] 신규+기존 테스트 통과, spotless 적용, AccessibilityTemplateTest 외 신규 실패 0.
- [ ] (권장) fat jar 실DB로 "전력" 시나리오 수동 검증.

---

## 8. 하지 말 것 / 주의
- 메시지 로그 **삭제/변조 금지**(되돌리기·재노출은 새 메시지 append). 마이그레이션 **가산적**만.
- 라벨 정제는 **표시 전용** — 검색 매칭/score/SQL 셀렉션 로직을 라벨 때문에 바꾸지 말 것.
- 적합도 SQL 가중(2-2)은 자신 없으면 **생략**하고 앱 측(2-1)만. 변경 시 형태소 검색 회귀 필수 확인.
- 재노출되는 검색옵션은 **새 최신 BOT payload(searchOptions/overflowOptions)** 에 반드시 기록(anti-IDOR `findOfferedSearchOption`/`latestBotOverflowOptions` 정합).
- `SearchOptionResponse`/`SessionResponse` record 인자 변경 시 **모든 호출부·테스트 동시 수정**.
- 보편 뒤로가기 복원 시 `session_type`/`scenario_no`/`current_node_no` 정합(검색 복귀=전부 null + SEARCH). 잘못 두면 이후 freeText 분기가 깨짐.
- 커밋/푸시는 **사용자 지시가 있을 때만**.

---

## 9. 결정 사항 (확정)
- 범위 = **A안(과제1·2·3·4 전부)**.
- 적합도 = **앱 측 minScore 절대하한 + maxCrawlDocuments 캡 우선**, SQL 가중조정은 보조/선택.
- minScore 기본 0.0(튜닝 대상), maxCrawlDocuments=3, 라벨 prefer=FIRST·site-suffix=["한국전력공사","KEPCO"].
- 그룹 = "상담 주제" / "관련 문서" 2그룹.
- 뒤로가기 = 노드백 + 전환직후 검색복귀 + 검색세션백, 감사 SEARCH_BACK.
