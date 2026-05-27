# P0 Scenario Graph Edit View 구현 PR 작업지시서

> 작성일: 2026-05-26  
> 대상 프로젝트: `3.개발/cleverchat`  
> 기준 문서: `3.개발/작업지시서/P0_Scenario_Graph_Edit_View_작업지시.md` §12  
> 작업 성격: Codex 구현 PR 지시서  
> 원칙: 본 문서는 구현 지시 문서다. 이 문서 작성 작업에서는 코드 수정 금지. 후속 구현 PR에서만 코드, 테스트, 설계 문서를 수정한다.

## 1. 목표

`P0_Scenario_Graph_Edit_View_작업지시.md` §12의 구현 분해 1~9단계를 기준으로 DRAFT 시나리오 버전의 graph 편집 화면을 구현한다.

완성 흐름:

```text
목록 -> 상세 -> 새초안 -> 편집 -> graph 조회 -> graph 저장 -> 미리보기 -> 게시
```

후속 구현 PR은 아래 두 URL을 확정 구현한다.

| Method | Path | 목적 |
|---|---|---|
| GET | `/admin/scenarios/{scenarioId}/versions/{versionId}/graph` | DRAFT 버전 graph 편집 화면 |
| GET | `/admin/api/scenarios/versions/{versionId}/graph` | 편집 화면 초기 graph JSON 조회 |

저장은 기존 API를 유지한다.

| Method | Path | 목적 |
|---|---|---|
| PUT | `/admin/api/scenarios/versions/{versionId}/graph` | DRAFT graph 저장 |

## 2. 구현 원칙

| 항목 | 지시 |
|---|---|
| 범위 | scenario 도메인 화면, API, 서비스, 테스트, 관련 설계 문서만 수정 |
| 금지 | DB/Flyway 변경, 인증/권한 정책 변경, `ApiResponse` envelope 변경, 대규모 리팩토링, 외부 CDN/외부 전송 추가 |
| 상태 규칙 | 편집 화면 진입과 저장은 `scenario_version.status=DRAFT`만 허용 |
| 조회 규칙 | graph JSON 조회 API는 저장된 graph를 반환한다. PUBLISHED/ARCHIVED 조회는 허용하되 편집 화면 진입과 저장은 차단 |
| 빈 graph | 저장된 노드가 없어도 200과 빈 payload를 반환해 화면에서 첫 노드 생성을 시작할 수 있게 한다 |
| CSRF | `head.html` meta, `ADM.Common.js`의 `X-CSRF-Token`/`X-CSRF-FormId` 규약을 재사용 |
| 설정 | `application.yml`, `WebMvcConfig`, CSRF 인터셉터 정책은 변경하지 않는 것을 기본값으로 한다 |

## 3. PR 전 사전 점검

구현 착수 전 아래 항목을 먼저 확인하고, 결과를 PR 본문에 남긴다.

### 3.1 상세 화면 구조

- `scenarioView.html`의 버전 목록 markup과 작업 버튼 위치를 확인한다.
- 기존 `새 초안`, `미리보기`, `게시`, `활성화` 버튼의 조건과 form/action 방식을 파악한다.
- DRAFT 행에만 `편집` 링크를 추가할 수 있는 최소 위치를 정한다.

### 3.2 화면 컨트롤러 구조

- `AdmScenarioController.java`의 목록, 상세, 등록, 수정, preview, publish route 패턴을 확인한다.
- 신규 route는 `GET /admin/scenarios/{scenarioId}/versions/{versionId}/graph`로 추가한다.
- route 추가 시 기존 preview route인 `/admin/scenarios/versions/{versionId}/preview`와 충돌하지 않게 한다.

### 3.3 API 컨트롤러 구조

- `AdmScenarioApiController.java`의 기존 graph 저장 API `PUT /versions/{versionId}/graph`를 확인한다.
- 신규 graph 조회 API는 같은 컨트롤러에 `GET /versions/{versionId}/graph`로 추가한다.
- 응답 envelope는 기존 `ApiResponse.ok(...)` 패턴을 따른다.

### 3.4 서비스 graph 저장 구조

- `ScenarioService.saveGraph(versionId, request)`의 트랜잭션, 상태 검증, node/option 삭제 후 재삽입 흐름을 확인한다.
- 기존 private helper가 있더라도 화면 조회용으로 무리하게 public 노출하지 않는다.
- 권장 구현은 `graph(Long versionId)` public read method를 추가하고, 내부에 `readPersistedGraph(versionId)` private helper를 둔다.

### 3.5 DTO 구조

- `ScenarioGraphDtos.SaveRequest`, node DTO, option DTO 필드명을 확인한다.
- 신규 조회 응답은 가능하면 저장 요청 DTO와 같은 JSON 구조를 반환한다.
- 화면 초기화에 필요한 `startNodeKey`, `nodes`, `options`, `metadata`가 누락되지 않게 한다.

### 3.6 mapper와 XML

- `ScenarioNodeMapper`, `ScenarioNodeOptionMapper`와 XML의 조회 SQL을 확인한다.
- 기존 조회 메서드로 충분하면 SQL ID를 추가하지 않는다.
- 추가가 필요하면 versionId 기준 node 목록과 nodeId 기준 option 목록만 좁게 추가한다.

### 3.7 오류 처리

- DRAFT가 아닌 버전의 편집 화면 GET은 409 Conflict와 `STATE_CONFLICT`로 차단한다.
- DRAFT가 아닌 버전의 PUT 저장은 기존 저장 검증을 유지한다.
- 존재하지 않는 scenario/version, scenarioId-versionId 불일치는 404 또는 기존 예외 정책을 따른다.

### 3.8 CSRF

- 편집 템플릿이 `admmgr/common/head.html`을 포함해 `csrfToken`, `csrfFormId` meta가 렌더링되는지 확인한다.
- AJAX 저장 호출은 `X-CSRF-Token`, `X-CSRF-FormId` 헤더를 붙인다.
- `ADM.Common.js`의 기존 helper를 쓸 수 있으면 재사용하고, 불가능하면 같은 규약의 작은 local helper만 둔다.

### 3.9 `application.yml` 영향

- 신규 URL과 static JS 추가에 `application.yml` 변경이 필요한지 확인한다.
- 원칙적으로 변경 없음이어야 한다.
- 변경 필요성이 생기면 PR에서 사유와 영향 범위를 별도 설명하고, 이 P0 범위에 꼭 필요한지 재검토한다.

### 3.10 `WebMvcConfig` 영향

- `/admin/scenarios/**`, `/admin/api/scenarios/**`, static asset 경로가 기존 interceptor 정책에 포함되는지 확인한다.
- 원칙적으로 `WebMvcConfig` 변경 없음이어야 한다.
- 변경 없이 동작하지 않으면 먼저 route/static path 선택이 기존 정책과 어긋난 것은 아닌지 점검한다.

### 3.11 테스트 기반

- 기존 scenario controller/service 테스트 유무와 테스트 fixture 작성 방식을 확인한다.
- 신규 테스트는 `AdmScenarioControllerTest`, `AdmScenarioApiControllerTest` 중심으로 작성한다.
- validation 자체는 기존 `ScenarioGraphValidatorTest`와 중복하지 말고 controller/API 계약 중심으로 보강한다.

### 3.12 문서 동기화 대상

- 아래 문서의 해당 절만 보강한다.
- 전체 재작성, 표기 통일 목적의 대량 변경, unrelated 정리는 금지한다.

| 문서 | 보강 범위 |
|---|---|
| `2.설계/01.화면설계서/화면목록.md` | graph 편집 화면 추가 |
| `2.설계/03.API설계서/API목록.md` | 신규 graph 조회 API 추가 |
| `2.설계/03.API설계서/M2_시나리오API.md` | graph 편집 화면 URL, GET graph API, PUT 저장 API 흐름 보강 |
| `2.설계/02.DB설계서/M2_시나리오ERD.md` | DB 변경 없음, 기존 node/option 재사용 명시 |
| `2.설계/05.보안설계서/보안체크리스트.md` | 관리자 권한, AJAX CSRF, 외부 전송 없음 확인 |
| `2.설계/01.화면설계서/접근성체크리스트.md` | label, keyboard, 오류 메시지 연결 기준 추가 |
| `2.설계/06.운영메모/M2_운영메모.md` | 저장, 미리보기, 게시 전 확인 절차 추가 |
| `4.테스트/02.테스트시나리오/M2_시나리오테스트.md` | P0-GE-TC-001~012 추가 또는 매핑 |

## 4. 구현 단계

### S1. 상세 화면 DRAFT 편집 링크 추가

기준: §12 1단계

수정 파일:

- `src/main/resources/templates/admmgr/scenario/scenarioView.html`

작업:

1. 버전 목록의 작업 열에 DRAFT 전용 `편집` 링크를 추가한다.
2. 링크 URL은 `/admin/scenarios/{scenarioId}/versions/{versionId}/graph`를 사용한다.
3. `PUBLISHED`, `ARCHIVED` 버전에는 편집 링크를 렌더링하지 않는다.
4. 기존 `미리보기`, `게시`, `활성화`, `새 초안` 동작과 CSRF hidden fragment는 변경하지 않는다.

수용 기준:

- DRAFT 행에만 편집 링크가 보인다.
- 링크 클릭 시 신규 graph 편집 화면 route로 이동한다.
- 기존 상세 화면 기능은 회귀하지 않는다.

### S2. graph 편집 화면 GET route 추가

기준: §12 2단계

수정 파일:

- `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java`

작업:

1. `@GetMapping("/{scenarioId}/versions/{versionId}/graph")`를 추가한다.
2. scenarioId와 versionId 소속 관계를 검증한다.
3. version status가 `DRAFT`가 아니면 409 Conflict와 `STATE_CONFLICT`로 차단한다.
4. model에는 화면 렌더링에 필요한 최소 식별 정보를 담는다.
   - `scenarioId`
   - `versionId`
   - scenario title
   - version number
   - version status
   - 상세 복귀 URL
   - preview URL
   - graph GET API URL
   - graph PUT API URL
5. view name은 `admmgr/scenario/scenarioGraphEdit`를 반환한다.

수용 기준:

- DRAFT version은 200으로 편집 화면을 렌더링한다.
- PUBLISHED/ARCHIVED version 직접 접근은 409로 차단한다.
- scenarioId와 versionId가 불일치하면 기존 not found 정책으로 차단한다.

### S3. graph 조회 service/model 구성

기준: §12 3단계

수정 파일 후보:

- `src/main/java/kr/co/cleverchat/domain/scenario/service/ScenarioService.java`
- `src/main/java/kr/co/cleverchat/domain/scenario/dto/ScenarioGraphDtos.java`
- mapper interface/XML은 필요할 때만 최소 수정

작업:

1. `ScenarioService.graph(Long versionId)`를 추가한다.
2. 저장된 node/option을 읽어 `ScenarioGraphDtos.SaveRequest`와 호환되는 구조로 반환한다.
3. 저장된 graph가 없으면 아래 형태의 빈 graph를 반환한다.

```json
{
  "startNodeKey": null,
  "nodes": []
}
```

4. metadata는 저장 API가 기대하는 문자열 또는 기존 DTO 타입에 맞춰 그대로 반환한다.
5. node sortOrder, option sortOrder 기준 정렬을 보장한다.
6. 조회 메서드는 데이터 변경을 하지 않는다.

권장 구조:

```text
ScenarioService.graph(versionId)
  -> version 존재 확인
  -> readPersistedGraph(versionId)
  -> startNodeKey 계산
  -> SaveRequest 호환 DTO 반환
```

수용 기준:

- 빈 DRAFT version도 graph 조회 API에서 200을 반환한다.
- 저장된 graph는 저장 API payload와 같은 구조로 재조회된다.
- 조회만으로 node/option 데이터가 변경되지 않는다.

### S4. graph 조회 API 추가

기준: §12 3단계와 §4.4 확정 사항

수정 파일:

- `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioApiController.java`

작업:

1. `@GetMapping("/versions/{versionId}/graph")`를 추가한다.
2. `ScenarioService.graph(versionId)` 결과를 `ApiResponse.ok(...)`로 반환한다.
3. DRAFT, PUBLISHED, ARCHIVED 모두 조회는 허용한다.
4. 없는 version은 기존 not found 응답 정책을 따른다.

수용 기준:

- `GET /admin/api/scenarios/versions/{versionId}/graph`가 200과 graph JSON을 반환한다.
- PUT 저장 API의 URL과 method는 변경하지 않는다.
- 공통 API envelope가 유지된다.

### S5. graph 편집 템플릿 추가

기준: §12 4단계

신규 파일:

- `src/main/resources/templates/admmgr/scenario/scenarioGraphEdit.html`

작업:

1. 기존 관리자 화면 layout, head fragment, 스타일 class 사용 방식을 따른다.
2. 화면 상단에 시나리오 제목, version number, status를 표시한다.
3. 필수 UI를 제공한다.
   - 시작 노드 선택
   - 노드 목록 편집
   - 옵션 목록 편집
   - 저장
   - 미리보기
   - 상세 돌아가기
4. P0에서는 표/폼 기반 editor로 구현한다. canvas/drag-drop은 구현하지 않는다.
5. metadata는 raw JSON textarea로 허용하되 invalid JSON은 저장 전 차단한다.
6. 화면에서 필요한 URL과 ID는 `data-*` 속성 또는 hidden input으로 전달한다.

수용 기준:

- 신규 화면은 단독 진입 시 필요한 정보가 렌더링된다.
- 키보드만으로 주요 입력과 저장 버튼에 접근할 수 있다.
- label 또는 aria-label이 없는 입력 컨트롤을 만들지 않는다.

### S6. 저장 호출 JS 연결

기준: §12 5단계와 6단계

신규 파일 후보:

- `src/main/resources/static/asset/admmgr/style2/js/ADM.ScenarioGraphEdit.js`

템플릿 수정:

- `scenarioGraphEdit.html`에서 신규 JS를 로드한다.

작업:

1. 화면 진입 시 `GET /admin/api/scenarios/versions/{versionId}/graph`로 초기 graph를 적재한다.
2. 빈 graph이면 첫 node를 추가할 수 있는 빈 상태 UI를 표시한다.
3. 저장 시 현재 화면 상태를 아래 payload로 직렬화한다.

```json
{
  "startNodeKey": "start",
  "nodes": [
    {
      "nodeKey": "start",
      "nodeType": "QUESTION",
      "title": "문의 유형 선택",
      "content": "무엇을 도와드릴까요?",
      "sortOrder": 1,
      "metadata": "{}",
      "options": [
        {
          "label": "가입 방법",
          "nextNodeKey": "join-guide",
          "conditionExpr": null,
          "sortOrder": 1,
          "enabled": true
        }
      ]
    }
  ]
}
```

4. `PUT /admin/api/scenarios/versions/{versionId}/graph` 호출 시 `Content-Type: application/json`을 사용한다.
5. AJAX 요청에는 `X-CSRF-Token`, `X-CSRF-FormId`를 포함한다.
6. 응답 헤더의 신규 CSRF 값이 있으면 meta 값을 갱신한다.
7. validation/API 오류는 화면 상단 또는 필드 근처에 표시한다.
8. 저장 성공 후 같은 화면에 머무르며 저장 성공 메시지를 표시하고, 미리보기 버튼을 사용할 수 있게 한다.

수용 기준:

- 저장 호출이 기존 PUT API에 JSON으로 들어간다.
- CSRF 누락으로 403/`CSRF_INVALID`가 발생하지 않는다.
- 저장 실패 메시지를 alert만으로 끝내지 않고 화면에서도 확인 가능하다.

### S7. 미리보기/게시 연결

기준: §12 7단계

수정 파일:

- `scenarioGraphEdit.html`
- 필요 시 `ADM.ScenarioGraphEdit.js`

작업:

1. 미리보기 버튼은 기존 `/admin/scenarios/versions/{versionId}/preview`를 사용한다.
2. 게시는 기존 상세 화면의 publish form을 우선 유지한다.
3. 편집 화면에서 게시 버튼을 제공할 경우 기존 `POST /admin/scenarios/versions/{versionId}/publish`와 CSRF hidden 방식을 재사용한다.
4. 저장 전 미리보기 클릭 시 현재 미저장 변경이 있음을 알려준다.

수용 기준:

- 저장된 graph는 preview에서 시작 노드와 option 흐름을 확인할 수 있다.
- 게시 업무 규칙은 기존 service 정책을 따른다.
- 편집 화면 추가로 기존 상세 화면 게시가 깨지지 않는다.

### S8. 테스트 추가

기준: §12 8단계

신규 테스트 파일:

- `src/test/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioControllerTest.java`
- `src/test/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioApiControllerTest.java`

필수 테스트 매핑:

| ID | 테스트 위치 | 검증 |
|---|---|---|
| P0-GE-TC-001 | `AdmScenarioControllerTest` | DRAFT 편집 화면 GET 200, view name `admmgr/scenario/scenarioGraphEdit` |
| P0-GE-TC-002 | `AdmScenarioControllerTest` | PUBLISHED 편집 화면 GET 409, `STATE_CONFLICT` |
| P0-GE-TC-003 | `AdmScenarioApiControllerTest` | DRAFT graph 저장 PUT 성공 |
| P0-GE-TC-004 | `AdmScenarioApiControllerTest` | 시작 노드 없는 graph 저장 validation error |
| P0-GE-TC-005 | `AdmScenarioApiControllerTest` | 없는 `nextNodeKey` 저장 validation error |
| P0-GE-TC-006 | `AdmScenarioApiControllerTest` | END 노드 option 포함 validation error |
| P0-GE-TC-007 | `AdmScenarioControllerTest` 또는 기존 preview 테스트 | graph 저장 후 preview GET |
| P0-GE-TC-008 | controller/service 테스트 | graph 저장 후 publish POST 성공 |
| P0-GE-TC-009 | `AdmScenarioApiControllerTest` | 게시 후 같은 version graph 저장 `STATE_CONFLICT` |
| P0-GE-TC-010 | controller/API 보안 테스트 | 권한 없는 사용자 접근은 로그인 요구 또는 403 |
| P0-GE-TC-011 | `AdmScenarioApiControllerTest` | DRAFT graph JSON GET 200 |
| P0-GE-TC-012 | `AdmScenarioApiControllerTest` | PUBLISHED graph JSON GET 200, 저장/편집은 차단 |

테스트 작성 기준:

- CSRF 인터셉터가 테스트 대상에 포함되면 유효한 `csrfToken`, `csrfFormId` session/header를 넣는다.
- controller 단위 테스트에서 service mock을 사용할 경우 route, status, model, envelope를 검증한다.
- 통합 테스트를 추가하는 경우 `./mvnw clean test` 시간을 확인하고 fixture를 최소화한다.

수용 기준:

- 신규 테스트가 P0-GE-TC-001~012를 추적 가능하게 커버한다.
- 기존 `ScenarioGraphValidatorTest`와 중복되는 세부 validation은 최소화한다.
- `./mvnw clean test`가 통과한다.

### S9. 설계/테스트 문서 동기화

기준: §12 9단계

수정 문서:

- `2.설계/01.화면설계서/화면목록.md`
- `2.설계/03.API설계서/API목록.md`
- `2.설계/03.API설계서/M2_시나리오API.md`
- `2.설계/02.DB설계서/M2_시나리오ERD.md`
- `2.설계/05.보안설계서/보안체크리스트.md`
- `2.설계/01.화면설계서/접근성체크리스트.md`
- `2.설계/06.운영메모/M2_운영메모.md`
- `4.테스트/02.테스트시나리오/M2_시나리오테스트.md`

작업:

1. 신규 화면 URL과 목적을 화면목록에 추가한다.
2. 신규 GET graph API를 API목록과 M2 시나리오 API 문서에 추가한다.
3. PUT graph 저장 API는 기존 계약 유지로 명시한다.
4. ERD 문서에는 DB 변경 없음과 기존 `scenario_node`, `scenario_node_option` 재사용을 명시한다.
5. 보안 체크리스트에는 관리자 권한, AJAX CSRF, 외부 전송 없음, 설정 변경 없음 점검을 추가한다.
6. 접근성 체크리스트에는 편집 표/폼의 label, 오류 메시지 연결, 키보드 조작 기준을 추가한다.
7. 운영 메모에는 저장 후 미리보기 확인, 게시 전 graph 검증 절차를 추가한다.
8. 테스트 시나리오에는 P0-GE-TC-001~012를 추가하거나 기존 항목과 매핑한다.

수용 기준:

- 구현된 URL/API와 문서가 일치한다.
- DB 변경 없음, 설정 변경 없음, CSRF 재사용이 문서에 남는다.
- 문서 변경은 관련 절로 제한된다.

## 5. 커밋 분리 지시

후속 구현 PR은 반드시 아래 3개 커밋으로 분리한다. `git add .` 사용 금지. 커밋마다 대상 파일을 명시적으로 stage 한다.

### 5.1 feat 커밋

커밋 메시지:

```text
feat(scenario): add draft graph edit screen workflow
```

포함 파일:

- `src/main/resources/templates/admmgr/scenario/scenarioView.html`
- `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java`
- `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioApiController.java`
- `src/main/java/kr/co/cleverchat/domain/scenario/service/ScenarioService.java`
- `src/main/java/kr/co/cleverchat/domain/scenario/dto/ScenarioGraphDtos.java` 또는 관련 DTO 파일
- mapper interface/XML은 조회 메서드 추가가 필요한 경우에만 포함
- `src/main/resources/templates/admmgr/scenario/scenarioGraphEdit.html`
- `src/main/resources/static/asset/admmgr/style2/js/ADM.ScenarioGraphEdit.js`

포함 금지:

- 설계 문서
- 테스트 파일
- 이 작업지시서 파일
- `.gitignore`, wrapper, unrelated auth/search/chatbot 파일

### 5.2 docs 커밋

커밋 메시지:

```text
docs(scenario): define graph edit view P0 workflow
```

포함 파일:

- `2.설계/01.화면설계서/화면목록.md`
- `2.설계/03.API설계서/API목록.md`
- `2.설계/03.API설계서/M2_시나리오API.md`
- `2.설계/02.DB설계서/M2_시나리오ERD.md`
- `2.설계/05.보안설계서/보안체크리스트.md`
- `2.설계/01.화면설계서/접근성체크리스트.md`
- `2.설계/06.운영메모/M2_운영메모.md`
- `4.테스트/02.테스트시나리오/M2_시나리오테스트.md`

포함 금지:

- 코드 파일
- 테스트 Java 파일
- 이 작업지시서 파일
- unrelated 문서 정리

### 5.3 test 커밋

커밋 메시지:

```text
test(scenario): cover draft graph edit save workflow
```

포함 파일:

- `src/test/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioControllerTest.java`
- `src/test/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioApiControllerTest.java`
- 필요한 경우 scenario test fixture/helper의 최소 변경

포함 금지:

- production code
- 설계 문서
- 이 작업지시서 파일

## 6. PR 전 영향 점검

PR 생성 전 아래 표를 채우고 본문에 붙인다.

| 점검 항목 | 기대 결과 | PR 작성자 확인 |
|---|---|---|
| `application.yml` 변경 여부 | 변경 없음 |  |
| `WebMvcConfig` 변경 여부 | 변경 없음 |  |
| CSRF interceptor 정책 변경 여부 | 변경 없음 |  |
| `head.html` CSRF meta 사용 여부 | 사용 |  |
| AJAX `X-CSRF-Token` 전송 여부 | 전송 |  |
| AJAX `X-CSRF-FormId` 전송 여부 | 전송 |  |
| CSRF 응답 헤더 meta 갱신 여부 | 갱신 |  |
| 외부 CDN/외부 전송 추가 여부 | 없음 |  |
| DB/Flyway 변경 여부 | 없음 |  |
| `ApiResponse` envelope 변경 여부 | 없음 |  |
| `/admin/scenarios` 기존 URL 회귀 여부 | 없음 |  |
| preview/publish 기존 흐름 회귀 여부 | 없음 |  |

## 7. 검증 명령

후속 구현 PR에서 아래 명령을 실행한다.

```bash
cd /mnt/c/02.Project/02.자바/01.WorkSpace/CLEVERCHAT/3.개발/cleverchat
./mvnw clean test
```

수동 검증:

1. `/admin/scenarios` 목록 접속
2. 시나리오 상세 접속
3. `새 초안` 생성
4. DRAFT 버전 행의 `편집` 진입
5. 빈 graph에서 QUESTION 노드와 END 노드, option 1개 입력
6. 저장 성공 확인
7. 미리보기에서 시작 노드와 option 이동 확인
8. 게시 성공 확인
9. 게시된 version 편집 화면 직접 접근이 409로 차단되는지 확인
10. 게시된 version graph 저장 PUT이 `STATE_CONFLICT`로 차단되는지 확인

## 8. 완료 기준

- S1~S9가 모두 완료된다.
- P0-GE-TC-001~012가 테스트 또는 문서 매핑으로 추적된다.
- `./mvnw clean test`가 통과한다.
- `application.yml`, `WebMvcConfig`, CSRF 인터셉터 정책 변경이 없음을 PR 본문에 명시한다.
- 커밋은 feat, docs, test 3개로 분리된다.
- 이 작업지시서 파일은 후속 구현 PR 커밋에 포함하지 않는다.
