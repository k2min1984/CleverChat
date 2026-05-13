# Phase 1 명명정렬 작업지시서

- 작성일: 2026-05-13
- 대상 프로젝트: `3.개발/cleverchat`
- 베이스 소스: `8.소스/OverseasNPP_20260511`
- 선행 기준: `1.기획/결정사항.md` §11 "8소스 우선 정렬 원칙"
- 작업 성격: scenario 도메인 8소스 기준 폴더/화면/명명/SQL 정렬 PR 지시서. 본 문서는 실제 코드 수정, 리네임, 파일 이동을 수행하지 않는다.

## 0. 절대 원칙

1. `8.소스/OverseasNPP_20260511`은 참고자료가 아니라 CleverChat 개발의 원본 베이스 소스다.
2. 8소스 실제 구조는 `kr.core`, `kr.admmgr`, `kr.admmgr.<기능별>`, `com` 축을 기준으로 한다.
3. `kr.core`는 공통 코어, 애플리케이션/웹 설정, 세션/공통 부트스트랩 축이다.
4. `kr.admmgr`는 관리자 업무 root다.
5. `kr.admmgr.<기능별>`는 `scenario`, `member`, `security`, `chat`, `search` 같은 관리자 업무 기능별 패키지다.
6. `com`은 8소스의 공통/외부 유틸 축으로 유지한다.
7. CleverChat의 기존 `kr.co.cleverchat.domain` 및 `kr.co.cleverchat.common` 기반 Spring Boot 스타일 구조는 최종 기준이 아니다. 현재 구현 출발점이자 단계적 정렬 대상이다.
8. 코드 작성 형태, 폴더 구조, 패키지 명명, Controller/Service/Mapper/XML 구성, SQL ID 규칙, 화면 파일명, 화면 액션명, 관리자 UI 디자인/레이아웃/CSS/템플릿 구성은 8소스 우선 정렬 원칙을 따른다.
9. 기술적으로 불가피한 차이는 `1.기획/결정사항.md` §11.3 예외 표에 한정한다.
10. Phase 1은 scenario 도메인을 첫 정렬 대상으로 삼고, 1.A부터 1.D까지 순차 적용한다.

## 1. Phase 1 분할 원칙

Phase 1은 한 번에 전체 리네임을 수행하지 않는다. 아래 4개 단계로 나누어 각 단계가 통과한 뒤 다음 단계로 진입한다.

| 단계 | 이름 | 핵심 산출물 |
|---|---|---|
| Phase 1.A | 관리자 공통 레이아웃 + 템플릿 정렬 | `templates/admmgr/scenario/*`, 관리자 공통 레이아웃/CSS/JS 기준 |
| Phase 1.B | 컨트롤러 + 메서드명 정렬 | `AdmScenario*Controller`, `scenarioList`, `scenarioRegist` 등 8소스식 액션명 |
| Phase 1.C | `kr.admmgr.<기능별>` 기준 패키지 평탄화 | `kr.admmgr.scenario` 중심 업무 패키지 |
| Phase 1.D | SQL XML namespace/ID 정렬 | `sql/postgresql/tb_scenario*.xml`, `tb_scenario*` namespace, 8소스식 SQL ID |

각 단계는 목적, 포함 파일 범위, 금지 범위, 검증 명령, 롤백 기준, 다음 단계 진입 조건을 반드시 PR 본문에 기록한다.

## 2. Phase 1.A 관리자 공통 레이아웃 + 템플릿 정렬

### 2.1 목적

scenario 화면을 8소스 관리자 UI 기준으로 먼저 정렬한다. 컨트롤러, Java 패키지, SQL XML은 아직 바꾸지 않고 화면 경로와 템플릿 구조의 기준만 만든다.

### 2.2 포함 파일 범위

| 구분 | 현재 후보 | 8소스 정렬 후보 |
|---|---|---|
| 관리자 레이아웃 | `src/main/resources/templates/admin/index.html` 또는 현행 layout fragment | `src/main/resources/templates/layout/admin-layout.html` |
| scenario 목록 | `src/main/resources/templates/admin/scenarios/list.html` | `src/main/resources/templates/admmgr/scenario/scenarioList.html` |
| scenario 등록/수정 | `src/main/resources/templates/admin/scenarios/form.html` | `src/main/resources/templates/admmgr/scenario/scenarioRegist.html` |
| scenario 상세 | `src/main/resources/templates/admin/scenarios/detail.html` | `src/main/resources/templates/admmgr/scenario/scenarioView.html` |
| scenario 미리보기 | `src/main/resources/templates/admin/scenarios/preview.html` | `src/main/resources/templates/admmgr/scenario/scenarioPreviewLayer.html` |
| 관리자 CSS | 현행 static CSS | `src/main/resources/static/asset/admmgr/style2/css/*` 기준 |
| 관리자 공통 JS | 현행 static JS | `src/main/resources/static/asset/admmgr/js/ADM.Common.js` 기준 |

화면 본문은 8소스의 관리자 화면 폭, 좌측 메뉴, 상단 영역, 콘텐츠 컨테이너, 버튼, 검색, 테이블, 페이지네이션 스타일을 우선 참고한다.

### 2.3 금지 범위

- Java controller/service/mapper/model/dto 패키지 변경 금지
- Controller 클래스명 및 메서드명 변경 금지
- REST JSON URL, 요청/응답 포맷 변경 금지
- SQL XML 파일 이동, namespace 변경, SQL ID 변경 금지
- Flyway migration 추가 또는 DB 스키마 변경 금지
- scenario 기능 필드 삭제, 저장 로직 변경, 권한 정책 변경 금지

### 2.4 검증 명령

```powershell
cd C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT\3.개발\cleverchat
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```powershell
rg "templates/admin/scenarios|admin/scenarios|templates/admmgr/scenario|scenarioList|scenarioRegist|scenarioView|scenarioPreviewLayer" src/main
git diff --name-only
```

수동 확인 대상:

| 화면 | 기대 결과 |
|---|---|
| 관리자 메인 | 8소스 기준 관리자 레이아웃으로 정상 진입 |
| scenario 목록 | 신규 `admmgr/scenario/scenarioList` 템플릿 렌더링 |
| scenario 등록/수정 | 신규 `admmgr/scenario/scenarioRegist` 템플릿 렌더링 |
| scenario 상세 | 신규 `admmgr/scenario/scenarioView` 템플릿 렌더링 |
| scenario 미리보기 | 신규 `admmgr/scenario/scenarioPreviewLayer` 템플릿 렌더링 |

### 2.5 롤백 기준

- `.\mvnw.cmd clean test`에서 기존 대비 신규 실패가 발생한다.
- 애플리케이션 기동 중 `TemplateInputException` 또는 정적 자산 로딩 오류가 발생한다.
- scenario 목록/등록/상세/미리보기 중 하나라도 신규 404 또는 500 오류가 발생한다.
- REST JSON 응답 포맷 또는 URL이 의도치 않게 바뀐다.
- `git diff --name-only`에 관리자 공통 레이아웃/정적 자산/scenario 템플릿 범위를 벗어난 파일이 포함된다.

### 2.6 다음 단계 진입 조건

- Phase 1.A 변경만 포함한 PR 또는 커밋이 검증 명령을 통과한다.
- 모든 scenario 화면이 신규 8소스식 템플릿 경로로 렌더링된다.
- Controller, Java package, SQL XML 변경이 Phase 1.A에 섞이지 않았음이 확인된다.
- PR 본문에 남은 현행 `admin/scenarios` 참조와 제거 계획이 기록된다.

## 3. Phase 1.B 컨트롤러 + 메서드명 정렬

### 3.1 목적

scenario 화면/처리 컨트롤러를 8소스의 `Adm<업무>Controller` 및 액션 접미사 방식으로 정렬한다. 패키지 평탄화와 SQL XML namespace 정렬은 아직 수행하지 않는다.

### 3.2 포함 파일 범위

| 현재 후보 | 8소스 정렬 후보 |
|---|---|
| `ScenarioPageController` | `AdmScenarioController` |
| `ScenarioApiController` | `AdmScenarioApiController` 또는 `AdmScenarioController` 내부 처리 |
| `ScenarioCategoryApiController` | `AdmScenarioCategoryController` |
| `ScenarioKeywordApiController` | `AdmScenarioKeywordController` |

메서드명은 아래 액션명을 우선한다.

| 액션 | 메서드명 후보 | 비고 |
|---|---|---|
| 목록 | `scenarioList` | 목록 화면 |
| 등록/수정 화면 | `scenarioRegist` | 8소스식 `Regist` 통합 우선 |
| 등록/수정 처리 | `scenarioRegistProc` | REST JSON 유지 시 URL은 현행 유지 가능 |
| 상세 | `scenarioView` | 상세 화면 |
| 삭제 진입 또는 화면 | `scenarioDelete` | 필요 시에만 작성 |
| 삭제 처리 | `scenarioDeleteProc` | SQL ID는 Phase 1.D에서 정합화 |
| 미리보기/부분 화면 | `scenarioPreviewLayer` | `Layer` 접미사 적용 |

### 3.3 금지 범위

- Java package root 변경 금지. 현행 `kr.co.cleverchat.domain.scenario.*`는 Phase 1.B에서 유지한다.
- Service/Mapper/Model/DTO 파일 이동 금지
- SQL XML 경로, namespace, SQL ID 변경 금지
- REST JSON URL 및 응답 envelope 변경 금지
- Thymeleaf 템플릿 대규모 구조 변경 금지. Phase 1.A 결과의 반환 문자열 정합화만 허용한다.
- 인증/권한/감사 로그 정책 변경 금지

### 3.4 검증 명령

```powershell
cd C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT\3.개발\cleverchat
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```powershell
rg "class ScenarioPageController|class ScenarioApiController|class ScenarioCategoryApiController|class ScenarioKeywordApiController" src/main/java
rg "class AdmScenario|scenarioList|scenarioRegist|scenarioRegistProc|scenarioView|scenarioDeleteProc|scenarioPreviewLayer" src/main/java src/main/resources/templates
git diff --name-only
```

### 3.5 롤백 기준

- `.\mvnw.cmd clean test`에서 기존 대비 신규 실패가 발생한다.
- Spring bean name 충돌, handler mapping 충돌, `ClassNotFoundException`이 발생한다.
- 기존 관리자 scenario URL 중 Phase 1.B 범위에서 유지하기로 한 URL이 신규 404/405/500 오류를 낸다.
- REST JSON 응답 포맷이 변경된다.
- `git diff --name-only`에 controller 명명 정렬과 직접 연관 없는 패키지 이동 또는 SQL XML 변경이 포함된다.

### 3.6 다음 단계 진입 조건

- `AdmScenario*Controller` 명명과 `scenario*` 메서드명이 적용된다.
- Phase 1.A의 템플릿 반환 문자열과 Controller 메서드가 정합된다.
- 현행 REST JSON 계약 유지 여부가 수동/API 테스트로 확인된다.
- PR 본문에 Phase 1.C에서 이동할 잔여 `kr.co.cleverchat.domain.scenario.*` 파일 목록이 기록된다.

## 4. Phase 1.C `kr.admmgr.<기능별>` 기준 패키지 평탄화

### 4.1 목적

scenario 도메인을 8소스 실제 구조인 `kr.admmgr.<기능별>` 기준으로 평탄화한다. CleverChat의 `kr.co.cleverchat.domain.scenario` 구조는 최종 기준이 아니므로 `kr.admmgr.scenario`로 단계 정렬한다.

### 4.2 포함 파일 범위

| 현재 | 8소스 정렬 후보 |
|---|---|
| `kr.co.cleverchat.domain.scenario.controller` | `kr.admmgr.scenario` |
| `kr.co.cleverchat.domain.scenario.service` | `kr.admmgr.scenario` |
| `kr.co.cleverchat.domain.scenario.mapper` | `kr.admmgr.scenario` 또는 Phase 1.D 이후 점진 제거 |
| `kr.co.cleverchat.domain.scenario.model` | `kr.admmgr.scenario` |
| `kr.co.cleverchat.domain.scenario.dto` | `kr.admmgr.scenario` 또는 REST DTO 예외 유지 |
| `kr.co.cleverchat.domain.scenario.event` | `kr.admmgr.scenario` 또는 명시 호출 헬퍼로 정리 |
| 테스트 패키지 | main 패키지 정렬에 맞춰 최소 범위 정렬 |

공통 영역은 아래 기준을 따른다.

| 현행 후보 | Phase 1.C 기준 |
|---|---|
| `kr.co.cleverchat.common.*` | 최종 기준 아님. `kr.core` 또는 `com` 이동 대상이나 Phase 1.C에서는 scenario 컴파일에 필요한 최소 참조만 조정 |
| `kr.co.cleverchat.domain.auth.*` | Phase 2 대상. Phase 1.C에서 이동 금지 |
| `kr.co.cleverchat.domain.chatbot.*` | Phase 3 대상. Phase 1.C에서 이동 금지 |

### 4.3 금지 범위

- `kr.core`, `com`, `kr.admmgr.member`, `kr.admmgr.security`, `kr.admmgr.chat`, `kr.admmgr.search`로의 대규모 동시 이동 금지
- auth/chatbot/search 도메인 패키지 이동 금지
- SQL XML namespace/ID 변경 금지. Phase 1.D에서 처리한다.
- Mapper 인터페이스 제거 또는 `CDao` 전면 전환 금지. 필요 시 후속 전환 대상으로 표시한다.
- REST DTO 제거, API 계약 변경 금지
- Flyway migration 및 DB 스키마 변경 금지

### 4.4 검증 명령

```powershell
cd C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT\3.개발\cleverchat
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```powershell
rg "kr\.co\.cleverchat\.domain\.scenario|domain/scenario" src/main/java src/test/java
rg "package kr\.admmgr\.scenario|import kr\.admmgr\.scenario" src/main/java src/test/java
rg "kr\.co\.cleverchat\.common|kr\.core|^package com\.|^package kr\.admmgr" src/main/java
git diff --name-only
```

### 4.5 롤백 기준

- `.\mvnw.cmd clean test`에서 기존 대비 신규 실패가 발생한다.
- Spring component scan 누락, MyBatis mapper scan 누락, bean 주입 실패가 발생한다.
- auth/chatbot/search 등 Phase 1.C 범위를 벗어난 도메인 이동이 포함된다.
- REST JSON 계약이나 DTO 필드가 변경된다.
- `kr.co.cleverchat.common`을 무리하게 `kr.core` 또는 `com`으로 이동해 공통 영향 범위가 확대된다.

### 4.6 다음 단계 진입 조건

- scenario 관련 main/test Java 파일이 `kr.admmgr.scenario` 기준으로 컴파일된다.
- `rg "kr\.co\.cleverchat\.domain\.scenario|domain/scenario"` 결과가 없거나, 남은 항목이 기술 예외로 PR 본문에 명시된다.
- auth/chatbot/search/common 영역은 Phase 1.C 범위 밖으로 보존된다.
- Mapper 인터페이스 유지 여부와 Phase 1.D의 SQL XML 정렬 영향 범위가 기록된다.

## 5. Phase 1.D SQL XML namespace/ID 정렬

### 5.1 목적

scenario SQL XML을 8소스의 테이블명 중심 파일명, namespace, SQL ID 규칙으로 정렬한다. Java 패키지 평탄화가 끝난 뒤 수행하며, DB 스키마 변경 없이 MyBatis XML 호출 정합성만 맞춘다.

### 5.2 포함 파일 범위

| 현재 후보 | 8소스 정렬 후보 |
|---|---|
| `src/main/resources/mapper/scenario/ScenarioMapper.xml` | `src/main/resources/sql/postgresql/tb_scenario.xml` |
| `src/main/resources/mapper/scenario/ScenarioVersionMapper.xml` | `src/main/resources/sql/postgresql/tb_scenario_version.xml` |
| `src/main/resources/mapper/scenario/ScenarioNodeMapper.xml` | `src/main/resources/sql/postgresql/tb_scenario_node.xml` |
| `src/main/resources/mapper/scenario/ScenarioNodeOptionMapper.xml` | `src/main/resources/sql/postgresql/tb_scenario_option.xml` |
| `src/main/resources/mapper/scenario/ScenarioCategoryMapper.xml` | `src/main/resources/sql/postgresql/tb_scenario_category.xml` |
| `src/main/resources/mapper/scenario/ScenarioKeywordMapper.xml` | `src/main/resources/sql/postgresql/tb_scenario_keyword.xml` |
| `src/main/resources/mapper/scenario/ScenarioSynonymMapper.xml` | `src/main/resources/sql/postgresql/tb_scenario_synonym.xml` |

namespace는 XML 파일명과 동일한 `tb_scenario*` 형식을 우선한다.

| 용도 | SQL ID |
|---|---|
| 목록 수 | `select_list_cnt` |
| 목록 | `select_list` |
| 상세 | `select_view` |
| 등록 | `insert` |
| 수정 | `update` |
| 논리 삭제 | `delete` |
| 물리 삭제 | `delete_physical` |
| 상태/업무 변경 | `update_<verb>` |
| 이력/로그 적재 | `insert_<verb>` 또는 `insert` |

Mapper 인터페이스 호출 구조를 유지해야 하는 경우에도 신규/정렬 SQL의 파일명, namespace, SQL ID는 8소스 기준을 우선한다. 호출부 전환이 큰 경우에는 동일 PR에서 adapter를 두고 후속 PR로 `CDao` 전환을 분리한다.

### 5.3 금지 범위

- DB 테이블명, 컬럼명, Flyway migration 변경 금지
- SQL의 업무 조건, 권한 조건, soft delete 조건 변경 금지
- REST JSON 응답 필드 변경 금지
- Controller/Service 추가 리네임 금지. Phase 1.B/1.C 결과와 SQL 호출 정합화만 허용한다.
- scenario 외 auth/chatbot/audit SQL XML 정렬 금지
- 성능 튜닝용 인덱스 추가 금지

### 5.4 검증 명령

```powershell
cd C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT\3.개발\cleverchat
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```powershell
rg "namespace=\".*Scenario|Scenario.*Mapper|mapper/scenario|src/main/resources/mapper/scenario" src/main src/main/resources
rg "namespace=\"tb_scenario|id=\"select_list_cnt\"|id=\"select_list\"|id=\"select_view\"|id=\"insert\"|id=\"update\"|id=\"delete\"" src/main/resources
git diff --name-only
```

수동/API 확인 대상:

| 기능 | 기대 결과 |
|---|---|
| scenario 목록 | `select_list_cnt`, `select_list` 호출 정상 |
| scenario 상세 | `select_view` 호출 정상 |
| scenario 등록/수정 | `insert`, `update` 호출 정상 |
| scenario 삭제 | `delete` 또는 `delete_physical` 호출 정상 |
| node/option/category/keyword/synonym | 기존 업무 조건 유지 |

### 5.5 롤백 기준

- `.\mvnw.cmd clean test`에서 기존 대비 신규 실패가 발생한다.
- MyBatis XML 로딩 오류, namespace 중복, statement not found 오류가 발생한다.
- 기존 SQL 결과 건수 또는 주요 정렬 조건이 바뀐다.
- scenario 저장/삭제 API에서 신규 500 오류가 발생한다.
- scenario 외 XML 또는 DB migration 변경이 포함된다.

### 5.6 다음 단계 진입 조건

- `src/main/resources/sql/postgresql/tb_scenario*.xml` 기준으로 scenario SQL XML이 로딩된다.
- `namespace`와 SQL ID가 8소스식 기준으로 정렬된다.
- 기존 scenario 화면/API 수동 확인이 통과한다.
- Mapper 인터페이스 또는 `CDao` 전환 잔여 작업이 후속 PR 항목으로 기록된다.

## 6. Phase 1 전체 완료 기준

Phase 1은 아래 조건을 모두 만족해야 완료로 판단한다.

1. Phase 1.A부터 1.D까지 각 단계 검증 명령이 통과한다.
2. scenario 화면은 `templates/admmgr/scenario/*` 기준으로 동작한다.
3. scenario 컨트롤러와 메서드명은 `AdmScenario*`, `scenario*` 액션명 기준으로 정렬된다.
4. scenario Java package는 `kr.admmgr.scenario` 기준으로 평탄화된다.
5. scenario SQL XML은 `sql/postgresql/tb_scenario*.xml`, `tb_scenario*` namespace, 8소스식 SQL ID 기준으로 정렬된다.
6. CleverChat 현행 `kr.co.cleverchat.domain/common` 구조가 최종 기준이 아니라 단계적 정렬 대상이라는 내용이 PR 본문과 후속 로드맵에 남아 있다.
7. Java 17, Spring Boot 3, Jakarta, REST JSON, HandlerInterceptor + 세션 VO, 필요한 DTO/Mapper 인터페이스 등 기술 예외는 `결정사항.md` §11.3 범위 안에서만 유지된다.

## 7. 커밋 및 PR 양식

단계별 커밋 메시지 후보:

```text
chore(phase1a): align admin layout and scenario templates
chore(phase1b): align scenario controller action names
chore(phase1c): flatten scenario package to kr.admmgr
chore(phase1d): align scenario sql xml ids
```

Phase 1 통합 커밋 후보:

```text
chore(phase1): align scenario with 8source naming
```

PR 본문 필수 섹션:

```markdown
## 8소스 기준
- 원본 베이스: 8.소스/OverseasNPP_20260511
- 목표 구조: kr.core, kr.admmgr, kr.admmgr.<기능별>, com
- CleverChat 현행 kr.co.cleverchat.domain/common 구조는 최종 기준이 아니라 단계적 정렬 대상

## 단계별 변경 범위
- Phase 1.A: 관리자 공통 레이아웃 + templates/admmgr/scenario/* 정렬
- Phase 1.B: AdmScenario*Controller + scenario* 액션명 정렬
- Phase 1.C: kr.admmgr.scenario 기준 패키지 평탄화
- Phase 1.D: sql/postgresql/tb_scenario*.xml namespace/ID 정렬

## 유지한 기술 예외
- Java 17/Spring Boot 3/Jakarta
- REST JSON 응답 포맷
- HandlerInterceptor + 세션 VO 인증
- @Audited 감사 트레일
- 필요한 DTO와 Mapper 인터페이스 점진 유지

## 검증
- [ ] .\mvnw.cmd clean test
- [ ] .\mvnw.cmd spring-boot:run
- [ ] scenario 목록/등록/상세/미리보기 수동 확인
- [ ] scenario 저장/삭제 REST JSON 응답 확인
- [ ] 옛 경로/패키지/Mapper 참조 rg 결과 확인
- [ ] 변경 범위 위반 없음

## 롤백 기준
- 테스트 신규 실패
- 템플릿 경로 오류
- Spring bean/handler mapping 오류
- MyBatis XML 로딩 오류 또는 statement not found
- 화면/API 신규 404/500
- REST JSON 포맷 변경
- DB migration 또는 범위 외 도메인 변경 포함
```

## 8. 후속 PR 로드맵

Phase 1 이후에도 같은 원칙을 도메인별로 반복한다.

| Phase | 도메인 | 핵심 정렬 항목 | 선행 의존 |
|---|---|---|---|
| Phase 2 | auth | `domain.auth`를 `kr.admmgr.member` + `kr.admmgr.security`로 정렬, `templates/admmgr/member/` 로그인/계정 화면 정렬, 관리자 세션/접속 로그 8소스 기준 검토 | Phase 1 공통 레이아웃 |
| Phase 3 | chatbot | 운영자/관리자 기능은 `kr.admmgr.ai` + `kr.admmgr.chat` 기준으로 정렬, 별도 사용자 root는 후속 예외 결정 전까지 도입하지 않음, `templates/admmgr/chat/`, `tb_chat_*.xml` 정렬 | Phase 2 인증 정렬 |
| Phase 4 | search | M4 신규 도메인. 처음부터 `kr.admmgr.search`, `templates/admmgr/search/`, `tb_search_*.xml` 기준으로 작성 | Phase 3 |

후속 PR도 `결정사항.md` §11.3 기술 예외를 제외한 항목은 8소스 방식을 따른다.
