# Scenario 게시 실패 UX 분석

## 조사 범위

- 화면 템플릿: `src/main/resources/templates/admmgr/scenario/scenarioView.html`
- MVC 컨트롤러: `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java`
- API 컨트롤러: `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioApiController.java`
- 서비스: `src/main/java/kr/co/cleverchat/domain/scenario/service/ScenarioService.java`
- 검증기: `src/main/java/kr/co/cleverchat/domain/scenario/service/ScenarioGraphValidator.java`
- 공통 예외 처리: `src/main/java/kr/co/cleverchat/common/error/GlobalExceptionHandler.java`

## 결론

현재 `publish()` 실패 UX는 사용자가 다음 행동을 알기 어렵다. 특히 신규 시나리오 생성 직후의 빈 `DRAFT` 버전에서도 `게시` 버튼이 노출되며, 클릭하면 `persistedGraph()`에서 `VALIDATION_ERROR`와 `"저장된 그래프가 없습니다."` 메시지가 발생한다. 이 메시지는 실패 원인 자체는 말하지만, "그래프를 먼저 저장해야 한다"는 선행조건과 이동/수정 방법을 알려주지 않는다.

더 큰 문제는 화면 form POST 흐름이다. `scenarioView.html`의 게시 form은 일반 form POST이고, `AdmScenarioController.scenarioPublishProc()`는 성공 시에만 `redirect:/admin/scenarios/{scenarioId}`를 반환한다. 예외가 발생하면 `@RestControllerAdvice`인 `GlobalExceptionHandler`가 JSON 응답을 반환하므로 사용자는 기존 상세 화면 안에서 안내를 받는 것이 아니라 HTTP 400/409 JSON 오류 페이지를 보게 된다.

따라서 `"저장된 그래프가 없습니다."` 단독 문구는 불충분하며, 게시 버튼의 사전 비활성화 또는 근접 안내, 실패 시 상세 화면 복귀와 Flash 메시지, API/화면용 메시지 표준화를 함께 검토하는 것이 적절하다.

## 게시 흐름

| 단계 | 위치 | 처리 | 실패 시 노출 |
|---|---|---|---|
| 1 | `scenarioView.html` | `version.status == 'DRAFT'`이면 게시 form 렌더 | 그래프 저장 여부는 버튼 조건에 없음 |
| 2 | `AdmScenarioController.scenarioPublishProc()` | `/admin/scenarios/versions/{versionId}/publish` form POST 수신 | 예외 처리 로직 없음 |
| 3 | `ScenarioService.publish()` | `draftVersion(versionId)` 호출 | `NOT_FOUND`, `STATE_CONFLICT` 가능 |
| 4 | `ScenarioService.publish()` | `persistedGraph(versionId)` 호출 | 저장된 시작 노드 또는 노드 목록이 없으면 `VALIDATION_ERROR` |
| 5 | `ScenarioService.publish()` | `graphValidator.validateForPublish(graph)` 호출 | 그래프 정합성 오류 시 `VALIDATION_ERROR` |
| 6 | `ScenarioService.publish()` | 기존 published archive 후 현재 버전 publish | 성공 후 상세 화면 redirect |

API publish 경로인 `AdmScenarioApiController.scenarioPublishProc()`도 같은 `ScenarioService.publish()`를 호출한다. API 호출자는 JSON 오류 응답을 받는 것이 자연스럽지만, 관리자 화면 form POST까지 동일한 `@RestControllerAdvice` 응답을 받는 현재 구조는 화면 UX로 부적합하다.

## 실패 분기

| 분기 | 코드 위치 | ErrorCode | 현재 메시지 | 사용자 관점 평가 |
|---|---|---|---|---|
| 버전 없음 | `draftVersion()` | `NOT_FOUND` | `대상을 찾을 수 없습니다.` | 대상이 무엇인지 불명확 |
| DRAFT 아님 | `draftVersion()` | `STATE_CONFLICT` | `DRAFT 버전만 수정하거나 게시할 수 있습니다.` | 원인은 비교적 명확 |
| 저장 그래프 없음 | `persistedGraph()` | `VALIDATION_ERROR` | `저장된 그래프가 없습니다.` | 다음 행동 안내 부족 |
| 시작 노드 누락 | `validateForSave()` | `VALIDATION_ERROR` | `시작 노드가 노드 목록에 없습니다.` | 편집 화면에서 무엇을 고칠지 추가 안내 필요 |
| END 노드 옵션 존재 | `validateForSave()` | `VALIDATION_ERROR` | `END 노드는 옵션을 가질 수 없습니다.` | 원인은 명확하나 위치 안내 없음 |
| 존재하지 않는 다음 노드 | `validateForSave()` | `VALIDATION_ERROR` | `존재하지 않는 다음 노드가 있습니다.` | 어떤 옵션인지 알기 어려움 |
| 중복 노드 키 | `nodeMap()` | `VALIDATION_ERROR` | `중복된 노드 키가 있습니다.` | 원인은 명확 |
| 노드 키 형식 오류 | `nodeMap()` | `VALIDATION_ERROR` | `노드 키는 영문, 숫자, 하이픈, 언더스코어만 허용합니다.` | 원인은 명확 |
| 노드 유형 오류 | `nodeMap()` | `VALIDATION_ERROR` | `허용되지 않는 노드 유형입니다.` | 일반 사용자에게는 모호 |
| 도달 불가 노드 | `validateForPublish()` | `VALIDATION_ERROR` | `시작 노드에서 도달할 수 없는 노드가 있습니다.` | 편집 위치 안내 필요 |

## 현재 화면 흐름

`scenarioView.html`의 버전 목록 작업 영역은 다음 조건으로 게시 버튼을 노출한다.

```html
<form th:if="${version.status == 'DRAFT'}" th:action="@{/admin/scenarios/versions/{id}/publish(id=${version.id})}" method="post">
```

버튼 노출 조건은 버전 상태뿐이다. `version.startNodeId`, 노드 수, 저장 그래프 정합성은 화면에서 확인하지 않는다. 신규 시나리오 생성 시 `ScenarioService.create()`가 곧바로 `createVersion()`을 호출하므로, 사용자는 그래프를 한 번도 저장하지 않은 `DRAFT` 버전에서 게시 버튼을 볼 수 있다.

실패 시에는 `GlobalExceptionHandler`가 아래 형태의 JSON 응답을 반환한다.

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "저장된 그래프가 없습니다."
  }
}
```

이 응답은 API에는 적합하지만 일반 form POST 화면에서는 상세 화면의 맥락, 버튼 주변 안내, 재시도 경로를 모두 잃는다.

## 메시지 충분성 평가

| 평가 항목 | 판단 | 비고 |
|---|---|---|
| 원인 식별 | 부분 충족 | 저장된 그래프가 없다는 사실은 전달 |
| 해결 행동 | 미충족 | 그래프 편집/저장 후 게시하라는 안내 없음 |
| 화면 맥락 유지 | 미충족 | 상세 화면으로 돌아가지 않고 JSON 오류 페이지 노출 |
| 사전 예방 | 미충족 | 게시 불가 상태에서도 버튼 활성 |
| 접근성 | 미충족 | 버튼 상태, 사유, 대체 행동을 보조기술에 제공하지 않음 |

권장 사용자 문구는 `"그래프를 저장한 뒤 게시할 수 있습니다. 노드를 추가하고 시작 노드를 지정한 후 그래프를 저장해 주세요."`처럼 조건과 행동을 함께 제시하는 형태다.

## 선행조건 안내 필요성

게시의 실질 선행조건은 최소한 아래와 같다.

| 선행조건 | 근거 | 화면 안내 필요성 |
|---|---|---|
| 버전 상태가 `DRAFT` | `draftVersion()` | 이미 버튼 렌더 조건에 반영됨 |
| 저장된 노드가 1개 이상 존재 | `persistedGraph()`의 `nodes.isEmpty()` | 필요 |
| `startNodeId`가 존재 | `persistedGraph()`의 `version.getStartNodeId() == null` | 필요 |
| 시작 노드가 노드 목록에 존재 | `validateForSave()` | 필요 |
| END 노드는 옵션 없음 | `validateForSave()` | 필요 |
| 옵션의 다음 노드가 존재 | `validateForSave()` | 필요 |
| 노드 키가 유일하고 형식에 맞음 | `nodeMap()` | 필요 |
| 시작 노드에서 모든 노드가 도달 가능 | `validateForPublish()` | 필요 |

화면에서 모든 검증을 사전에 재현할 필요는 없다. 다만 빈 초안처럼 명확히 게시 불가능한 상태는 버튼 비활성화와 근접 안내로 예방하는 편이 좋다.

## 버튼 Disable 후보

### 후보 A: 시작 노드 없는 초안은 게시 버튼 비활성화

```html
<button
    type="submit"
    th:disabled="${version.startNodeId == null}"
    th:title="${version.startNodeId == null ? '그래프를 저장한 뒤 게시할 수 있습니다.' : '게시'}">
    게시
</button>
```

| 항목 | 평가 |
|---|---|
| 장점 | 현재 모델의 `version.startNodeId`만으로 적용 가능, 빈 그래프 게시 실패를 대부분 사전 차단 |
| 단점 | 노드 도달성, 옵션 오류 등 세부 검증 실패는 여전히 POST 후 발생 |
| 권장도 | 높음 |

### 후보 B: 버튼은 유지하고 클릭 전 확인/차단

```html
<button
    type="submit"
    th:data-publishable="${version.startNodeId != null}"
    th:data-disabled-message="${version.startNodeId == null ? '그래프를 저장한 뒤 게시할 수 있습니다.' : ''}">
    게시
</button>
```

| 항목 | 평가 |
|---|---|
| 장점 | 기존 버튼 레이아웃 유지, 공통 JS alert/toast와 결합 가능 |
| 단점 | JavaScript 의존, 접근성 처리가 별도로 필요 |
| 권장도 | 중간 |

### 후보 C: 서버 계산 publishable 플래그 도입

컨트롤러 모델에 `versionPublishStates` 같은 DTO를 추가해 `hasGraph`, `hasStartNode`, `publishable`, `reason`을 화면에 전달한다.

| 항목 | 평가 |
|---|---|
| 장점 | 버튼, 툴팁, help text, 테스트 기준을 명확히 통일 가능 |
| 단점 | DTO/서비스 조회 범위가 늘어나 문서 작업 범위를 넘어섬 |
| 권장도 | 중장기 개선으로 높음 |

## Tooltip 후보

| 상황 | 후보 문구 |
|---|---|
| 그래프 미저장 | `그래프를 저장한 뒤 게시할 수 있습니다.` |
| 시작 노드 없음 | `시작 노드를 지정하고 그래프를 저장해 주세요.` |
| 초안 아님 | `DRAFT 버전만 게시할 수 있습니다.` |
| 정합성 검증 필요 | `게시 전 시작 노드와 연결되지 않은 노드가 없는지 확인해 주세요.` |
| 게시 가능 | `이 초안 버전을 게시합니다.` |

HTML `title`만으로는 키보드/모바일 UX가 제한적이므로, `aria-describedby`로 연결되는 숨김 또는 근접 설명 텍스트를 함께 두는 편이 좋다.

## Help Text 후보

버전 목록 상단 또는 게시 버튼 인접 영역에 아래 문구 중 하나를 노출할 수 있다.

| 위치 | 후보 문구 | 용도 |
|---|---|---|
| 버전 섹션 상단 | `게시하려면 그래프를 저장하고 시작 노드를 지정해야 합니다.` | 전체 규칙 안내 |
| 게시 버튼 옆 | `그래프 저장 후 게시 가능` | 짧은 인라인 안내 |
| 비활성 버튼 아래 | `노드를 추가하고 시작 노드를 지정한 뒤 그래프를 저장해 주세요.` | 빈 초안의 직접 행동 안내 |
| 실패 Flash | `게시할 수 없습니다. 그래프를 저장한 뒤 다시 시도해 주세요.` | POST 실패 후 복귀 메시지 |
| 실패 Flash 상세 | `게시할 수 없습니다. 시작 노드에서 연결되지 않은 노드를 정리한 뒤 다시 시도해 주세요.` | 도달성 실패 안내 |

## 실패 메시지 표준화 후보

| 현재 메시지 | 사용자용 후보 | 비고 |
|---|---|---|
| `저장된 그래프가 없습니다.` | `그래프를 저장한 뒤 게시할 수 있습니다. 노드를 추가하고 시작 노드를 지정한 후 그래프를 저장해 주세요.` | 최우선 개선 후보 |
| `시작 노드가 노드 목록에 없습니다.` | `시작 노드를 다시 지정하고 그래프를 저장해 주세요.` | 내부 데이터 불일치 완화 문구 |
| `END 노드는 옵션을 가질 수 없습니다.` | `종료 노드에는 선택지를 둘 수 없습니다. 선택지를 제거한 뒤 저장해 주세요.` | 행동 포함 |
| `존재하지 않는 다음 노드가 있습니다.` | `삭제된 노드로 연결된 선택지가 있습니다. 연결 대상을 다시 지정해 주세요.` | 편집 행동 명확화 |
| `시작 노드에서 도달할 수 없는 노드가 있습니다.` | `시작 노드에서 연결되지 않은 노드가 있습니다. 모든 노드가 시작 노드에서 이어지도록 연결해 주세요.` | 게시 전 검증의 핵심 |

## 권장 개선 묶음

1. 단기: `version.startNodeId == null`인 `DRAFT` 버전의 게시 버튼을 비활성화하고 `그래프를 저장한 뒤 게시할 수 있습니다.` 안내를 노출한다.
2. 단기: form POST 실패 시 JSON 오류 페이지 대신 상세 화면으로 redirect하고 Flash 메시지를 표시한다.
3. 단기: `"저장된 그래프가 없습니다."`를 사용자 행동 중심 문구로 바꾸거나 화면용 메시지 매핑을 둔다.
4. 중기: 게시 가능 여부 DTO를 만들어 `hasGraph`, `hasStartNode`, `publishable`, `reason`을 화면과 테스트에서 재사용한다.
5. 중기: `activate`, `deactivate`, `new draft`, `publish` 같은 관리자 form POST 실패 UX를 공통화한다.

## 검증 후보

| 케이스 | 기대 UX |
|---|---|
| 신규 시나리오 생성 직후 | 게시 버튼 비활성 또는 게시 전 안내 노출 |
| 시작 노드 없는 초안 | `그래프를 저장한 뒤 게시할 수 있습니다.` 안내 |
| 저장된 그래프가 있지만 도달 불가 노드 존재 | 상세 화면 복귀 후 도달 불가 안내 |
| DRAFT가 아닌 버전 직접 POST | 상세 화면 또는 API 응답에서 상태 충돌 안내 |
| API publish 실패 | JSON `error.code`, `error.message` 유지 |

## 커밋 후보

- 파일: `3.개발/cleverchat/docs/Scenario_Publish_UX_분석.md`
- 커밋 메시지: `docs(scenario): analyze publish failure UX`
- 작업 분류: 문서 단독, 코드 무수정
