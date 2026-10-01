# 검색 세션 분리 작업 결과 보고서

작성일: 2026-06-16

대상 문서: `docs/WORKORDER-search-session-decoupling.md`

## 1. 작업 방향

기존 채팅 세션은 `tb_chat_session.scenario_no`, `version_no`가 필수라서 첫 입력이 특정 시나리오에 매칭되지 않으면 세션을 만들 수 없었다. 이 구조에서는 사용자가 `/chat`에서 바로 검색성 질문을 입력했을 때 자연스럽게 검색 결과를 보여주기 어렵고, 억지로 특정 시나리오에 묶으면 이후 버튼과 로깅이 섞이는 문제가 생긴다.

이번 작업의 방향은 다음과 같이 잡았다.

- 시나리오에 매칭되지 않는 첫 입력은 에러가 아니라 `SEARCH` 세션으로 처리한다.
- 검색 결과가 여러 개면 일반 시나리오 옵션이 아닌 `searchOptions` 버튼으로 제공한다.
- 검색 결과 중 크롤 문서를 선택하면 현재 세션 안에서 문서 답변과 원문 링크만 제공한다.
- 검색 결과 중 시나리오를 선택하면 같은 세션의 컨텍스트만 해당 시나리오로 전환한다.
- 세션은 새로 만들지 않고 유지하되, 시나리오 전환 이력은 별도 이벤트 로그로 남긴다.
- 검색 결과 답변 뒤에는 기존 시나리오 노드 버튼이 다시 붙지 않게 한다.
- 크롤/검색 품질 문제로 잘못된 결과가 섞이는 부분은 필터링과 검색 조건을 함께 보정한다.

## 2. 구현 내용

### 2.1 DB 구조 변경

`V29__chat_session_search_decoupling.sql`을 추가했다.

- `tb_chat_session.scenario_no` nullable 처리
- `tb_chat_session.version_no` nullable 처리
- `tb_chat_session.session_type` 추가
- 허용 값은 `SCENARIO`, `SEARCH`
- 기존 세션은 기본값 `SCENARIO` 유지

`V30__chat_session_event_log.sql`을 추가했다.

- `tb_chat_session_event` 생성
- 현재는 `SCENARIO_SWITCH` 이벤트를 저장
- 같은 세션 안에서 어떤 시나리오에서 어떤 시나리오로 전환됐는지 기록
- 전환을 유발한 사용자 메시지와 상세 JSON도 함께 저장

### 2.2 채팅 런타임 변경

`ChatRuntimeService`를 중심으로 검색 세션 흐름을 분리했다.

- `startWithText`에서 시나리오 매칭 실패 시 `startSearch`로 검색 세션 생성
- 검색 세션은 `scenarioNo`, `versionNo`, `currentNodeNo`가 모두 `null`
- 검색 세션 안의 추가 입력은 시나리오 매칭을 다시 하지 않고 바로 검색 수행
- 검색 결과 2건 이상은 최신 BOT 메시지 payload에 `searchOptions`로 저장
- 사용자가 검색 결과를 클릭할 때는 최신 BOT 메시지에 실제 제공된 항목만 선택 가능
- 크롤 문서 선택 시 스니펫 답변과 원문 링크 제공
- 시나리오 결과 선택 시 같은 세션의 시나리오 컨텍스트를 갱신하고 `SCENARIO_SWITCH` 이벤트 기록

### 2.3 버튼 노출 정책 보정

사용자 검증 중 다음 문제가 확인됐다.

> KEPCO 세션에서 `하남`을 검색한 뒤 하남 검색 결과를 선택하면, 답변 아래에 다시 KEPCO 현재 노드 버튼이 노출됨.

이 동작은 세션 로깅 관점에서는 같은 세션을 유지하는 것이 맞지만, UI 관점에서는 문서 답변에 KEPCO 선택지가 붙어 사용자가 세션이 섞였다고 느낄 수 있다.

보정 내용:

- 크롤 문서 답변 payload에 `source: "SEARCH_RESULT"` 저장
- 같은 payload에 `hideNodeOptions: true` 저장
- `response()` 조립 시 최신 BOT 메시지가 검색 결과 답변이면 현재 노드 옵션 조회를 생략
- 검색 결과 선택지 버튼(`searchOptions`)이 떠 있을 때도 기존 노드 옵션은 숨김

### 2.4 검색 품질 보정

검색 세션 분리 후 실제 테스트에서 `하남시`, `김상호` 같은 입력에 부정확한 결과가 섞이는 문제가 확인됐다.

반영 내용:

- 검색 토큰에서 단일 한글 형태소 토큰을 제외해 `시` 같은 과도한 매칭을 줄임
- 초기 시나리오 매칭은 단어 내부 부분 문자열 매칭을 제한
- `김상호`가 `상호` 같은 짧은 키워드에 잘못 걸리는 문제 방지
- 크롤 문서 검색 SQL에서 본문 trigram 유사도 기반의 과도한 매칭 제거
- `content_tokens`, `to_tsvector`, 제목 exact/prefix 중심으로 검색 조건 정리
- 하남시청 통합검색, 로그인, 포토하남 메인 페이지처럼 내용 페이지가 아닌 문서 제외
- 크롤 링크 수집 단계에서도 검색/로그인/대표 포토 인덱스 등 비본문 페이지 필터링

### 2.5 하남시청 데이터 정제

실제 검색 결과 품질 확인 과정에서 하남시청 관련 데이터도 정리했다.

- 하남시청 공식 홈페이지 크롤 타깃 추가/정리
- 검색 결과로 부적절했던 통합검색 페이지 문서 제거
- 로그인 페이지 문서 제거
- 포토하남 메인페이지 문서 제거
- 하남시청 성공 문서 기준 불필요 문서 제거 후 검색 대상 정제

## 3. 주요 변경 파일

- `src/main/resources/db/migration/V29__chat_session_search_decoupling.sql`
- `src/main/resources/db/migration/V30__chat_session_event_log.sql`
- `src/main/java/kr/co/cleverchat/domain/chatbot/model/ChatSession.java`
- `src/main/java/kr/co/cleverchat/domain/chatbot/mapper/ChatSessionMapper.java`
- `src/main/resources/mapper/chatbot/ChatSessionMapper.xml`
- `src/main/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeService.java`
- `src/main/java/kr/co/cleverchat/domain/chatbot/controller/ChatRuntimeApiController.java`
- `src/main/java/kr/co/cleverchat/domain/chatbot/dto/ChatRuntimeDtos.java`
- `src/main/java/kr/co/cleverchat/domain/chatbot/service/ScenarioMatchingService.java`
- `src/main/java/kr/co/cleverchat/domain/search/service/SearchService.java`
- `src/main/resources/mapper/search/SearchMapper.xml`
- `src/main/java/kr/co/cleverchat/domain/crawl/service/CrawlService.java`
- `src/test/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeServiceTest.java`
- `src/test/java/kr/co/cleverchat/domain/chatbot/service/ScenarioMatchingServiceTest.java`
- `src/test/java/kr/co/cleverchat/domain/search/service/SearchServiceTest.java`
- `src/test/java/kr/co/cleverchat/domain/crawl/service/CrawlServiceTest.java`

## 4. 검증 결과

### 4.1 단위 테스트

다음 테스트 묶음을 실행했고 통과했다.

```powershell
.\mvnw.cmd -q "-Dspotless.check.skip=true" "-Dtest=ChatRuntimeServiceTest,SearchServiceTest,ScenarioMatchingServiceTest,CrawlServiceTest,MyBatisMapperSqlSafetyTest" test
```

주요 검증 항목:

- 시나리오 매칭 실패 시 검색 세션 생성
- 검색 세션에서는 시나리오 매칭 없이 재검색
- 검색 결과 버튼이 있을 때 현재 노드 옵션 미노출
- 검색 문서 답변 후 현재 노드 옵션 미노출
- 검색 결과 시나리오 선택 시 같은 세션에서 컨텍스트 전환 및 이벤트 기록
- `김상호` 같은 단어 내부 부분 문자열 오매칭 방지
- `하남시` 검색 시 단일 한글 형태소 토큰 제외
- 비본문 크롤 페이지 필터링

### 4.2 빌드 및 서버 기동

패키징을 수행했고 성공했다.

```powershell
.\mvnw.cmd -q "-DskipTests" "-Dspotless.check.skip=true" package
```

서버는 `dev` 프로필로 재기동했다.

- URL: `http://localhost:8080`
- 마지막 확인 PID: `5164`

### 4.3 실제 API 흐름 검증

다음 흐름을 직접 확인했다.

1. KEPCO 시나리오로 세션 시작
2. 같은 세션에서 `하남` 입력
3. 검색 결과 3개가 `searchOptions`로 노출
4. 일반 KEPCO 노드 옵션은 노출되지 않음
5. 첫 번째 하남 검색 결과 선택
6. 세션의 `scenarioId`는 KEPCO `34`로 유지
7. 선택 후 `options`는 `0`
8. 선택 후 `searchOptions`도 `0`
9. 문서 답변만 내려오고 KEPCO 버튼은 다시 붙지 않음

검증 요약:

```text
scenarioIdAfterFreeText: 34
freeTextOptionCount: 0
freeTextSearchOptionCount: 3
scenarioIdAfterSelect: 34
optionCountAfterSelect: 0
searchOptionCountAfterSelect: 0
```

## 5. 최종 결과

원본 워크오더의 핵심 목표였던 “시나리오에 묶이지 않는 검색 세션”은 반영됐다.

현재 동작은 다음과 같다.

- 시나리오에 매칭되는 첫 입력은 기존처럼 시나리오 세션으로 진행된다.
- 시나리오에 매칭되지 않는 첫 입력은 검색 세션으로 생성된다.
- 기존 시나리오 세션 안에서 검색이 발생해도 세션은 유지된다.
- 검색 결과 중 시나리오를 선택하면 세션은 유지하고 컨텍스트만 전환한다.
- 이 전환은 `tb_chat_session_event`에 `SCENARIO_SWITCH`로 기록된다.
- 검색 결과 중 크롤 문서를 선택하면 문서 답변만 보여주고 기존 시나리오 버튼은 숨긴다.
- 검색 결과 선택지는 최신 BOT 메시지에 제시된 항목만 선택 가능하다.
- 하남시청 검색 품질 문제를 줄이기 위해 크롤 데이터와 검색 조건을 함께 정리했다.

## 6. 남은 주의사항

- 전체 테스트는 기존에 알려진 `AccessibilityTemplateTest` 관련 실패가 있어, 이번 변경 검증은 관련 테스트 묶음 중심으로 수행했다.
- 운영 데이터에서는 크롤 문서 품질이 검색 품질에 직접 영향을 주므로, 검색/로그인/대표 인덱스 같은 비본문 페이지 유입 여부를 계속 점검해야 한다.
- 검색 결과가 너무 넓게 잡히는 경우에는 형태소 필터와 SQL 매칭 조건을 추가로 조정해야 한다.
