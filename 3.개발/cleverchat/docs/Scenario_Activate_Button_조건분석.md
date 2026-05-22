# Scenario 활성화 버튼 조건 분석

## 조사 범위

- 화면 템플릿: `src/main/resources/templates/admmgr/scenario/scenarioView.html`
- MVC 컨트롤러: `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java`
- API 컨트롤러: `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioApiController.java`
- 서비스: `src/main/java/kr/co/cleverchat/domain/scenario/service/ScenarioService.java`
- 검증기: `src/main/java/kr/co/cleverchat/domain/scenario/service/ScenarioGraphValidator.java`
- 매퍼/스키마: `ScenarioMapper.xml`, `ScenarioVersionMapper.xml`, `V3__scenario_baseline.sql`

## 결론

`scenarioView.html`의 활성화 버튼은 화면 렌더링 시점에 아래 조건을 모두 만족하는 버전 행에서만 출력된다.

| 구분 | 조건 | 근거 | 결과 |
|---|---|---|---|
| 버전 상태 | `version.status == 'PUBLISHED'` | `scenarioView.html`의 활성화 form `th:if` | `DRAFT`, `ARCHIVED` 버전 행에는 활성화 버튼 미노출 |
| 시나리오 상태 | `scenario.status != 'ACTIVE'` | `scenarioView.html`의 활성화 form `th:if` | 시나리오가 이미 `ACTIVE`면 활성화 버튼 미노출 |
| 화면 데이터 | `scenarioService.get(id)`, `scenarioService.versions(id)`가 반환한 모델 기준 | `AdmScenarioController.scenarioView()` | DB 상태가 그대로 Thymeleaf 조건에 사용됨 |
| POST 대상 | `/admin/scenarios/{id}/activate` + hidden `versionId` | `scenarioView.html`, `AdmScenarioController.scenarioActivateProc()` | 버튼 클릭 시 해당 published version을 활성화 시도 |

즉, 현재 활성화 버튼이 보이지 않는 직접 원인은 화면에 전달된 `versions` 중 `PUBLISHED` 상태인 버전이 없거나, 해당 `scenario.status`가 이미 `ACTIVE`이기 때문이다. 신규 생성 직후나 그래프 저장 직후의 버전은 `DRAFT`이므로 활성화 버튼 대신 게시 버튼 조건만 만족한다.

## 화면 렌더 조건

`scenarioView.html`의 버전 목록 작업 영역은 버전별로 다음 버튼을 조건부 렌더링한다.

| 버튼 | 렌더 조건 | 비고 |
|---|---|---|
| 미리보기 | 조건 없음 | 모든 버전 행에 노출 |
| 게시 | `version.status == 'DRAFT'` | 초안 버전만 게시 가능 |
| 활성화 | `version.status == 'PUBLISHED' and scenario.status != 'ACTIVE'` | 게시된 버전이 있고 시나리오가 아직 활성 상태가 아닐 때만 노출 |
| 비활성화 | `scenario.status == 'ACTIVE'` | 버전 행이 아니라 기본 정보 영역에 노출 |

## 상태 조합별 활성화 버튼 노출

| 시나리오 상태 | 버전 상태 | 그래프 저장 여부 | 게시 가능 여부 | 활성화 버튼 노출 | 설명 |
|---|---|---:|---:|---:|---|
| `DRAFT` | `DRAFT` | 미저장 | 불가 | 아니오 | 게시 버튼은 렌더되지만 서비스에서 저장된 그래프 없음 오류 |
| `DRAFT` | `DRAFT` | 저장됨 | 가능 | 아니오 | 아직 `PUBLISHED`가 아니므로 활성화 버튼 조건 불만족 |
| `DRAFT` | `PUBLISHED` | 저장됨 | 해당 없음 | 예 | 활성화 버튼의 기본 노출 케이스 |
| `DRAFT` | `ARCHIVED` | 저장됨 | 해당 없음 | 아니오 | `PUBLISHED`가 아니므로 미노출 |
| `INACTIVE` | `DRAFT` | 저장됨 | 가능 | 아니오 | 게시 후에만 활성화 버튼 노출 |
| `INACTIVE` | `PUBLISHED` | 저장됨 | 해당 없음 | 예 | 비활성 시나리오를 다시 활성화할 수 있음 |
| `ACTIVE` | `PUBLISHED` | 저장됨 | 해당 없음 | 아니오 | 이미 활성 상태라 활성화 버튼은 숨고 비활성화 버튼 노출 |
| `ACTIVE` | `DRAFT` | 저장됨 | 가능 | 아니오 | 신규 초안 게시는 가능하지만 현재 활성화 버튼 조건은 불만족 |
| `DELETED` | 전체 | 전체 | 해당 없음 | 아니오 | `ScenarioService.get()`에서 조회 차단 |

## 그래프 저장 여부와 게시 조건

그래프 저장 여부는 활성화 버튼의 직접 렌더 조건에는 포함되어 있지 않다. 다만 버전이 `PUBLISHED`가 되기 위한 선행 조건으로 작동한다.

| 단계 | 상태/조건 | 코드 흐름 | 버튼 영향 |
|---|---|---|---|
| 시나리오 생성 | scenario `DRAFT`, version `DRAFT` | `ScenarioService.create()`가 시나리오 생성 후 `createVersion()` 호출 | 버전 행에는 게시 버튼만 노출 |
| 그래프 저장 | version은 계속 `DRAFT`, `start_node_id` 설정 | `saveGraph()`가 노드/옵션 저장 후 `versionMapper.setStartNode()` 호출 | 저장만으로 활성화 버튼은 노출되지 않음 |
| 게시 시도 | version이 반드시 `DRAFT`여야 함 | `publish()`가 `draftVersion()` 호출 | `DRAFT`가 아니면 게시 불가 |
| 게시 검증 | 저장된 시작 노드와 노드 목록 필요 | `persistedGraph()`에서 `startNodeId == null` 또는 노드 없음이면 오류 | 그래프 미저장 상태에서는 `PUBLISHED`로 전환되지 못함 |
| 게시 검증 | 시작 노드에서 모든 노드 도달 가능 | `ScenarioGraphValidator.validateForPublish()` | 도달 불가 노드가 있으면 게시 실패 |
| 게시 완료 | version `PUBLISHED`, 기존 published는 `ARCHIVED` | `archivePublished()` 후 `publish()` | 다음 상세 화면 렌더부터 활성화 버튼 후보가 됨 |

## 서비스 레벨 활성화 조건

화면에서 버튼이 노출되어도 POST 처리 시 서비스에서 한 번 더 상태를 검증한다.

| 검증 위치 | 조건 | 실패 메시지 |
|---|---|---|
| `ScenarioService.activate()` | scenario status가 `DRAFT` 또는 `INACTIVE` | `DRAFT 또는 INACTIVE 시나리오만 활성화할 수 있습니다.` |
| `ScenarioService.activate()` | version이 존재해야 함 | `게시된 버전만 활성화할 수 있습니다.` |
| `ScenarioService.activate()` | version의 `scenarioId`가 요청 scenario와 같아야 함 | `게시된 버전만 활성화할 수 있습니다.` |
| `ScenarioService.activate()` | version status가 `PUBLISHED` | `게시된 버전만 활성화할 수 있습니다.` |
| `ScenarioMapper.activate()` | scenario status를 `ACTIVE`, `active_version_id`를 요청 version으로 업데이트 | 성공 시 매칭 캐시 무효화 |

화면 조건은 `scenario.status != 'ACTIVE'`로 되어 있어 이론상 `DELETED`도 조건상 통과할 수 있지만, 상세 화면 진입 전 `ScenarioService.get()`이 `DELETED`를 `NOT_FOUND`로 차단한다. 따라서 실제 화면에서는 `DRAFT` 또는 `INACTIVE` + `PUBLISHED` 조합이 활성화 버튼 노출 조합이다.

## 버전 상태 전이

| 작업 | 이전 상태 | 이후 상태 | 관련 코드 |
|---|---|---|---|
| 새 초안 생성 | 없음 | `DRAFT` | `createVersion()` |
| 그래프 저장 | `DRAFT` | `DRAFT` 유지 | `saveGraph()` |
| 게시 | `DRAFT` | `PUBLISHED` | `publish()` |
| 다른 버전 게시 | 기존 `PUBLISHED` | `ARCHIVED` | `archivePublished()` |
| 활성화 | scenario `DRAFT`/`INACTIVE` | scenario `ACTIVE`, `active_version_id` 설정 | `activate()` |
| 비활성화 | scenario `ACTIVE` | scenario `INACTIVE` | `deactivate()` |

## 현재 미노출 이유

코드 기준으로 활성화 버튼이 현재 미노출되는 가장 직접적인 이유는 `scenarioView.html`의 `th:if` 조건을 만족하는 행이 없기 때문이다.

- 버전이 `DRAFT`이면 `게시` 버튼만 노출되고 `활성화` 버튼은 노출되지 않는다.
- 그래프를 저장해도 버전 상태는 계속 `DRAFT`이므로 활성화 버튼은 노출되지 않는다.
- 그래프 저장 후 `게시`까지 성공해 버전 상태가 `PUBLISHED`가 되어야 활성화 버튼 렌더 후보가 된다.
- 시나리오가 이미 `ACTIVE`이면 `PUBLISHED` 버전이 있어도 활성화 버튼은 숨고 기본 정보 영역의 `비활성화` 버튼이 노출된다.
- 새 버전을 게시하면 기존 `PUBLISHED` 버전은 `ARCHIVED`로 바뀌며, `ARCHIVED` 버전 행에는 활성화 버튼이 노출되지 않는다.

따라서 현재 화면에서 활성화 버튼을 기대했는데 보이지 않는 상황은 대체로 다음 중 하나다.

| 관측 상태 | 미노출 원인 | 확인 포인트 |
|---|---|---|
| 버전 상태가 `DRAFT` | 게시 전 상태 | 버전 목록에서 `게시` 버튼이 보이는지 확인 |
| 그래프만 저장한 상태 | 저장은 게시가 아님 | `startNodeId` 값이 있어도 `version.status`가 `PUBLISHED`가 아니면 미노출 |
| 게시 버튼 클릭 후에도 `DRAFT` 유지 | 게시 검증 실패 가능 | 저장 그래프 없음, 시작 노드 없음, 도달 불가 노드 여부 확인 |
| 시나리오 상태가 `ACTIVE` | 이미 활성화된 시나리오 | 기본 정보 영역의 `비활성화` 버튼 확인 |
| 버전 상태가 `ARCHIVED` | 최신 published에서 밀린 버전 | 같은 scenario의 다른 `PUBLISHED` 버전 확인 |

## 확인용 SQL

```sql
SELECT
    s.id AS scenario_id,
    s.status AS scenario_status,
    s.active_version_id,
    v.id AS version_id,
    v.version_no,
    v.status AS version_status,
    v.start_node_id,
    v.published_at
FROM scenario s
JOIN scenario_version v ON v.scenario_id = s.id
WHERE s.id = :scenarioId
ORDER BY v.version_no DESC;
```

활성화 버튼이 노출되려면 결과 중 최소 한 행이 `scenario_status IN ('DRAFT', 'INACTIVE')`이고 `version_status = 'PUBLISHED'`여야 한다.
