# 작업지시서 — 검색결과 노출 정책(적합도 기반 동적 컷) + 대화 "이전 단계" 내비게이션

> 대상 실행자: Codex (자동 코딩 에이전트)
> 전제: 본 문서의 "현재 상태"는 조사 시점(파일:라인 명시) 기준이다. **착수 전 해당 파일을 직접 읽어 현재 코드/컬럼/제약을 재확인**하라.
> 모든 변경은 기존 시나리오-결합 흐름·검색 세션 흐름·기존 테스트를 **깨지 않아야** 한다. 마이그레이션은 가산적으로만.

---

## 0. 한 줄 목표

두 가지를 동시에 처리한다.

- **A. 검색결과 노출 정책**: 챗봇 폴백 검색이 다중 후보를 반환할 때, "고정 개수(3→10)"로 자르는 방식 대신 **후보 풀(LIMIT)과 화면 노출 수(DISPLAY)를 분리**하고, **적합도(ts_rank score) 기반 동적 컷 + 노출 상한 + '더 보기'**로 운영 친화적으로 만든다. 임계값/상한은 **외부 설정(application.yml)으로 운영자가 무중단 튜닝** 가능하게 한다.
- **B. 이전 단계 버튼**: 시나리오 대화에서 선택지를 누르고 다음 노드로 이동한 뒤, **"이전" 버튼으로 직전 노드(질문/선택지)로 되돌아가는** 기능을 추가한다. 추가-전용(append-only) 로그를 보존하는 비파괴 방식으로 구현한다.

---

## 1. 배경 & 현재 상태 (조사 결과)

### 1.1 검색 노출 (A 관련)
- `ChatRuntimeService.SEARCH_FALLBACK_LIMIT = 10` — [ChatRuntimeService.java:65](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L65). (과거 3 → 현재 10으로 이미 상향됨.)
- `respondWithSearch(...)` — [ChatRuntimeService.java:312](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L312):
  - `searchService.search(text, "CHAT_FALLBACK", SEARCH_FALLBACK_LIMIT, ...)` 호출 후
  - `results.size() > 1` → `insertSearchOptionsMessage(...)` (선택지 버튼) — [:327](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L327)
  - `results.size() == 1` → 크롤문서 스니펫+링크 또는 단일 시나리오/AI 답변
  - `0` → `NO_MATCH` 실패기록 + 안내 메시지
- 검색결과는 **채팅 말풍선 안의 클릭 버튼**으로 렌더된다(선택지 UI). 옵션 payload는 직전 BOT 메시지에 JSON으로 저장 — `insertSearchOptionsMessage` [:714](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L714).
- `SearchResultItem`에는 **`double score`(ts_rank 합산)** 가 이미 실려 있다 — [SearchResultItem.java:10](../src/main/java/kr/co/cleverchat/domain/search/model/SearchResultItem.java#L10). SQL은 이미 **`ORDER BY score DESC, field_priority, matched_field` + `LIMIT #{limit}`** — [SearchMapper.xml:231-232](../src/main/resources/mapper/search/SearchMapper.xml#L231). 즉 풀은 점수순 상위 N이 보장됨.
- `SearchService.search()` 자체 기본/최대 = `DEFAULT_LIMIT=20`, `MAX_LIMIT=100` — [SearchService.java:26-27](../src/main/java/kr/co/cleverchat/domain/search/service/SearchService.java#L26). (챗봇은 명시적으로 10을 넘김.)
- 프론트 `searchOptionButton`/`inlineSearchOptions` — [chat.js:217-240](../src/main/resources/static/asset/chat/chat.js#L217). `SearchOptionResponse(crawlDocumentNo, scenarioNo, label, matchedField)` — score/snippet은 클라이언트로 안 내려감(클릭 시 노출). [ChatRuntimeDtos.java:49](../src/main/java/kr/co/cleverchat/domain/chatbot/dto/ChatRuntimeDtos.java#L49).

### 1.2 대화 내비게이션 (B 관련)
- 세션은 **단일 포인터 `currentNodeNo`만** 보유. 이전 노드 스택/히스토리 컬럼 없음 — [ChatSession.java:12](../src/main/java/kr/co/cleverchat/domain/chatbot/model/ChatSession.java#L12).
- 단, **모든 메시지가 `nodeNo`+`seq`를 영속화**한다(`insertUserMessage`/`insertBotMessage`가 노드번호 저장) — [ChatRuntimeService.java:561,587](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L561). → **노드 경로는 메시지 로그에서 재구성 가능**(별도 스택 불필요).
- `advance(session, nextNodeId, botSeq)` — [:531](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L531): `updateCurrentNode`로 포인터 전진 + 다음 노드 BOT 메시지 insert. `nextNodeId==null`이면 `COMPLETED`.
- `response()` — [:602](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L602): 옵션을 **`currentNodeNo` 기준으로 매번 새로 조회**(`optionMapper.findEnabledByNodeId`). 즉 포인터만 되돌리면 그 노드 선택지가 자동 복원된다. `state=="ACTIVE" && currentNodeNo!=null`일 때만 옵션 노출.
- `selectOption` — [:238](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L238): 세션 advisory lock → 옵션 검증 → USER 메시지 insert → `advance`.
- 감사 이벤트 테이블 `tb_chat_session_event` 존재(V30). **CHECK 제약이 `event_type IN ('SCENARIO_SWITCH')`로 고정** — [V30__chat_session_event_log.sql:19](../src/main/resources/db/migration/V30__chat_session_event_log.sql#L19). 시나리오 전환은 `insertScenarioSwitchEvent`로 기록됨 — [ChatRuntimeService.java:469](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L469).
- 컨트롤러 엔드포인트 패턴 — [ChatRuntimeApiController.java](../src/main/java/kr/co/cleverchat/domain/chatbot/controller/ChatRuntimeApiController.java): `POST /chat/api/sessions/{id}/select-option`, `/select-search-result`, `/free-text` 등.
- 현재 최고 마이그레이션 = **V33** (V29~V33 사용 중, V34 비어 있음).

> ⚠️ 착수 전 필수 확인: `ChatRuntimeService`, `ChatSessionMapper.xml`, `ChatMessageMapper.xml`, `SearchMapper.xml`, `db/migration` 최고 번호, `chat.js` 렌더링을 직접 읽어 라인/시그니처를 재확인하라.

---

## 2. 최종 동작 정의 (수용 기준)

### A. 검색결과 노출
1. 검색 후보는 여전히 **풀 LIMIT(기본 10)**으로 가져온다. (DB 호출 변경 없음.)
2. 화면(버튼)에 노출하는 후보는 다음 규칙으로 추린다:
   - **적합도 컷**: `score >= topScore × relativeThreshold`(기본 0.5)인 후보만.
   - **노출 상한**: 위 결과를 다시 상위 `displayMax`(기본 5)개까지.
   - **최소 보장**: 적합도 컷으로 0개가 되는 일이 없도록 **항상 최소 1개**(최상위)는 노출.
3. 적합도 컷/상한으로 **잘려나간 후보가 있으면**, 선택지 마지막에 **"검색 결과 더 보기 (N건)"** 버튼을 추가한다. 클릭 시 나머지 후보를 같은 메커니즘으로 이어서 노출(2차 메시지). 더 보일 게 없으면 버튼 미표시.
4. 위 임계값/상한/풀 크기는 **`application.yml`의 `chat.search.*` 프로퍼티로 외부화**하여 운영자가 환경별로(또는 재기동만으로) 조정 가능해야 한다. 코드 상수 하드코딩 금지.
5. 단일 후보(1건) 및 0건 동작은 **기존과 동일**(스니펫+링크 / 단일 시나리오·AI / NO_MATCH).
6. 관측: 검색 로그에 **"가져온 풀 수"와 "실제 노출 수"**를 함께 남겨 운영자가 컷 정책 효과를 모니터링할 수 있게 한다(아래 5.A 참조).

### B. 이전 단계 버튼
1. **시나리오 세션**에서 직전 노드가 존재하면, 대화 하단(최신 BOT 메시지 영역)에 **"이전" 버튼**을 노출한다.
2. "이전" 클릭 시:
   - `currentNodeNo`를 **직전 노드로 되돌리고**(메시지 로그에서 재구성), 상태가 `COMPLETED`였으면 `ACTIVE`로 복원한다.
   - **추가-전용**으로 "이전 단계로 돌아갑니다." 류 BOT 메시지 + 직전 노드 질문/선택지를 다시 렌더한다. **기존 메시지를 삭제/변조하지 않는다.**
   - 감사 이벤트(`NODE_BACK`)를 기록한다.
3. **노출 조건(`canGoBack`)**: 시나리오 세션(`scenarioNo != null`) AND 직전 노드가 존재(시작 노드가 아님) AND 세션이 `ACTIVE` 또는 `COMPLETED`. 그 외에는 버튼 미노출.
4. **검색 세션**(`scenarioNo == null`, `currentNodeNo == null`)에서는 이전 버튼을 노출하지 않는다(v1 범위 밖).
5. 직전 노드 계산은 **현재 시나리오/버전 내부로 한정**한다. 시나리오 전환(검색→시나리오) 경계를 넘어 되돌아가지 않는다(v1 한계, 명시).
6. 기존 `selectOption`/`advance`/`freeText` 동작 불변.

---

## 3. 변경 범위 — A. 검색결과 노출

### 3.A.1 설정 프로퍼티 신설 — `chat.search.*`
- `@ConfigurationProperties(prefix = "chat.search")` 클래스 신설(예: `domain/chatbot/config/ChatSearchProperties.java`) 또는 기존 챗봇 설정 클래스가 있으면 거기에 중첩.
- 필드(기본값):
  - `poolLimit` = 10  (DB에서 가져올 후보 수, 기존 `SEARCH_FALLBACK_LIMIT` 대체)
  - `displayMax` = 5  (버튼으로 노출할 최대 수)
  - `relativeThreshold` = 0.5  (`topScore` 대비 노출 하한 비율; 0~1)
  - `moreEnabled` = true  ("더 보기" 버튼 사용 여부)
- `application.yml`에 기본 블록 추가:
  ```yaml
  chat:
    search:
      pool-limit: 10
      display-max: 5
      relative-threshold: 0.5
      more-enabled: true
  ```
- `@EnableConfigurationProperties` 등록(메인 설정 또는 `@Configuration`). 기존 프로퍼티 등록 방식과 일치시킬 것.
- 검증: `displayMax >= 1`, `poolLimit >= displayMax`, `0 <= relativeThreshold <= 1`을 생성 시 보정(clamp)하거나 `@Validated`로 강제.

### 3.A.2 노출 선별 로직 — `ChatRuntimeService`
- `ChatSearchProperties` 주입.
- `SEARCH_FALLBACK_LIMIT` 상수 제거 → `props.getPoolLimit()` 사용.
- `respondWithSearch`에서 `results`(점수 내림차순 보장됨)를 다음으로 가공하는 **순수 헬퍼** 추가:
  ```java
  // 반환: 노출할 부분집합 + 잔여 건수
  private record SearchDisplay(List<SearchResultItem> shown, int hiddenCount) {}

  private SearchDisplay selectForDisplay(List<SearchResultItem> pool) {
      if (pool.isEmpty()) return new SearchDisplay(List.of(), 0);
      double top = pool.get(0).getScore();
      double floor = top * props.getRelativeThreshold();
      List<SearchResultItem> relevant = pool.stream()
          .filter(it -> it.getScore() >= floor)
          .toList();
      if (relevant.isEmpty()) relevant = List.of(pool.get(0)); // 최소 1 보장
      List<SearchResultItem> shown = relevant.stream().limit(props.getDisplayMax()).toList();
      int hidden = pool.size() - shown.size();
      return new SearchDisplay(shown, hidden);
  }
  ```
  - 주의: `score`가 모두 동일(예: 토큰 폴백)하거나 0인 경우에도 NPE/0-division 없이 동작해야 함(`top==0`이면 floor=0 → 전부 통과 → displayMax 컷). 단위테스트로 못 박을 것.
- `respondWithSearch` 분기 수정:
  - `results.size() > 1`일 때 `selectForDisplay(results)`로 `shown`/`hidden` 계산 → `insertSearchOptionsMessage(sid, botSeq, shown, hidden>0 && props.isMoreEnabled(), latencyMs)`.
  - `shown.size() == 1`로 줄어드는 경우의 처리: **버튼 1개 + (있으면) 더보기**로 노출한다. (단일 결과 즉답 분기는 "원래 풀이 1건"일 때만. 즉 컷 결과가 1이어도 풀이 2건+이면 버튼 UI 유지 — 사용자에게 선택 맥락을 보존.)
- `insertSearchOptionsMessage` 시그니처에 `boolean showMore`(또는 `int hiddenCount`) 추가:
  - payload에 `searchOptions` 외 **`more` 메타**(예: `{"more": {"available": N}}`) 추가. "더 보기"는 별도 서버 처리가 필요하므로 아래 3.A.3 참고.

### 3.A.3 "더 보기" 처리 (서버) — **확정: 실제 추가 노출**
- "더 보기" 클릭은 프론트에서 `free-text`가 아니라 **신규 엔드포인트** `POST /chat/api/sessions/{id}/search-more`로 처리. 서버는 **이미 노출된 항목을 제외한 나머지 후보**를 같은 `selectForDisplay` 규칙(상한 `displayMax` 적용, 임계값은 완화/해제)으로 **새 BOT 메시지에 이어서 노출**.
- **payload 설계(중요)**: `insertSearchOptionsMessage`가 저장하는 payload에 **노출분 + 잔여분 후보를 모두 보관**한다(잔여분에는 `hidden:true` 마킹 또는 별도 `overflow` 배열). 또는 직전 사용자 쿼리를 payload에 저장해 `search-more`에서 재검색 후 차집합.
  - 권장: **노출분 `searchOptions` + 잔여분 `overflowOptions`를 한 payload에 저장**. `search-more`는 `overflowOptions`에서 다음 `displayMax`개를 꺼내 새 BOT 메시지의 `searchOptions`로 옮기고, 남으면 다시 `overflow`로. 더 없으면 "더 보기" 미표시.
- **anti-IDOR 정합**: 기존 `findOfferedSearchOption`([:673](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L673))은 "직전 BOT payload에 제시된 옵션만 선택 가능"을 보장한다. **더보기로 새로 제시되는 항목은 반드시 새 BOT 메시지 payload(`searchOptions`)에 기록**되어야 클릭 가능. (잔여분을 클릭 가능하게 만들기 전에는 `searchOptions`가 아니라 `overflowOptions`에만 두어, 노출 전 클릭이 차단되도록.)
- 메시지 크기: 잔여분까지 저장 → payload 증가. poolLimit=10 수준이면 무시 가능.

### 3.A.4 관측/로그 — **확정: 경량 로그**
- `SearchService.recordSearch`는 이미 `result_count`(풀 크기)를 남긴다 — [SearchService.java:124](../src/main/java/kr/co/cleverchat/domain/search/service/SearchService.java#L124).
- 추가로 `ChatRuntimeService.respondWithSearch`에서 **구조화 로그 1줄**: `log.info("chat search display query='{}' pool={} shown={} hidden={}", ...)` (또는 SLF4J MDC/구조화). 운영자가 임계값/상한 튜닝 근거로 사용.
- **`tb_search_log.shown_count` 정식 컬럼은 미채택**(추후 대시보드 필요 시 별도 마이그레이션). → 본 작업의 V34에는 검색 관측 컬럼을 넣지 않는다.

---

## 4. 변경 범위 — B. 이전 단계 버튼

### 4.B.1 마이그레이션 — `V34__chat_back_navigation.sql` (가산적)
> 착수 시 `db/migration` 최고 번호 재확인 후 +1 (현재 V33 → V34).
```sql
-- 뒤로가기 감사 이벤트 허용 (기존 CHECK 확장). 이게 V34의 전부.
ALTER TABLE tb_chat_session_event DROP CONSTRAINT ck_chat_session_event_type;
ALTER TABLE tb_chat_session_event ADD CONSTRAINT ck_chat_session_event_type
    CHECK (event_type IN ('SCENARIO_SWITCH','NODE_BACK'));
-- 검색 노출 관측은 경량 로그로 처리 → shown_count 컬럼 추가하지 않음(결정 §9-2).
```
- CHECK 교체는 가산적(허용값 추가)이라 기존 행에 영향 없음. 기존 `'SCENARIO_SWITCH'` 행 그대로 유효.

### 4.B.2 직전 노드 계산 — `ChatMessageMapper`
- 신규 매퍼 메서드: **현재 노드 직전의 노드번호**를 메시지 로그에서 구한다.
  ```java
  // 같은 세션에서 node_no IS NOT NULL 인 BOT 메시지를 seq 오름차순으로 봤을 때,
  // current_node_no 가 가장 최근에 등장하기 "직전"의 서로 다른 node_no.
  Long findPreviousNodeNo(@Param("sessionId") String sessionId,
                          @Param("currentNodeNo") Long currentNodeNo);
  ```
  - SQL 가이드(정확 구현은 실제 스키마로): 세션의 `direction='BOT' AND node_no IS NOT NULL` 메시지를 `seq` 순으로 정렬 → `current_node_no`가 처음 나타나는 행의 직전, **다른 node_no**를 반환. 동일 노드가 반복 노출됐을 수 있으니 "직전의 서로 다른 노드"로 정의. 없으면 NULL.
  - 대안(권장, 단순): 서비스에서 `messageMapper.findBySessionId(sid)`로 BOT+nodeNo 시퀀스를 가져와 자바로 계산(메시지 수가 작아 비용 무시 가능, 테스트 용이). 매퍼 신규 SQL 없이 처리 가능 → **이 방식 우선**.

### 4.B.3 서비스 — `ChatRuntimeService.goBack(...)`
```java
@Transactional
public SessionResponse goBack(UUID sessionId, ChatRequestContext context) {
    String sid = sessionId.toString();
    sessionMapper.lockSessionByAdvisoryKey(sid);
    ChatSession session = findSession(sid);
    validateOwnerAndActive(session, context); // 단 COMPLETED 허용 필요 → 아래 주의
    // 1) 가드: 검색세션(scenarioNo==null) 또는 currentNodeNo==null → 400 "되돌아갈 단계가 없습니다."
    // 2) previousNodeNo 계산 (4.B.2). null이면 400.
    // 3) previousNode 로드 + 같은 versionNo 검증 (advance()의 버전 정합 가드 재사용).
    // 4) sessionMapper.updateCurrentNode(sid, previousNodeNo, stateFor(prevNode)=ACTIVE, expiresAt())
    //    - COMPLETED였으면 ACTIVE로 복원.
    // 5) seq = messageMapper.selectNextSeq(sid);
    //    insertBotMessage(sid, seq, prevNode, "이전 단계로 돌아갑니다." 또는 prevNode 질문 재노출)
    //    → response()가 prevNode 기준으로 옵션 자동 복원.
    // 6) 감사: insert NODE_BACK 이벤트(tb_chat_session_event), detail에 {fromNodeNo, toNodeNo}.
    return response(sid);
}
```
주의:
- `validateOwnerAndActive`가 `state=="ACTIVE"`만 통과시키면 `COMPLETED` 세션에서 되돌리기가 막힌다. **소유자/만료 검증은 하되 상태 게이트는 별도로** 처리(ACTIVE/COMPLETED 모두 허용, EXPIRED/ABANDONED는 거부). 필요 시 `validateOwner` + 만료체크만 사용.
- rate limit: 되돌리기에도 `enforceRateLimit` 적용 여부 결정(권장: 적용 — 남용 방지).
- `advance`의 버전 정합 가드(`session.getVersionNo().equals(node.getVersionNo())`)를 prevNode에도 적용.

### 4.B.4 `canGoBack` 플래그 — `response()` / DTO
- `SessionResponse`에 **`boolean canGoBack`** 필드 추가 — [ChatRuntimeDtos.java:33](../src/main/java/kr/co/cleverchat/domain/chatbot/dto/ChatRuntimeDtos.java#L33).
  - ⚠️ record 인자 추가 → **모든 `new SessionResponse(...)` 호출부와 `ChatRuntimeApiControllerTest`의 생성자 인자 정합**을 함께 수정해야 함(컴파일 깨짐 방지). 호출부는 `response()` 한 곳으로 집중돼 있으니([:616](../src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java#L616)) 거기서 계산.
- `response()`에서 `canGoBack` 계산: `scenarioNo != null && currentNodeNo != null && (state ACTIVE|COMPLETED) && previousNodeNo(존재)`. 직전 노드 계산은 이미 로드한 `rawMessages`로 자바 계산(추가 쿼리 없음).

### 4.B.5 컨트롤러
- `ChatRuntimeApiController`에 추가 — [ChatRuntimeApiController.java:98](../src/main/java/kr/co/cleverchat/domain/chatbot/controller/ChatRuntimeApiController.java#L98) 패턴 따라:
  ```java
  @PostMapping("/sessions/{sessionId}/back")
  public SessionResponse goBack(@PathVariable UUID sessionId, /* context 주입 패턴 동일 */) {
      return chatRuntimeService.goBack(sessionId, context);
  }
  ```
- (안1 더보기 채택 시) `POST /sessions/{sessionId}/search-more`도 동일 패턴으로 추가.

### 4.B.6 프론트엔드 `chat.js`
- `renderMessages(messages, options, searchOptions, canGoBack)` 시그니처에 `canGoBack` 전달(또는 `renderSession`에서 세션 응답 참조).
- 최신 BOT 메시지 옵션/검색옵션 렌더 뒤([chat.js:263-268](../src/main/resources/static/asset/chat/chat.js#L263)), `canGoBack`이면 **"이전" 버튼** 추가:
  - 버튼 클릭 → `api('/chat/api/sessions/' + state.sessionId + '/back', {method:'POST'})` → 응답으로 `renderSession`.
  - 위치/스타일: 선택지 묶음 위 또는 헤더("현재 상담" 영역) 근처. 기존 `.secondary` 버튼 스타일 재사용, 좌측 정렬 작은 버튼 권장.
  - `state.busy` 동안 disabled 처리(기존 패턴 [chat.js:43-45](../src/main/resources/static/asset/chat/chat.js#L43)).
- (안1 더보기) `searchOptions` 렌더 시 payload `more.available > 0`이면 "검색 결과 더 보기 (N건)" 버튼 추가 → `/search-more` 호출.

---

## 5. 테스트 요구사항 (`src/test/java/.../chatbot/...`, Mockito 스타일 유지)

### 5.A 검색 노출
1. `selectForDisplay`: 풀 6건, topScore=1.0, threshold=0.5 → score≥0.5만, 상한 5 적용, hidden 계산 정확.
2. 모든 score 동일/0 → division 안전, displayMax까지 노출, 최소 1 보장.
3. 풀이 2건+이나 컷 결과 1건 → **버튼 UI 유지**(단일 즉답 분기로 빠지지 않음).
4. 프로퍼티 변경(displayMax=3 등) 반영(주입된 props로 동작) 검증.
5. (정식 관측안 채택 시) shownCount 기록 검증.

### 5.B 이전 단계
6. 선택지 2회 진행 후 `goBack` → `currentNodeNo`가 직전 노드로, 옵션 복원, "이전 단계" BOT 메시지 append, **기존 메시지 미삭제**(ArgumentCaptor로 update/insert만, delete 호출 없음).
7. 시작 노드(직전 없음) → `goBack` 400 또는 `canGoBack=false`.
8. 검색 세션(scenarioNo=null) → `canGoBack=false`, `goBack` 400.
9. `COMPLETED` 세션 `goBack` → `ACTIVE` 복원 + 직전 노드.
10. `NODE_BACK` 이벤트 insert 검증(detail from/to 노드).
11. `SessionResponse` 인자 추가에 따른 **기존 테스트/컨트롤러 테스트 생성자 정합** 전부 수정·통과.

- **기존 테스트 전부 통과 유지** (특히 `freeTextNoMatchUsesSearchFallbackWhenAvailable`, `freeTextNoMatchUsesAiSuggestionWhenAvailable`, `startWithTextMatchesScenarioAndSendsInitialQuestion`, select-search-result 관련).

---

## 6. 빌드 & 검증

- 이 머신은 **컴파일 JVM CDS 손상으로 `EXCEPTION_ACCESS_VIOLATION`** 발생 → maven 호출 전 환경변수:
  - PowerShell: `$env:MAVEN_OPTS="-Xshare:off"`
- 포맷터 필수: `.\mvnw.cmd -q spotless:apply`
- 단위테스트: `.\mvnw.cmd "-Dspotless.check.skip=true" test`
  - 사전 존재 실패 `AccessibilityTemplateTest`(sub.css 관련)은 본 작업과 무관 — **신규 실패 0**이어야 한다.
- 실DB 기동 검증(권장, 한글 경로로 `spring-boot:run` classpath 깨짐 → fat jar):
  - `.\mvnw.cmd "-DskipTests" "-Dspotless.check.skip=true" package` → `java -jar target\cleverchat.jar`
  - dev 프로파일 → dev.c2r.co.kr, Flyway V34 적용.
  - A 검증: 후보 多인 질의 → 버튼이 displayMax 이하로 노출 + 적합도 낮은 항목 누락 + (있으면) 더보기. yml에서 `chat.search.display-max` 조정 후 재기동 시 반영.
  - B 검증: 시나리오 선택지 2단계 진행 → "이전" 노출/클릭 → 직전 선택지 복원, 이전 메시지 보존, COMPLETED 후 되돌리기 동작.

---

## 7. 완료 기준 (DoD)

- [ ] `chat.search.*` 프로퍼티로 poolLimit/displayMax/relativeThreshold/moreEnabled 외부화, 하드코딩 상수 제거.
- [ ] 적합도 기반 동적 컷 + 노출 상한 + 최소 1 보장 동작, 잘린 후보 있으면 "더 보기"(채택 안에 따라) 노출.
- [ ] 검색 노출 관측(로그 또는 shown_count) 추가.
- [ ] `V34` 마이그레이션으로 `NODE_BACK` 이벤트 허용(+선택 shown_count), 기존 행 영향 없음.
- [ ] `POST /sessions/{id}/back` + `goBack` 서비스로 직전 노드 복원(비파괴, 감사 이벤트 기록), `canGoBack` 플래그 노출.
- [ ] 프론트 "이전" 버튼(+선택 "더 보기") 렌더 및 동작, busy 가드.
- [ ] `SessionResponse` 인자 변경에 따른 호출부/테스트 전부 정합, 신규+기존 테스트 통과.
- [ ] `spotless:apply` 적용, `AccessibilityTemplateTest` 외 신규 실패 0.
- [ ] (권장) fat jar 실DB 기동 수동 검증 A·B.

---

## 8. 하지 말 것 / 주의

- 기존 메시지 로그를 **삭제/변조하지 말 것**(되돌리기는 추가-전용). append-only 감사성 유지.
- 마이그레이션은 **가산적으로만**(CHECK 허용값 추가, 컬럼 추가). 파괴적 변경/데이터 삭제 금지.
- 시나리오-결합 세션의 매칭/노드진행(`matchingService.match`, `advance`, `selectOption`)을 **검색 세션 경로에서 호출하지 말 것**.
- `goBack`은 **현재 시나리오/버전 내부**로만 되돌린다. 시나리오 전환 경계를 넘지 말 것(v1).
- 검색 "더 보기"로 새로 제시하는 항목은 **반드시 새 BOT payload에 기록**(anti-IDOR `findOfferedSearchOption` 정합).
- `SearchOptionResponse`/`SessionResponse` 등 **record 인자 변경 시 모든 호출부·테스트 동시 수정**(컴파일 정합).
- 검색 노출 분기 변경 시 **단일(1건)·0건 기존 동작 보존**.
- 커밋/푸시는 **사용자 지시가 있을 때만**(자동 커밋 금지).

---

## 9. 결정 사항 (확정됨, 2026-06-18)

1. **"더 보기" 방식** = **안1(실제 추가 노출)**. → 3.A.3 안1 구현. 잔여 후보를 새 BOT payload에 기록(anti-IDOR 정합), `POST /sessions/{id}/search-more`.
2. **검색 노출 관측** = **경량 로그**(구조화 로그로 `query, pool=, shown=, hidden=`). → `tb_search_log.shown_count` 컬럼/V34 추가 컬럼 **미채택**(추후 대시보드 필요 시 재검토). 즉 **V34는 `NODE_BACK` CHECK 확장만** 수행.
3. **기본값** = `displayMax=5`, `relativeThreshold=0.5`, `poolLimit=10`, `moreEnabled=true` 유지(프로퍼티라 운영 중 조정 가능).
4. **이전 버튼 위치** = **선택지(옵션/검색옵션) 버튼이 노출되는 그 인라인 자리에 함께 노출**. 즉 `renderMessages`에서 최신 BOT 메시지의 `inlineOptions`/`inlineSearchOptions`가 붙는 위치([chat.js:263-268](../src/main/resources/static/asset/chat/chat.js#L263))에 "이전" 버튼도 같은 묶음으로 렌더. 별도 헤더/입력창 배치 안 함.
