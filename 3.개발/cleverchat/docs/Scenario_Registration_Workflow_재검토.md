# Scenario 등록 워크플로우 재검토

## 1. 검토 범위

본 문서는 시나리오 관리자 워크플로우를 `controller`, `service`, `template`, `db`의 현재 구현 상태 기준으로 재검토한다. 코드 수정은 하지 않았으며, 사용자가 확인한 "새초안 시 version+1", "preview 노드 없음", "ACTIVE 외 버튼 부족" 현상은 전체 흐름 검토의 사례로만 사용했다.

검토 대상 파일은 다음과 같다.

| 구분 | 파일 |
|---|---|
| MVC controller | `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java` |
| REST controller | `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioApiController.java`, `AdmScenarioCategoryController.java`, `AdmScenarioKeywordController.java` |
| service | `src/main/java/kr/co/cleverchat/domain/scenario/service/ScenarioService.java`, `ScenarioGraphValidator.java` |
| template | `src/main/resources/templates/admmgr/scenario/scenarioList.html`, `scenarioRegist.html`, `scenarioView.html`, `scenarioPreviewLayer.html` |
| mapper/db | `src/main/resources/mapper/scenario/*.xml`, `src/main/resources/db/migration/V3__scenario_baseline.sql` |
| 관련 기존 분석 | `docs/Scenario_Publish_UX_분석.md`, `docs/Scenario_Activate_Button_조건분석.md`, `docs/Scenario_CSRF_Hidden_Fragment_분석.md` |

## 2. 결론

현재 구현은 시나리오와 버전의 상태 머신, 그래프 저장 API, 게시/활성화 API 계약을 갖추고 있다. 그러나 관리자 화면은 목록, 기본정보 등록/수정, 상세의 버전 버튼, 단순 미리보기만 제공하며 그래프 편집/저장, 키워드 편집, 카테고리 관리, 게시 실패 복귀 UX가 화면 워크플로우로 연결되어 있지 않다.

따라서 전체 흐름은 `목록 -> 등록 -> 상세 -> 새초안 -> 버전 미리보기 -> 게시/활성화/비활성화`로 보이지만, 실제 사용자는 상세 화면에서 게시 선행조건인 그래프 저장을 수행할 수 없다. API를 별도로 호출하지 않는 한 신규 등록 직후의 초안은 `start_node_id`가 `NULL`인 빈 버전으로 남고, 미리보기는 "미리보기 가능한 노드가 없습니다."를 보여주며, 게시는 form POST 실패 시 JSON 오류 화면으로 이탈한다.

상태 머신 자체보다 화면과 서비스 계약 사이의 단절이 핵심 문제다.

## 3. 현재 상태 전이표

### 3.1 scenario 상태

| 작업 | 선행 상태 | 후속 상태 | 구현 위치 | 화면 노출 | 비고 |
|---|---|---|---|---|---|
| 등록 | 없음 | `DRAFT` | `ScenarioService.create()` | `GET /admin/scenarios/new`, `POST /admin/scenarios` | 등록과 동시에 version `DRAFT` 1건 생성 |
| 기본정보 수정 | `DRAFT`, `INACTIVE` | 상태 유지 | `ScenarioService.update()` | `GET /admin/scenarios/{id}/edit`, `POST /admin/scenarios/{id}` | `ACTIVE`는 화면에 수정 버튼이 보이나 서비스에서 거부 |
| 삭제 | `DRAFT`, `INACTIVE` | `DELETED` | `ScenarioService.delete()` | REST API만 있음 | MVC 화면 삭제 버튼 없음 |
| 활성화 | `DRAFT`, `INACTIVE` + `PUBLISHED` version | `ACTIVE`, `active_version_id` 설정 | `ScenarioService.activate()` | 상세 버전 행의 활성화 form | `ACTIVE`에서는 서비스가 재활성화 거부 |
| 비활성화 | `ACTIVE` | `INACTIVE` | `ScenarioService.deactivate()` | 상세 기본정보 영역의 비활성화 form | `active_version_id`는 유지됨 |

### 3.2 scenario_version 상태

| 작업 | 선행 상태 | 후속 상태 | 구현 위치 | 화면 노출 | 비고 |
|---|---|---|---|---|---|
| 최초 초안 생성 | 없음 | `DRAFT`, `version_no=1` | `ScenarioService.create()` -> `createVersion()` | 등록 성공 후 상세에서 확인 | `start_node_id`는 `NULL` |
| 새 초안 생성 | scenario가 `DELETED` 아님 | `DRAFT`, `MAX(version_no)+1` | `ScenarioService.createVersion()` | 상세의 `새 초안` form | 기존 `DRAFT` 중복 여부 검사 없음 |
| 그래프 저장 | version `DRAFT` | `DRAFT` 유지, `start_node_id` 설정 | `ScenarioService.saveGraph()` | REST API만 있음 | MVC 그래프 편집/저장 화면 없음 |
| 게시 | version `DRAFT` + 저장 그래프 유효 | 현재 버전 `PUBLISHED`, 기존 `PUBLISHED`는 `ARCHIVED` | `ScenarioService.publish()` | 상세의 `게시` form | 게시 후 scenario 상태는 바뀌지 않음 |
| 기존 게시본 보관 | 기존 `PUBLISHED` | `ARCHIVED` | `ScenarioVersionMapper.archivePublished()` | 자동 처리 | scenario `active_version_id`와 불일치 가능 |

### 3.3 DB 제약과 실제 의미

| 테이블/컬럼 | 현재 제약 | 워크플로우 의미 |
|---|---|---|
| `scenario.status` | `DRAFT`, `ACTIVE`, `INACTIVE`, `DELETED` | 시나리오 운영 상태 |
| `scenario.active_version_id` | nullable FK to `scenario_version.id` | 매칭 대상 활성 버전 포인터 |
| `scenario_version.status` | `DRAFT`, `PUBLISHED`, `ARCHIVED` | 버전 게시 상태 |
| `scenario_version.start_node_id` | nullable FK to `scenario_node.id` | 그래프 저장 후 시작 노드 포인터 |
| `ux_scenario_version_published_one` | scenario별 `PUBLISHED` 1개 | 게시본은 하나만 유지 |

주의할 점은 `publish()`가 기존 `PUBLISHED`를 `ARCHIVED`로 바꾸지만 `scenario.active_version_id`는 갱신하지 않는다는 점이다. 이미 `ACTIVE`인 시나리오에서 새 초안을 게시하면 DB상 `active_version_id`가 `ARCHIVED` 버전을 가리킬 수 있고, 동시에 최신 `PUBLISHED` 버전은 활성 포인터가 아닐 수 있다. 매칭 조회는 `scenario.status='ACTIVE'`와 `active_version_id IS NOT NULL`만 확인하므로 의도와 컬럼 상태가 어긋날 여지가 있다.

## 4. 화면별 진입과 후속 액션

| 화면 | 진입 | 현재 제공 액션 | 후속 경로 | 누락/불일치 |
|---|---|---|---|---|
| 목록 `scenarioList.html` | `GET /admin/scenarios?status=` | 관리자 홈, 등록, 상세 링크 | 등록 또는 상세 | status 파라미터는 컨트롤러에서 받지만 화면 필터 UI 없음 |
| 등록/수정 `scenarioRegist.html` | `GET /admin/scenarios/new`, `GET /admin/scenarios/{id}/edit` | 저장, 취소 | 저장 성공 시 상세 또는 목록 | 카테고리 생성 화면 없음. 카테고리가 없으면 신규 등록이 막힘 |
| 상세 `scenarioView.html` | `GET /admin/scenarios/{id}` | 목록, 수정, 비활성화, 새 초안, 미리보기, 게시, 활성화 | 각 POST 후 상세 redirect | 그래프 편집/저장 버튼 없음. 키워드 목록만 있고 편집 없음. `ACTIVE`에서도 수정/새 초안 버튼이 보이나 서비스 정책과 충돌 |
| 미리보기 `scenarioPreviewLayer.html` | `GET /admin/scenarios/versions/{versionId}/preview` | 상세 복귀, 옵션 이동 | 같은 미리보기 URL | 시작 노드 없으면 빈 안내만 표시. 공통 관리자 레이아웃 미적용은 Layer 명명 원칙상 가능하나 독립 페이지로 열릴 때 맥락이 약함 |
| 그래프 저장 | REST `PUT /admin/api/scenarios/versions/{versionId}/graph` | 노드/옵션 저장 | API 응답 | MVC 화면 진입점 없음 |
| 카테고리 관리 | REST `/admin/api/scenario-categories` | 목록/등록/수정 | API 응답 | MVC 화면 없음 |
| 키워드 관리 | REST `/admin/api/scenarios/{scenarioId}/keywords` | 목록/일괄수정 | API 응답 | 상세에는 조회 결과만 노출 |

## 5. 사례 현상 원인

| 관측 사례 | 코드 기준 원인 | 설계 적합성 평가 |
|---|---|---|
| 새초안 시 `version+1` | `ScenarioVersionMapper.nextVersionNo()`가 `MAX(version_no)+1`을 반환하고 `createVersion()`이 항상 새 row를 생성 | 버전 이력 모델에는 자연스럽지만, 기존 `DRAFT`가 있는 상태에서 중복 초안 생성이 가능해 UX 혼란 발생 |
| preview 노드 없음 | 신규 `DRAFT`는 `start_node_id`가 `NULL`이고, 노드/옵션 저장은 REST API 전용 | 미리보기 버튼은 모든 버전에 노출되므로 빈 초안에서도 미리보기 가능해 보이는 오해 발생 |
| `ACTIVE` 외 버튼 부족 | `ACTIVE`에서는 비활성화만 실제 의미가 있고, 활성화 form은 `scenario.status != 'ACTIVE'` 조건으로 숨김 | 활성화 버튼 미노출 자체는 정책과 맞지만, `ACTIVE`에서 수정/새초안이 화면에 남고 서비스에서 일부 거부되어 액션 모델이 불명확 |

## 6. 필수 선행조건

| 액션 | 필수 선행조건 | 현재 화면 안내 |
|---|---|---|
| 시나리오 등록 | enabled 여부와 무관하게 존재하는 category id | "카테고리가 없으면 API로 먼저 생성해야 합니다." 문구만 있음 |
| 기본정보 수정 | scenario status가 `DRAFT` 또는 `INACTIVE` | 수정 버튼은 상태와 무관하게 노출 |
| 새 초안 생성 | scenario가 `DELETED`가 아님 | 항상 노출. 기존 `DRAFT` 존재 여부 안내 없음 |
| 그래프 저장 | version status가 `DRAFT`, 노드 키/유형/옵션 유효, 시작 노드 존재 | MVC 화면 없음 |
| 게시 | version status가 `DRAFT`, 저장된 노드와 `start_node_id` 존재, 모든 노드가 시작 노드에서 도달 가능 | `DRAFT`이면 게시 버튼 노출. 그래프 저장 여부 안내 부족 |
| 활성화 | scenario status가 `DRAFT` 또는 `INACTIVE`, version status가 `PUBLISHED` | 조건부 버튼 노출은 대체로 맞음 |
| 비활성화 | scenario status가 `ACTIVE` | 조건부 버튼 노출은 맞음 |
| 삭제 | scenario status가 `DRAFT` 또는 `INACTIVE` | MVC 버튼 없음 |

## 7. 잘못된 redirect/오류 UX

| 흐름 | 현재 동작 | 문제 |
|---|---|---|
| 게시 form POST 실패 | `ScenarioService.publish()` 예외가 `@RestControllerAdvice` JSON 오류로 응답 | 관리자 상세 화면으로 돌아오지 않아 사용자가 다음 조치를 알기 어려움 |
| 활성화 form POST 실패 | 서비스 예외가 JSON 오류로 응답 가능 | 직접 POST, 동시성, 상태 변경 상황에서 화면 맥락 상실 |
| 새 초안 POST 실패 | `ACTIVE`도 서비스상 허용되어 실패 가능성은 낮지만, `DELETED`/직접 요청은 JSON 오류 | 화면 form POST와 API 오류 응답 경계가 불명확 |
| 수정 form POST 실패 | Bean Validation 실패는 같은 화면 렌더, 서비스 상태 충돌은 JSON 오류 | `ACTIVE`에서 수정 버튼이 보이므로 충돌 가능성이 실제 UX로 노출 |
| 미리보기 옵션 이동 | `nextNodeId`가 `NULL`인 옵션도 링크 생성 가능 | 클릭 시 `nodeId` 없이 시작 노드 또는 빈 화면으로 돌아갈 수 있어 종료 옵션 표현이 불명확 |

## 8. 누락 버튼과 액션 정렬

| 위치 | 현재 | 권장 |
|---|---|---|
| 상세 상단 | 목록, 수정 | 상태별 액션 DTO를 기준으로 수정 가능 여부 표시. `ACTIVE`에서는 수정 비활성 또는 "비활성화 후 수정" 안내 |
| 버전 섹션 | 새 초안 항상 노출 | 기존 `DRAFT`가 있으면 새 초안 대신 해당 초안 편집/미리보기로 안내. `ACTIVE`에서 새 초안 허용 여부를 정책 결정 |
| 버전 행 | 미리보기 항상 노출 | `startNodeId == null`이면 비활성 또는 "그래프 저장 필요" 안내 |
| 버전 행 | `DRAFT`이면 게시 | `startNodeId`, 노드 수, 서버 계산 publishable 플래그 기반으로 비활성/사유 표시 |
| 버전 행 | `PUBLISHED` + not `ACTIVE`이면 활성화 | 대체로 유지. 단, `active_version_id`와 게시본 관계를 함께 표시 |
| 기본정보 영역 | `ACTIVE`이면 비활성화 | 유지. 비활성화 후 수정/재활성화 가능 흐름 안내 필요 |
| 키워드 영역 | 목록만 표시 | 키워드/유사어 편집 진입 버튼 필요 |
| 카테고리 | 등록폼 select만 표시 | 카테고리 관리 화면 또는 등록폼 내 생성 흐름 필요 |
| 삭제 | REST API만 있음 | `DRAFT`/`INACTIVE` 상세에 삭제 버튼과 확인 UX 필요 |
| 그래프 | 화면 없음 | 초안 버전 행에 그래프 편집/저장 진입 버튼 필요 |

## 9. 게시/활성화 정책 의사결정 필요

특히 `ACTIVE` 시나리오에서 새 `DRAFT`를 만들고 게시하는 경우의 정책을 정해야 한다.

| 후보 | 정책 | 장점 | 단점 |
|---|---|---|---|
| A | `ACTIVE`에서는 새 초안/게시 금지, 비활성화 후 수정 | 상태 모델 단순, 현재 `activate()` 제약과 일관 | 운영 중 개정 초안 작성 불가 |
| B | `ACTIVE`에서도 새 초안 작성/게시 허용, 게시 즉시 `active_version_id`도 새 `PUBLISHED`로 갱신 | 운영 중 무중단 개정 가능 | 게시와 활성화가 합쳐져 승인 단계 약화 |
| C | `ACTIVE`에서도 새 초안 작성/게시 허용, 별도 "활성 버전 교체" 액션 제공 | 게시 검증과 운영 반영을 분리 | 화면/서비스 액션이 늘어남 |

현재 코드는 C처럼 보이는 일부 요소가 있으나 실제로는 `activate()`가 `ACTIVE` 상태를 거부하므로 새 게시본을 활성 포인터로 교체할 수 없다. 이 상태는 의도 설계로 보기 어렵고, A/B/C 중 하나로 명시해야 한다.

## 10. 권장 개선안

### P0. 워크플로우 단절 해소

| 개선 | 내용 |
|---|---|
| 그래프 편집 화면 추가 | `DRAFT` 버전에서 노드/옵션/시작 노드를 저장할 수 있는 MVC 화면 또는 관리자 JS 화면 제공 |
| 새 초안 중복 방지 | 기존 `DRAFT`가 있으면 새 row 생성 대신 기존 초안으로 안내하거나 명시 확인 후 생성 |
| 게시 실패 UX 복구 | MVC form POST 실패 시 JSON 오류 대신 상세 redirect + Flash 메시지 |
| 미리보기 분기 | `startNodeId == null`이면 미리보기 버튼 비활성 또는 그래프 저장 안내 |
| `ACTIVE` 개정 정책 결정 | A/B/C 중 하나를 선택하고 서비스/화면 조건 정렬 |

### P1. 상태 가시화와 액션 정합성

| 개선 | 내용 |
|---|---|
| 액션 가능 여부 DTO | version별 `hasGraph`, `hasStartNode`, `publishable`, `activatable`, `reason` 계산 |
| 게시 버튼 사전 비활성화 | 빈 초안에서 게시 버튼을 누를 수 없게 하고 사유를 근접 표시 |
| 수정 버튼 상태 조건 | `DRAFT`/`INACTIVE`에서만 수정 링크 노출 또는 `ACTIVE`에서 비활성 안내 |
| `active_version_id` 표시 | 상세에 현재 활성 버전과 게시 버전을 구분 표시 |
| form POST 예외 공통 처리 | publish/activate/deactivate/new draft/update 실패를 화면용 Flash로 통일 |

### P2. 관리 기능 보강

| 개선 | 내용 |
|---|---|
| 카테고리 관리 화면 | 신규 환경에서 첫 시나리오 등록이 막히지 않도록 화면 제공 |
| 키워드/유사어 편집 화면 | REST API로만 가능한 매칭 데이터 관리를 상세에서 연결 |
| 삭제 버튼 | `DRAFT`/`INACTIVE` 시나리오 삭제 UX 제공 |
| 목록 필터 UI | `status` 파라미터와 화면 필터를 연결 |
| 미리보기 레이아웃 정리 | Layer 용도면 팝업/레이어 진입으로 제한하고, 독립 페이지면 관리자 공통 레이아웃 적용 |

## 11. 검증 후보

| 케이스 | 기대 결과 |
|---|---|
| 카테고리 없는 신규 환경 | 등록 화면에서 카테고리 생성 또는 명확한 차단 안내 제공 |
| 신규 시나리오 등록 직후 | 상세에 version 1 `DRAFT`, 그래프 편집 선행 안내, 게시/미리보기 제한 |
| 기존 `DRAFT`가 있는 상태에서 새 초안 클릭 | 중복 생성 방지 또는 명시 확인 |
| 그래프 저장 전 게시 클릭 | 화면 이탈 없이 상세에서 "그래프 저장 필요" 안내 |
| 저장 그래프 도달성 오류 후 게시 | 상세 또는 편집 화면에서 오류 위치/원인 안내 |
| `DRAFT`/`INACTIVE` + `PUBLISHED` | 활성화 버튼 노출 및 성공 시 `ACTIVE`, `active_version_id` 설정 |
| `ACTIVE`에서 새 버전 게시 | 선택한 정책 A/B/C에 맞게 금지, 즉시 교체, 별도 교체 중 하나로 동작 |
| `ACTIVE`에서 수정 클릭 | 버튼 미노출/비활성 또는 서비스 실패가 화면 Flash로 복귀 |
| 비활성화 후 재활성화 | `INACTIVE` + `PUBLISHED` 버전에서 활성화 가능 |

## 12. 커밋 후보

- 커밋 메시지: `docs(scenario): review scenario registration workflow end-to-end`
- 대상 파일: `docs/Scenario_Registration_Workflow_재검토.md`
- 작업 분류: 문서 단독, 코드 무수정
