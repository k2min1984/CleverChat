# Phase 1.B Scenario Controller Naming 8소스 작업지시서

> 작성일: 2026-05-26  
> 대상 프로젝트: `3.개발/cleverchat`  
> 기준 소스: `8.소스/OverseasNPP_20260511`  
> 선행 문서: `3.개발/작업지시서/Phase1_명명정렬_작업지시서.md`, `3.개발/cleverchat/docs/Phase1A_Admin_Layout_Template_8소스_작업지시.md`  
> 작업 성격: Codex 위임용 작업지시서. 본 문서는 코드 수정 없이 Phase 1.B 구현 지시를 정리한다.

## 1. 목적

Phase 1.B의 목적은 scenario 도메인 관리자 컨트롤러를 8소스 기준 명명 규칙에 맞춰 최종 점검하고, 필요한 경우 최소 범위로 정렬하는 것이다.

정렬 기준은 `Adm<업무>Controller` 클래스명과 `<entity><Action>(Proc)` 메서드명이다. 화면 컨트롤러는 Thymeleaf view 반환명을 Phase 1.A의 `admmgr/scenario/*` 템플릿명과 맞추고, API 컨트롤러는 현행 URL과 REST 응답 envelope를 유지한 채 메서드명만 점검한다.

현행 점검 기준으로 클래스명, 주요 메서드명, view 반환명은 대부분 8소스 기준에 맞춰져 있다. 잔존 검토 지점은 `scenarioModify` / `scenarioModifyProc` 분리 형태 1건이다. 본 Phase에서는 기존 동작 보존을 우선하며, 이 분리를 강제로 통합하지 않는다. 통합이 필요하다고 판단되면 후속 PR 후보로 기록한다.

## 2. 범위

### 2.1 포함 범위

| 구분 | 대상 |
|---|---|
| 관리자 화면 컨트롤러 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java` |
| 관리자 scenario API | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioApiController.java` |
| 관리자 category API | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioCategoryController.java` |
| 관리자 keyword API | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioKeywordController.java` |
| scenario 템플릿 | `3.개발/cleverchat/src/main/resources/templates/admmgr/scenario/scenarioList.html` |
| scenario 템플릿 | `3.개발/cleverchat/src/main/resources/templates/admmgr/scenario/scenarioRegist.html` |
| scenario 템플릿 | `3.개발/cleverchat/src/main/resources/templates/admmgr/scenario/scenarioView.html` |
| scenario 템플릿 | `3.개발/cleverchat/src/main/resources/templates/admmgr/scenario/scenarioPreviewLayer.html` |
| 공통 UI 참조 | `sidebar.html`, `navigation.html` 등 scenario URL 또는 view명을 참조하는 관리자 공통 템플릿 |
| 테스트 | controller 명명 변경으로 컴파일 또는 라우팅 영향을 받는 기존 테스트 |

### 2.2 허용 작업

1. `AdmScenario*Controller` 클래스명 유지 여부 점검
2. `scenarioList`, `scenarioRegist`, `scenarioRegistProc`, `scenarioView`, `scenarioDeleteProc`, `scenarioPreviewLayer` 등 8소스식 메서드명 점검
3. `scenarioCategoryList`, `scenarioCategoryRegistProc`, `scenarioCategoryModifyProc` 등 category 메서드명 점검
4. `scenarioKeywordList`, `scenarioKeywordModifyProc` 등 keyword 메서드명 점검
5. `return "admmgr/scenario/scenarioList"` 등 view 반환 문자열 점검
6. 템플릿의 form action, link, JavaScript fetch URL이 기존 관리자 URL과 맞는지 점검
7. 명명만 바꾸는 경우에 한해 영향 받는 테스트 import, 메서드 참조, MockMvc 기대값 수정

## 3. 금지 범위

아래 항목은 Phase 1.B에서 수행하지 않는다.

| 금지 항목 | 사유 / 후속 Phase |
|---|---|
| Java package root 이동 | `kr.co.cleverchat.domain.scenario.*`에서 `kr.admmgr.scenario`로의 이동은 Phase 1.C |
| SQL XML 경로, namespace, SQL ID 변경 | Phase 1.D |
| MyBatis mapper 구조 변경 | Phase 1.D 또는 별도 mapper 정렬 작업 |
| 관리자 URL 트리 변경 | 기존 `/admin/scenarios`, `/admin/api/scenarios`, `/admin/api/scenario-categories`, `/admin/api/scenarios/{scenarioId}/keywords` 유지 |
| REST 응답 envelope 변경 | `ApiResponse` 계약 유지 |
| 요청/응답 DTO 필드 변경 | 화면/API 계약 보존 |
| DB/Flyway 변경 | 명명 정렬과 무관 |
| 인증/권한/감사 정책 변경 | auth/security Phase 대상 |
| 타 도메인 변경 | auth, chatbot, search, admin 공통 도메인 확장 금지 |
| 대규모 템플릿 재작성 | Phase 1.A 범위를 넘어서는 UI 개편 금지 |

## 4. 8소스 명명 기준

### 4.1 클래스명 기준

8소스 관리자 업무 컨트롤러는 `Adm<업무>Controller` 형태를 우선한다. CleverChat scenario 도메인은 아래 명칭을 기준으로 한다.

| 역할 | 기준 클래스명 | 현행 기대 상태 |
|---|---|---|
| scenario 화면 컨트롤러 | `AdmScenarioController` | 유지 |
| scenario API 컨트롤러 | `AdmScenarioApiController` | 유지 |
| scenario category API 컨트롤러 | `AdmScenarioCategoryController` | 유지 |
| scenario keyword API 컨트롤러 | `AdmScenarioKeywordController` | 유지 |

`ScenarioPageController`, `ScenarioApiController`, `ScenarioCategoryApiController`, `ScenarioKeywordApiController` 같은 Phase 이전 명칭이 남아 있으면 제거 또는 리네임 대상이다. 단, 실제 파일명 변경이 필요할 때만 변경하고, 이미 존재하지 않으면 무변경 점검으로 기록한다.

### 4.2 메서드명 기준

메서드명은 `<entity><Action>` 또는 `<entity><Action>Proc` 형태를 따른다.

| 액션 | 화면 메서드 | 처리/API 메서드 | 비고 |
|---|---|---|---|
| 목록 | `scenarioList` | `scenarioList` | 화면/API 모두 허용 |
| 등록 화면 | `scenarioRegist` | - | 8소스의 `Regist` 표기 유지 |
| 등록 처리 | - | `scenarioRegistProc` | POST 처리 |
| 상세 | `scenarioView` | `scenarioView` | 조회 화면/API |
| 수정 화면 | `scenarioModify` 또는 `scenarioRegist` 통합 | - | 현행 분리 유지 가능 |
| 수정 처리 | - | `scenarioModifyProc` | 현행 URL 보존 |
| 삭제 처리 | - | `scenarioDeleteProc` | API 삭제 |
| 버전 등록 | - | `scenarioVersionRegistProc` | 버전 생성 |
| 그래프 수정 | - | `scenarioGraphModifyProc` | API graph 저장 |
| 게시 | - | `scenarioPublishProc` | publish 처리 |
| 활성화 | - | `scenarioActivateProc` | activate 처리 |
| 비활성화 | - | `scenarioDeactivateProc` | deactivate 처리 |
| 미리보기 레이어 | `scenarioPreviewLayer` | - | `*Layer` 접미사 |

`scenarioModify` / `scenarioModifyProc`는 `scenarioRegist` / `scenarioRegistProc`로 무리하게 합치지 않는다. 현행 라우팅과 사용자 동작이 안정적이면 유지하고, "8소스식 등록/수정 통합 검토"를 후속 이슈로 남긴다.

### 4.3 category / keyword 메서드 기준

| 컨트롤러 | 기준 메서드 |
|---|---|
| `AdmScenarioCategoryController` | `scenarioCategoryList`, `scenarioCategoryRegistProc`, `scenarioCategoryModifyProc` |
| `AdmScenarioKeywordController` | `scenarioKeywordList`, `scenarioKeywordModifyProc` |

삭제, 상세, 정렬 변경 기능이 현행에 없으면 신규 기능을 만들지 않는다. 명명 정렬은 존재하는 기능에 한정한다.

### 4.4 view 반환명 기준

화면 컨트롤러의 Thymeleaf 반환명은 아래 기준을 따른다.

| 화면 | 기준 반환명 | 물리 파일 |
|---|---|---|
| scenario 목록 | `admmgr/scenario/scenarioList` | `templates/admmgr/scenario/scenarioList.html` |
| scenario 등록/수정 | `admmgr/scenario/scenarioRegist` | `templates/admmgr/scenario/scenarioRegist.html` |
| scenario 상세 | `admmgr/scenario/scenarioView` | `templates/admmgr/scenario/scenarioView.html` |
| scenario 미리보기 레이어 | `admmgr/scenario/scenarioPreviewLayer` | `templates/admmgr/scenario/scenarioPreviewLayer.html` |

`admin/scenarios/list`, `admin/scenarios/form`, `admin/scenarios/detail`, `admin/scenarios/preview` 반환명이 남아 있으면 Phase 1.A 이전 잔존 참조로 보고 정리한다.

### 4.5 URL 기준

URL은 Phase 1.B에서 바꾸지 않는다. 명명 정렬 대상은 클래스명, 메서드명, view 반환명이다.

| 구분 | 유지 URL |
|---|---|
| scenario 화면 | `/admin/scenarios` |
| scenario API | `/admin/api/scenarios` |
| scenario category API | `/admin/api/scenario-categories` |
| scenario keyword API | `/admin/api/scenarios/{scenarioId}/keywords` |

`/admmgr/scenario` 같은 8소스식 URL로 변경하지 않는다. URL 트리 변경은 별도 승인 없이는 금지한다.

### 4.6 매핑표

| 대상 | 현재 기대명 | 8소스 기준 판단 | 조치 |
|---|---|---|---|
| `AdmScenarioController` | `scenarioList` | 부합 | 유지 |
| `AdmScenarioController` | `scenarioRegist` | 부합 | 유지 |
| `AdmScenarioController` | `scenarioRegistProc` | 부합 | 유지 |
| `AdmScenarioController` | `scenarioView` | 부합 | 유지 |
| `AdmScenarioController` | `scenarioModify` | 잔존 편차 가능 | 현행 유지, 후속 통합 검토 기록 |
| `AdmScenarioController` | `scenarioModifyProc` | 잔존 편차 가능 | 현행 유지, 후속 통합 검토 기록 |
| `AdmScenarioController` | `scenarioPreviewLayer` | 부합 | 유지 |
| `AdmScenarioApiController` | `scenarioDeleteProc` | 부합 | 유지 |
| `AdmScenarioCategoryController` | `scenarioCategory*` | 부합 | 유지 |
| `AdmScenarioKeywordController` | `scenarioKeyword*` | 부합 | 유지 |

### 4.7 무변경 점검 PR 허용

실제 점검 결과 모든 항목이 이미 기준에 부합하면, 본 PR은 코드 변경 없이 "무변경 점검 PR"로 종결할 수 있다.

무변경 점검 PR일 때는 PR 본문 또는 작업 로그에 아래를 반드시 남긴다.

1. 점검한 대상 파일 목록
2. 잔존 편차 여부
3. `scenarioModify` / `scenarioModifyProc` 유지 사유
4. 실행한 검증 명령과 결과
5. 후속 PR 후보

## 5. 작업 절차

1. `git status --short`로 기존 변경사항을 확인한다. 사용자 또는 다른 에이전트의 기존 변경은 되돌리지 않는다.
2. 대상 컨트롤러 4종을 열어 클래스명, `@RequestMapping`, 메서드명을 확인한다.
3. scenario 템플릿 4종을 열어 form action, link, fetch URL, view명 참조를 확인한다.
4. sidebar/navigation 등 관리자 공통 템플릿에서 scenario URL 또는 예전 view 경로 참조가 남았는지 확인한다.
5. 아래 잔존 참조 검사 명령을 실행한다.
6. 변경이 필요한 경우 controller/view명 참조만 최소 수정한다.
7. 변경이 불필요하면 무변경 점검 PR로 정리한다.
8. 검증 명령을 실행하고 결과를 PR 본문에 기록한다.

## 6. 검증 절차

Windows 기준:

```powershell
cd C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT\3.개발\cleverchat
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

WSL/bash 기준:

```bash
cd /mnt/c/02.Project/02.자바/01.WorkSpace/CLEVERCHAT/3.개발/cleverchat
./mvnw clean test
./mvnw spring-boot:run
```

잔존 참조 검사는 프로젝트 루트 `3.개발/cleverchat`에서 실행한다.

```bash
rg "class ScenarioPageController|class ScenarioApiController|class ScenarioCategoryApiController|class ScenarioKeywordApiController" src/main/java src/test/java
rg "admin/scenarios/list|admin/scenarios/form|admin/scenarios/detail|admin/scenarios/preview" src/main/java src/main/resources/templates src/test/java
rg "admmgr/scenario/scenarioList|admmgr/scenario/scenarioRegist|admmgr/scenario/scenarioView|admmgr/scenario/scenarioPreviewLayer" src/main/java src/main/resources/templates src/test/java
rg "scenarioList|scenarioRegist|scenarioRegistProc|scenarioView|scenarioModify|scenarioModifyProc|scenarioDeleteProc|scenarioPreviewLayer" src/main/java src/main/resources/templates src/test/java
rg "scenarioCategoryList|scenarioCategoryRegistProc|scenarioCategoryModifyProc|scenarioKeywordList|scenarioKeywordModifyProc" src/main/java src/test/java
rg "/admmgr/scenario|/admin/scenario[^s]" src/main/java src/main/resources/templates src/test/java
```

수동 확인 대상:

| 화면/API | 기대 결과 |
|---|---|
| `GET /admin/scenarios` | 목록 화면 렌더링 |
| `GET /admin/scenarios/new` | 등록 화면 렌더링 |
| `POST /admin/scenarios` | 등록 처리 후 상세 redirect |
| `GET /admin/scenarios/{id}` | 상세 화면 렌더링 |
| `GET /admin/scenarios/{id}/edit` | 수정 화면 렌더링 |
| `POST /admin/scenarios/{id}` | 수정 처리 후 상세 redirect |
| `GET /admin/scenarios/versions/{versionId}/preview` | 미리보기 레이어 렌더링 |
| `/admin/api/scenarios*` | `ApiResponse` envelope 유지 |
| `/admin/api/scenario-categories*` | `ApiResponse` envelope 유지 |
| `/admin/api/scenarios/{scenarioId}/keywords` | `ApiResponse` envelope 유지 |

## 7. 완료 기준

1. 컨트롤러 4종의 클래스명이 `AdmScenario*Controller` 기준에 맞는다.
2. 주요 메서드명이 `<entity><Action>(Proc)` 기준에 맞는다.
3. 화면 반환명이 `admmgr/scenario/scenario*` 기준에 맞는다.
4. 예전 controller 클래스명과 예전 `admin/scenarios/*` view 반환명 잔존 참조가 없다.
5. URL 트리, REST envelope, DTO, DB, mapper, package root가 변경되지 않았다.
6. `mvnw clean test`가 통과한다.
7. `spring-boot:run`으로 기동 가능하다.
8. 변경이 없을 경우 무변경 점검 PR로 종결 가능하며, 후속 검토 항목이 기록되어 있다.

## 8. 롤백 기준

아래 중 하나라도 발생하면 Phase 1.B 변경을 롤백하거나 무변경 점검으로 전환한다.

1. `mvnw clean test`에서 기존 대비 신규 실패가 발생한다.
2. Spring handler mapping 충돌, bean name 충돌, `ClassNotFoundException`, `TemplateInputException`이 발생한다.
3. 기존 관리자 scenario URL에서 신규 404, 405, 500 오류가 발생한다.
4. `ApiResponse` envelope 또는 JSON 필드 구조가 변경된다.
5. `git diff --name-only`에 SQL XML, Flyway, DB, package root 이동, 타 도메인 파일이 포함된다.
6. 단순 명명 정렬을 넘는 UI 재작성 또는 기능 변경이 섞인다.

## 9. 커밋 후보

변경 발생 시:

```text
chore(phase1b): align scenario controller method names to 8source
```

점검만 수행한 경우:

```text
chore(phase1b): verify scenario controller naming aligned with 8source
```

분리 옵션:

```text
chore(phase1b): order scenario controller methods by 8source action flow
chore(phase1b): align scenario template form action references
docs(phase1b): record scenarioModify integration as follow-up
```

## 10. 후속 후보

| 후보 | 내용 |
|---|---|
| Phase 1.B 후속 | `scenarioModify` / `scenarioModifyProc`를 8소스식 `scenarioRegist` 통합 흐름으로 합칠지 검토 |
| Phase 1.C | `kr.co.cleverchat.domain.scenario.*`를 `kr.admmgr.scenario` 기준으로 패키지 정렬 |
| Phase 1.D | scenario SQL XML namespace/ID를 8소스 기준으로 정렬 |
| 관리자 URL 정책 | `/admin/scenarios` 유지 또는 8소스식 관리자 URL 별도 전환 여부 결정 |
