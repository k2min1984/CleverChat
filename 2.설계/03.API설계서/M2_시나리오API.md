# M2 시나리오 API

> 작성일: 2026-05-08
> 권한: `ROLE_ADMIN`, `ROLE_OPERATOR`

## 1. 화면 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| GET | `/admin/scenarios` | 시나리오 목록 화면 |
| GET | `/admin/scenarios/new` | 시나리오 등록 화면 |
| GET | `/admin/scenarios/{scenarioId}` | 시나리오 상세/편집 화면 |
| GET | `/admin/scenarios/{scenarioId}/preview` | 시나리오 미리보기 화면 |

## 2. REST 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| GET | `/admin/api/scenarios` | 시나리오 목록/검색 |
| POST | `/admin/api/scenarios` | 시나리오 생성 |
| GET | `/admin/api/scenarios/{scenarioId}` | 시나리오 상세 |
| PUT | `/admin/api/scenarios/{scenarioId}` | 시나리오 기본정보 수정 |
| DELETE | `/admin/api/scenarios/{scenarioId}` | 시나리오 논리 삭제 |
| POST | `/admin/api/scenarios/{scenarioId}/versions` | 새 초안 버전 생성 |
| GET | `/admin/api/scenarios/{scenarioId}/versions/{versionId}` | 버전 상세 |
| PUT | `/admin/api/scenarios/{scenarioId}/versions/{versionId}` | 버전 노드 그래프 저장 |
| POST | `/admin/api/scenarios/{scenarioId}/versions/{versionId}/publish` | 버전 게시 |
| POST | `/admin/api/scenarios/{scenarioId}/activate` | 시나리오 활성화 |
| POST | `/admin/api/scenarios/{scenarioId}/deactivate` | 시나리오 비활성화 |
| GET | `/admin/api/categories` | 카테고리 목록 |
| POST | `/admin/api/categories` | 카테고리 생성 |
| PUT | `/admin/api/categories/{categoryId}` | 카테고리 수정 |
| DELETE | `/admin/api/categories/{categoryId}` | 카테고리 비활성화 |
| GET | `/admin/api/scenarios/{scenarioId}/keywords` | 키워드/유사어 목록 |
| PUT | `/admin/api/scenarios/{scenarioId}/keywords` | 키워드/유사어 일괄 저장 |

## 3. 요청 DTO 초안

### 시나리오 생성

```json
{
  "categoryId": 1,
  "title": "회원가입 안내",
  "description": "회원가입 관련 질문 흐름"
}
```

### 버전 그래프 저장

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
      "options": [
        {
          "label": "가입 방법",
          "nextNodeKey": "join-guide",
          "sortOrder": 1
        }
      ]
    }
  ]
}
```

### 키워드 저장

```json
{
  "keywords": [
    {
      "keyword": "회원가입",
      "weight": 100,
      "synonyms": [
        {"synonym": "가입", "weight": 80}
      ]
    }
  ]
}
```

## 4. 업무 규칙

- 게시(`publish`) 전 시작 노드가 반드시 존재해야 한다.
- 옵션의 `nextNodeKey`는 같은 버전의 노드를 가리켜야 한다.
- `END` 노드는 옵션을 가질 수 없다.
- 활성화는 게시된 버전이 있을 때만 가능하다.
- 이미 게시된 버전은 직접 수정하지 않고 새 초안 버전을 만든다.
- 삭제는 논리 삭제로 처리하고, 활성 시나리오는 삭제 전 비활성화한다.

## 4.1 M2 결정사항 9절 동기화

- `/admin/**` 및 `/admin/api/**`의 M2 시나리오 관리 권한은 `ADMIN`, `OPERATOR`를 기준으로 한다.
- OPERATOR 운영 기본 계정은 비활성 상태로 시딩하고 `must_change_password=true`를 기본값으로 둔다. `users.must_change_password` 컬럼은 Flyway V3.1에서 분리 추가한다.
- 세션성 식별 쿠키는 `SameSite=Lax`, `Secure=true`, `HttpOnly=true`를 기본값으로 한다. AJAX CSRF 토큰 쿠키는 헤더 전송을 위해 `HttpOnly=false` 예외를 둔다.
- 프록시 헤더 처리는 `server.forward-headers-strategy=framework`를 기준으로 하며, 부팅 경고 로그는 prod 프로파일에서만 출력한다.
- M3 키워드/유사어 매칭 캐시 TTL 기본값은 300초이며, M2 게시/키워드 저장 성공 후 무효화 훅 연계를 후속 구현 기준으로 둔다.

## 5. 감사 대상

| Action | Target |
|---|---|
| `SCENARIO_CREATE` | scenario |
| `SCENARIO_UPDATE` | scenario |
| `SCENARIO_DELETE` | scenario |
| `SCENARIO_VERSION_SAVE` | scenario_version |
| `SCENARIO_VERSION_PUBLISH` | scenario_version |
| `SCENARIO_ACTIVATE` | scenario |
| `SCENARIO_DEACTIVATE` | scenario |
| `SCENARIO_KEYWORD_SAVE` | scenario_keyword |
