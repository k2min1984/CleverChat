# 작업지시서 — 채팅 세션과 시나리오 분리(b안): 시나리오 없는 "검색 세션" 지원

> 대상 실행자: Codex (자동 코딩 에이전트)
> 전제: 아래 "이미 완료된 작업(P1~P3)"은 현재 코드에 반영돼 있음. 이 지시서는 그 위에 **b안(세션을 시나리오에서 분리)**을 추가하는 것.
> 모든 변경은 기존 시나리오-결합 흐름과 기존 테스트를 **깨지 않아야** 한다.

---

## 0. 한 줄 목표
`tb_chat_session`이 시나리오에 강결합(`scenario_no NOT NULL`)되어 있어, /chat 첫 입력이 어떤 시나리오에도 매칭되지 않으면 세션을 못 만들고 에러가 난다.
**시나리오에 묶이지 않은 "검색 세션"을 허용**하여, 첫 입력부터 검색 결과(크롤 게시물 스니펫/링크 + 다중결과 선택지 버튼)를 대화로 제공한다.

---

## 1. 배경 & 현재 상태

### 1.1 이미 완료된 작업 (수정 금지, 재사용 대상)
- **P1 형태소 색인**: `tb_crawl_document.content_tokens`(Nori 형태소) + GIN 인덱스(`V28`). 크롤 저장 시 토큰화, 기동 시 백필(`CrawlTokenBackfillRunner`).
- **P2 형태소 검색**: `SearchService.tokenize()`가 형태소 분석, `SearchMapper.searchScenarios(query, terms, tokenQuery, limit)`가 `content_tokens`를 형태소 tsquery로 매칭.
- **P3 챗봇 검색 노출(in-session 한정)**:
  - `ChatRuntimeDtos`: `SearchOptionResponse(crawlDocumentNo, scenarioNo, label, matchedField)`, `SessionResponse.searchOptions`, `SelectSearchResultRequest(crawlDocumentNo, scenarioNo)`.
  - `ChatRuntimeService.freeText`의 no-match 폴백: 검색 결과 **2건+ → 가이드 BOT 메시지 + searchOptions(직전 BOT 메시지 payload JSON에 저장)**, **단일 크롤문서 → 스니펫+원문링크**, 단일 시나리오/AI는 기존 보존.
  - `selectSearchResult(sessionId, crawlDocumentNo, scenarioNo)`: 직전 BOT 메시지 payload의 제시된 옵션만 선택 가능(anti-IDOR), 크롤→스니펫+링크, 시나리오→`start()`로 새 시나리오 세션.
  - `response()`가 최신 BOT 메시지 payload에서 searchOptions 추출(state=ACTIVE일 때만). 노드 없는 BOT 메시지는 payload의 `links` 배열을 파싱해 렌더.
  - 컨트롤러 `POST /chat/api/sessions/{id}/select-search-result`.
  - 프론트 `static/asset/chat/chat.js`: `inlineSearchOptions` 버튼 렌더 + `selectSearchResult` 핸들러.

### 1.2 핵심 제약(이번 작업이 푸는 것)
- `tb_chat_session.scenario_no`, `version_no`가 **NOT NULL** (원본 `V4__chat_runtime_baseline.sql`의 `scenario_id`, `version_id`; `V20` 명명표준에서 `*_no`로 리네임됨). `current_node_no`는 nullable.
- `startWithText(text, context)`(=`POST /chat/api/sessions/auto`)는 시나리오 매칭(제목→키워드→검색) 실패 시 `BusinessException(VALIDATION_ERROR, "질문과 맞는 상담 주제를 찾지 못했습니다...")`를 던진다. → **여기가 첫 입력 검색이 막히는 지점.**
- 프론트 `chat.js`: 세션 없을 때 첫 입력은 `/sessions/auto`(autoStart), 세션 있을 때는 `/free-text`.

### 1.3 관련 참고 API/모델 (구현 시 사용)
- `SearchService.search(query, source, limit, userId, anonymousIdHash)` → `SearchResponse(normalizedQuery, resultCount, results)`.
- `SearchResultItem`: `scenarioNo, scenarioTitle, crawlDocumentNo, crawlUrl, matchedField('SCENARIO'|'KEYWORD'|'NODE'|'CRAWL_DOCUMENT'), score, snippet`.
- `ChatSession` 모델 필드: `chatSessionNo(String UUID), anonymousId, userNo?, scenarioNo, versionNo, currentNodeNo, state, expiresAt, ipHash, userAgentHash`.

> ⚠️ **착수 전 필수 확인**: `ChatSession.java`, `mapper/chatbot/ChatSessionMapper.xml`, 마이그레이션 체인(`V4`, `V20`/`V21` 등)을 직접 읽어 **현재 실제 컬럼명/제약을 재확인**하라. 이 문서의 컬럼명은 조사 시점 기준이다.

---

## 2. 최종 동작 정의 (수용 기준 행동)

1. **첫 입력에 시나리오가 매칭되면** → 기존과 100% 동일(해당 시나리오 세션 시작 후 freeText).
2. **첫 입력에 시나리오가 안 맞으면** → 에러 대신 **검색 세션(scenario_no=null)**을 열고 검색을 수행하여:
   - 0건: "질문에 맞는 답변을 찾지 못했습니다." 류 안내.
   - 1건·크롤문서: 스니펫 + "원문 보기" 링크.
   - 1건·시나리오: "관련 상담: {제목}" (AI 켜져 있으면 AI 답변).
   - 2건+: "관련 자료를 찾았어요. 아래에서 선택해 주세요." + 제목 선택지 버튼.
3. **검색 세션 안에서 추가 입력(freeText)** → 시나리오 매칭을 건너뛰고 바로 다시 검색.
4. **검색 세션에서 시나리오 결과 버튼 클릭** → 그 시나리오로 **새 시나리오 세션 전환**(기존 `selectSearchResult` 동작).
5. **검색 세션에서 크롤문서 버튼 클릭** → 현재 검색 세션에 스니펫+링크 답변 추가.
6. **이력(history)**: 검색 세션도 목록에 남되 제목은 "검색"(또는 유사) 라벨로 노출(시나리오 제목이 null이므로).
7. 기존 시나리오-결합 세션의 모든 동작과 기존 단위테스트는 **불변**.

---

## 3. 변경 범위 — 파일별 상세

### 3.1 DB 마이그레이션 — 새 파일 `V29__chat_session_search_decoupling.sql`
> 현재 최고 버전이 V28이므로 V29. 착수 시 `db/migration` 최고 번호 재확인 후 +1.

목표 최종 상태:
```sql
-- 시나리오 비결합(검색) 세션 허용
ALTER TABLE tb_chat_session ALTER COLUMN scenario_no DROP NOT NULL;
ALTER TABLE tb_chat_session ALTER COLUMN version_no  DROP NOT NULL;
-- current_node_no 는 이미 nullable (확인만)

-- 세션 종류 구분(가독성/쿼리용). 기존 행은 전부 SCENARIO.
ALTER TABLE tb_chat_session ADD COLUMN session_type varchar(20) NOT NULL DEFAULT 'SCENARIO';
ALTER TABLE tb_chat_session ADD CONSTRAINT ck_chat_session_type
    CHECK (session_type IN ('SCENARIO','SEARCH'));
```
주의:
- FK(`fk_chat_session_scenario`, `fk_chat_session_version`)는 컬럼이 nullable이면 NULL을 허용하므로 그대로 둔다(드롭 금지).
- `ck_chat_session_state` 상태값에 **새 상태를 추가하지 않는다**(검색 세션도 `ACTIVE`/`EXPIRED`/`ABANDONED` 재사용). 상태 CHECK 변경 불필요.
- Flyway: 되돌릴 수 없는 운영 DB(dev.c2r.co.kr)에 기동 시 적용된다. 마이그레이션은 가산적이어야 한다(위 ALTER만).

### 3.2 모델/매퍼 — `ChatSession` & `ChatSessionMapper`
- `ChatSession`에 `sessionType`(String) 필드 + getter/setter 추가.
- `ChatSessionMapper.xml`:
  - `insert`: `session_type` 컬럼/값 추가. `scenario_no, version_no, current_node_no`가 NULL이어도 INSERT 되도록(파라미터 그대로 바인딩) 확인.
  - 조회 SELECT(`findById`, 컬럼 목록): `session_type` 포함.
  - **이력/리스트 쿼리**: 현재 `JOIN tb_scenario sc ON sc.scenario_no = s.scenario_no` (INNER JOIN) → **`LEFT JOIN`**으로 변경(검색 세션은 scenario_no NULL이라 INNER JOIN 시 누락됨). 제목 표기는 `coalesce(sc.title, '검색')` 류로 null 안전 처리. `GROUP BY`에 사용된 `sc.title`도 LEFT JOIN과 정합 유지.
  - `ChatSessionListItem.scenarioTitle`이 null일 수 있으니 매핑/이후 처리 null 안전.
- 인터페이스(`ChatSessionMapper.java`)에 새 시그니처가 필요하면 추가(기본은 기존 insert 재사용).

### 3.3 핵심 — `ChatRuntimeService`
파일: `domain/chatbot/service/ChatRuntimeService.java`

#### (a) 검색 폴백 로직을 메서드로 추출
현재 `freeText`의 `if (match.isEmpty()) { ... 검색 폴백 ... }` 블록(P3에서 작성: 2건+ 버튼 / 단일 크롤 스니펫+링크 / 단일 시나리오·AI / 0건 NO_MATCH)을 **private 메서드로 추출**:
```java
private SessionResponse respondWithSearch(
        String sid, int botSeq, String safeText, ChatSession session,
        ChatRequestContext context, Integer latencyMs)
```
- 내부는 기존 P3 분기 로직 그대로(검색 limit = `SEARCH_FALLBACK_LIMIT`, `insertSearchOptionsMessage` / `insertCrawlAnswer` / AI·관련상담 / NO_MATCH 실패기록).
- `freeText`의 no-match 분기는 이 메서드 호출로 대체(동작 동일 → 기존 테스트 유지).

#### (b) 검색 세션 생성 메서드 신설
```java
@Transactional
public SessionResponse startSearch(String text, ChatRequestContext context)
```
- `safeChatText(text)`로 PII 처리.
- `ChatSession` 생성: `scenarioNo=null, versionNo=null, currentNodeNo=null, state="ACTIVE", sessionType="SEARCH"`, anonymousId/expiresAt/ipHash/userAgentHash 세팅 → `sessionMapper.insert`.
- 사용자 메시지 insert(seq=1, nodeId=null, optionId=null, payload="{}").
- `respondWithSearch(sid, 2, safeText, session, context, latencyMs)` 호출하여 첫 검색 응답 생성 후 반환.

#### (c) `startWithText` 변경 — 에러 대신 검색 세션
```java
// 기존: ...searchMatchedScenarioId(...).orElseThrow(VALIDATION_ERROR);
// 변경: 시나리오 매칭(제목→키워드→시나리오검색) 실패 시 startSearch(safeText, context) 반환
```
- 시나리오가 매칭되면 기존대로 `start(scenarioId)` → `freeText(...)` (불변).
- 매칭 실패 시 `return startSearch(safeText, context);`

#### (d) `freeText` 변경 — null 시나리오 분기
- 진입부에서 `session.getScenarioNo() == null` 이면(검색 세션):
  - `matchingService.match(...)` **호출하지 않음**(시나리오/노드가 없으므로).
  - 사용자 메시지 insert 후 `return respondWithSearch(sid, seq+1, safeText, session, context, latencyMs);`
- `scenarioNo != null`이면 기존 흐름(매칭→advance/검색폴백) 그대로.

#### (e) `response()` — null 노드 가드
- 노드 옵션 조회는 `state=="ACTIVE" && currentNodeNo != null`일 때만 `optionMapper.findEnabledByNodeId(currentNodeNo)`. `currentNodeNo == null`이면 `options=List.of()`.
- `searchOptions` 추출 로직(P3)은 그대로.
- 반환 `SessionResponse`의 `scenarioId/versionId/currentNodeId`는 null 가능(record는 Long이라 OK).

#### (f) `selectSearchResult` — 검토만
- 이미 직전 BOT payload의 옵션만 선택 가능하고, 크롤→현재세션 답변, 시나리오→`start()` 새 세션. 검색 세션(null scenario)에서도 동작해야 함(현재 세션이 ACTIVE면 OK). 추가 변경 불필요하나, 시나리오 클릭 후 검색 세션을 `ABANDONED`로 마킹하고 싶으면 선택적으로 처리(필수 아님).

#### (g) `historyList` / `toHistorySessionResponse`
- scenario 제목 null 처리: 검색 세션은 "검색" 라벨. (매퍼 LEFT JOIN + coalesce와 정합.)

### 3.4 컨트롤러 / DTO
- `ChatRuntimeApiController`: 별도 신규 엔드포인트 불필요(`/sessions/auto`가 `startWithText`를 통해 검색 세션까지 처리). 다만 의미를 명확히 하려면 `startWithText`가 검색 세션을 반환할 수 있음을 주석으로 남길 것.
- DTO: 추가 불필요(`SessionResponse.searchOptions`/`scenarioId` nullable 이미 존재).

### 3.5 프론트엔드 `static/asset/chat/chat.js`
- `sendFreeText`의 autoStart 콜백: 반환 세션의 `scenarioId`가 **null일 수 있음**. 다음을 null 안전 처리:
  - `markScenarioActive(session.scenarioId)` → null이면 아무 시나리오도 활성표시 안 함(가드 추가).
  - `selectedScenarioName.textContent` → scenarioId null이면 "검색 결과"(또는 빈값)로.
- `renderSession`은 이미 `session.searchOptions`를 렌더(P3). 추가 변경 최소.
- 검색 세션에서도 입력창은 활성 유지되어 추가 freeText가 같은 세션으로 가야 함(현재 `state.sessionId` 세팅되므로 자동 충족).

---

## 4. 엣지 케이스 & 불변식
- 검색 세션도 소유자 검증(`anonymous_id`), 만료(`expires_at`), 레이트리밋(anonymousId+sessionNo)은 기존과 동일하게 적용된다(시나리오 비의존이라 그대로 동작). 확인할 것.
- `advance()`, `selectOption()`은 시나리오 세션 전용 — 검색 세션에서는 호출 경로가 없어야 한다(노드/옵션이 없으므로). `selectOption`이 검색 세션에서 호출되면 기존 "선택할 수 없는 옵션" 에러로 자연 차단됨(확인).
- `searchMatchedScenarioId`(startWithText 내 시나리오 후보 탐색)는 그대로 두되, 그 결과가 없을 때만 검색 세션으로 분기.
- 검색 세션은 시나리오 END가 없으므로 `COMPLETED`가 되지 않는다 — `ACTIVE`로 유지되다 만료. 정상.

---

## 5. 테스트 요구사항 (`src/test/java/.../chatbot/...`)
기존 `ChatRuntimeServiceTest` 스타일(Mockito) 유지. **추가**:
1. `startWithText` 시나리오 매칭 실패 → `startSearch` 경로로 검색 세션 생성, 검색결과 기반 응답(버튼/스니펫) 검증. (throw 하지 않음)
2. 검색 세션(`scenarioNo=null`)에서 `freeText` → `matchingService.match` **미호출** + 검색 응답.
3. `startSearch` 단독: scenario_no/version_no/current_node_no = null 로 세션 insert 되는지(ArgumentCaptor) + 첫 검색 응답.
4. `response()`에서 currentNodeNo=null일 때 `optionMapper.findEnabledByNodeId` 미호출/옵션 빈 배열.
5. 이력: 검색 세션 제목 null → "검색" 라벨 매핑.
- **기존 테스트 전부 통과 유지**(특히 `freeTextNoMatchUsesSearchFallbackWhenAvailable`, `freeTextNoMatchUsesAiSuggestionWhenAvailable`, `startWithTextMatchesScenarioAndSendsInitialQuestion`).
- `ChatRuntimeApiControllerTest`의 `SessionResponse` 생성자 인자 수 정합 유지.

---

## 6. 빌드 & 검증 방법
- 이 머신은 **컴파일 JVM의 CDS 손상으로 `EXCEPTION_ACCESS_VIOLATION`** 가 발생함. 모든 maven 호출 전에 환경변수 설정:
  - PowerShell: `$env:MAVEN_OPTS="-Xshare:off"`
- 포맷터(spotless, google-java-format AOSP) 필수: 변경 후 `.\mvnw.cmd -q spotless:apply`.
- 단위테스트: `.\mvnw.cmd "-Dspotless.check.skip=true" test`
  - 사전 존재 실패 `AccessibilityTemplateTest`(3건, sub.css 관련)은 본 작업과 무관 — 신규 실패가 없어야 한다.
- 실DB 기동 검증(권장): `spring-boot:run`은 한글 경로 때문에 포크 classpath가 깨짐 → **fat jar로 기동**:
  - `.\mvnw.cmd "-DskipTests" "-Dspotless.check.skip=true" package` → `java -jar target\cleverchat.jar`
  - dev 프로파일이 dev.c2r.co.kr(cleverchat_dev)에 붙고 Flyway가 V29 적용.
  - 검증: 세션 없이 `POST /chat/api/sessions/auto {"text":"<시나리오에 없을 법한 크롤 키워드>"}` → 200 + 검색 세션(scenarioId null) + searchOptions/스니펫. 이어서 `select-search-result`로 크롤문서 클릭 → 스니펫+링크.

---

## 7. 완료 기준 (DoD)
- [ ] V29 마이그레이션으로 `scenario_no`/`version_no` nullable + `session_type` 추가, 기존 행 영향 없음.
- [ ] 첫 입력이 시나리오 미매칭일 때 200으로 검색 세션 + 검색 결과 노출(에러 아님).
- [ ] 검색 세션 내 freeText 추가 검색, 크롤문서 클릭 스니펫+링크, 시나리오 클릭 새 세션 전환 동작.
- [ ] 이력에 검색 세션이 "검색" 라벨로 노출.
- [ ] 기존 시나리오 결합 흐름/테스트 전부 유지, 신규 단위테스트 통과.
- [ ] `spotless:apply` 적용, `AccessibilityTemplateTest` 외 신규 실패 0.
- [ ] (권장) fat jar 실DB 기동으로 위 시나리오 수동 검증.

---

## 8. 하지 말 것 / 주의
- P1~P3 코드를 **되돌리거나 재작성하지 말 것**(추출 리팩터링은 허용, 동작 보존).
- 세션 상태 CHECK에 새 값 추가/제거 금지(`ACTIVE/COMPLETED/ABANDONED/EXPIRED` 유지).
- FK 제약 드롭 금지(컬럼만 nullable화).
- 시나리오-결합 세션의 매칭/노드진행 로직(`matchingService.match`, `advance`, `selectOption`)을 검색 세션 경로에서 호출하지 말 것.
- 마이그레이션은 가산적으로만(파괴적 변경/데이터 삭제 금지).
- 커밋은 사용자 지시가 있을 때만(자동 커밋/푸시 금지).
