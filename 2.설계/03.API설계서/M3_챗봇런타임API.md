# M3 챗봇 런타임 API

> 작성일: 2026-05-08  
> 원칙: `ApiResponse<T>`, `PageResponse<T>` 사용. 사용자 `/chat/api/**`, 관리자 `/admin/api/chat/**` 분리.

## 1. 확정 결정값

- 비로그인 사용자는 HttpOnly `anonymous_id` 쿠키(UUID, 1년)와 서버 세션으로 식별한다.
- 대화 상태는 `chat_session` DB 테이블에 저장한다.
- fallback은 M3에서 키워드 검색 결과 카드 + 안내를 반환하고, M4 검색 구현 후 추천 시나리오 분기로 강화한다.
- 추천 질문은 운영자 고정값을 우선 노출하고, 캐시 없이 매 요청 DB 조회로 처리한다. 최근 사용량 보강은 M6 통계 이후 적용한다.
- 대화 이력은 90일 보존 후 파기한다.
- 만족도는 `UP`/`DOWN`과 선택 코멘트를 수집한다.
- 조건식 DSL은 M3에서 실행하지 않고 `scenario_node_option` 단순 분기만 지원한다.
- 사용자 채널은 전용 페이지 `/chat` 우선이다.

## 2. 공통 규칙

- 비로그인 사용 가능 API도 CSRF 쿠키 토큰을 검증한다.
- 상태 변경 요청은 `X-CSRF-TOKEN` 또는 Spring Security 기본 CSRF 헤더를 사용한다.
- 본인 세션 검증은 `anonymous_id` 쿠키와 `chat_session.anonymous_id` 일치로 처리한다. 로그인 사용자는 `user_id`도 보조 검증한다.
- 만료 세션은 410 Gone과 `CHAT_SESSION_EXPIRED`를 반환한다.
- 모든 입력은 `입력검증표준.md`를 따르며 free-text 500자, feedback comment 1,000자 이하로 제한한다.
- 응답에는 원문 사용자 입력을 과도하게 되돌려주지 않는다. 필요한 경우 마스킹된 요약만 포함한다.

## 3. 사용자 엔드포인트

| Method | Path | 요약 | 인증 |
|---|---|---|---|
| GET | `/chat/api/scenarios` | 활성 시나리오 목록 | 비로그인 가능 |
| POST | `/chat/api/sessions` | 새 세션 시작 | 비로그인 가능 |
| GET | `/chat/api/sessions/{id}` | 세션 현재 상태 | 본인만 |
| POST | `/chat/api/sessions/{id}/select-option` | 버튼 선택 진행 | 본인만 |
| POST | `/chat/api/sessions/{id}/free-text` | 자유 텍스트 매칭/fallback | 본인만 |
| GET | `/chat/api/sessions/{id}/history` | 대화 이력 페이지 | 본인만 |
| GET | `/chat/api/history` | 본인 이력 목록 | 본인만 |
| POST | `/chat/api/sessions/{id}/end` | 세션 종료 | 본인만 |
| POST | `/chat/api/messages/{messageId}/feedback` | 만족도 등록 | 본인 메시지만 |
| GET | `/chat/api/recommendations` | 추천 질문 | 비로그인 가능 |

### 3.1 `GET /chat/api/scenarios`

요청 파라미터:

| 이름 | 필수 | 설명 |
|---|---|---|
| `categoryId` | N | 카테고리 필터 |
| `keyword` | N | 제목 검색. 100자 이하 |

응답:

```json
{
  "success": true,
  "data": [
    {
      "scenarioId": 10,
      "title": "회원가입 안내",
      "description": "회원가입과 로그인 절차 안내",
      "categoryName": "계정",
      "recommendationLabel": "회원가입은 어떻게 하나요?"
    }
  ],
  "error": null,
  "timestamp": "2026-05-08T10:00:00+09:00"
}
```

### 3.2 `POST /chat/api/sessions`

요청:

```json
{
  "scenarioId": 10
}
```

응답:

```json
{
  "success": true,
  "data": {
    "sessionId": "7d889c37-0b7d-45de-83c7-0b2f6f71df0d",
    "scenarioId": 10,
    "versionId": 15,
    "state": "ACTIVE",
    "currentNode": {
      "nodeId": 101,
      "nodeType": "QUESTION",
      "content": "무엇을 도와드릴까요?",
      "options": [
        {"optionId": 1001, "label": "가입 방법"}
      ]
    },
    "expiresAt": "2026-05-08T10:30:00+09:00"
  },
  "error": null,
  "timestamp": "2026-05-08T10:00:00+09:00"
}
```

처리 규칙:

- `anonymous_id` 쿠키가 없으면 UUIDv4를 생성해 발급한다.
- 최신 `PUBLISHED` 버전이 없으면 `SCENARIO_ACTIVE_VERSION_REQUIRED`를 반환한다.
- 시작 메시지는 `SYSTEM` 또는 `BOT` 메시지로 `chat_message`에 저장한다.

### 3.3 `GET /chat/api/sessions/{id}`

응답:

```json
{
  "success": true,
  "data": {
    "sessionId": "7d889c37-0b7d-45de-83c7-0b2f6f71df0d",
    "state": "ACTIVE",
    "currentNodeId": 101,
    "lastMessageSeq": 3,
    "expiresAt": "2026-05-08T10:30:00+09:00"
  },
  "error": null,
  "timestamp": "2026-05-08T10:05:00+09:00"
}
```

### 3.4 `POST /chat/api/sessions/{id}/select-option`

권장 헤더:

| 이름 | 필수 | 설명 |
|---|---|---|
| `X-Request-Id` | N | 멱등성 추적용 클라이언트 요청 ID |

요청:

```json
{
  "optionId": 1001
}
```

정상 응답:

```json
{
  "success": true,
  "data": {
    "sessionId": "7d889c37-0b7d-45de-83c7-0b2f6f71df0d",
    "state": "ACTIVE",
    "userMessage": {
      "messageId": 201,
      "seq": 4,
      "content": "가입 방법"
    },
    "botMessage": {
      "messageId": 202,
      "seq": 5,
      "nodeId": 102,
      "nodeType": "ANSWER",
      "content": "회원가입은 우측 상단 회원가입 버튼에서 진행할 수 있습니다.",
      "options": [
        {"optionId": 1002, "label": "로그인 문제"}
      ]
    },
    "expiresAt": "2026-05-08T10:35:00+09:00"
  },
  "error": null,
  "timestamp": "2026-05-08T10:05:00+09:00"
}
```

실패 예시:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "CHAT_INVALID_OPTION",
    "message": "현재 단계에서 선택할 수 없는 옵션입니다.",
    "fieldErrors": []
  },
  "timestamp": "2026-05-08T10:05:00+09:00"
}
```

처리 규칙:

- 옵션은 현재 노드의 enabled 옵션이어야 한다.
- 현재 노드가 `END`이면 `CHAT_NODE_TERMINAL`을 반환한다.
- 사용자 선택 메시지 저장, 다음 봇 메시지 저장, 세션 현재 노드 갱신은 단일 트랜잭션으로 처리한다.

### 3.5 `POST /chat/api/sessions/{id}/free-text`

요청:

```json
{
  "text": "비밀번호를 잊어버렸어요"
}
```

현재 노드 옵션 키워드 매칭 응답:

```json
{
  "success": true,
  "data": {
    "matchType": "CURRENT_OPTION",
    "matchedOptionId": 1004,
    "state": "ACTIVE",
    "botMessage": {
      "messageId": 220,
      "nodeId": 110,
      "content": "비밀번호 재설정은 로그인 화면의 비밀번호 찾기에서 진행합니다.",
      "options": []
    }
  },
  "error": null,
  "timestamp": "2026-05-08T10:06:00+09:00"
}
```

fallback 카드 응답:

```json
{
  "success": true,
  "data": {
    "matchType": "FALLBACK",
    "state": "ACTIVE",
    "botMessage": {
      "messageId": 221,
      "content": "정확한 시나리오를 찾지 못했습니다. 관련 질문을 확인해 주세요.",
      "payload": {
        "cards": [
          {"scenarioId": 10, "label": "회원가입 안내"},
          {"scenarioId": 11, "label": "비밀번호 재설정"}
        ]
      }
    }
  },
  "error": null,
  "timestamp": "2026-05-08T10:06:00+09:00"
}
```

NO_MATCH 응답:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "CHAT_NO_MATCH",
    "message": "일치하는 답변을 찾지 못했습니다.",
    "fieldErrors": []
  },
  "timestamp": "2026-05-08T10:06:00+09:00"
}
```

매칭 순서:

1. 현재 노드 옵션 라벨/키워드 매칭
2. 전역 시나리오 키워드/유사어 매칭
3. M4 검색 fallback stub 호출
4. 매칭 실패 시 `chat_failure`에 `NO_MATCH` 기록

M4 fallback stub 인터페이스:

```java
FallbackSearchResult search(String text, Long currentScenarioId, UUID sessionId);
```

`chat_failure.detail`은 `M3_챗봇런타임ERD.md`의 표준 키 사전을 따른다. `NO_MATCH` 기록 시 `inputLength`, `inputSummary`, `currentNodeId`, `candidateOptionIds`, `matchedScenarioIds`, `fallbackSource`, `requestId`를 우선 채운다.

### 3.6 `GET /chat/api/sessions/{id}/history`

요청 파라미터:

| 이름 | 기본 | 설명 |
|---|---:|---|
| `page` | 0 | 0부터 시작 |
| `size` | 20 | 최대 100 |

응답:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "messageId": 201,
        "seq": 4,
        "direction": "USER",
        "content": "가입 방법",
        "createdAt": "2026-05-08T10:05:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  },
  "error": null,
  "timestamp": "2026-05-08T10:07:00+09:00"
}
```

### 3.7 `GET /chat/api/history`

요청 파라미터:

| 이름 | 기본 | 설명 |
|---|---:|---|
| `page` | 0 | 0부터 시작 |
| `size` | 20 | 최대 50 |
| `state` | 전체 | `ACTIVE`, `COMPLETED`, `ABANDONED`, `EXPIRED` allow-list |
| `from` | 90일 전 | 조회 시작일. `to`와 함께 최대 90일 범위만 허용 |
| `to` | 현재 | 조회 종료일. 현재 시각 이후 값은 현재 시각으로 보정 |

응답:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "sessionId": "7d889c37-0b7d-45de-83c7-0b2f6f71df0d",
        "scenarioId": 10,
        "scenarioTitle": "회원가입 안내",
        "state": "EXPIRED",
        "startedAt": "2026-05-08T10:00:00+09:00",
        "lastActivityAt": "2026-05-08T10:20:00+09:00",
        "messageCount": 6,
        "lastBotMessageSummary": "회원가입은 우측 상단 회원가입 버튼에서 진행할 수..."
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "error": null,
  "timestamp": "2026-05-08T10:30:00+09:00"
}
```

처리 규칙:

- 정렬은 `lastActivityAt DESC`, `sessionId DESC`다.
- `anonymous_id` 쿠키가 없으면 빈 페이지를 반환하고 새 식별자를 강제 발급하지 않는다.
- 90일 보존 범위를 초과한 세션은 응답에서 제외한다.
- `EXPIRED` 상태도 이력 목록에 포함한다.
- 응답에는 IP, User-Agent, `anonymous_id` 원문을 포함하지 않는다.
- `lastBotMessageSummary`는 최근 봇 메시지 기준 80자 이하 마스킹 요약이다.

### 3.8 `POST /chat/api/sessions/{id}/end`

요청:

```json
{
  "reason": "USER_REQUEST"
}
```

응답:

```json
{
  "success": true,
  "data": {
    "sessionId": "7d889c37-0b7d-45de-83c7-0b2f6f71df0d",
    "state": "ABANDONED"
  },
  "error": null,
  "timestamp": "2026-05-08T10:08:00+09:00"
}
```

### 3.9 `POST /chat/api/messages/{messageId}/feedback`

요청:

```json
{
  "rating": "DOWN",
  "comment": "원하는 답변이 아니었습니다."
}
```

응답:

```json
{
  "success": true,
  "data": {
    "feedbackId": 501,
    "messageId": 202,
    "rating": "DOWN"
  },
  "error": null,
  "timestamp": "2026-05-08T10:09:00+09:00"
}
```

중복 등록은 `CHAT_FEEDBACK_DUPLICATE` 409로 반환한다.

### 3.10 `GET /chat/api/recommendations`

응답:

```json
{
  "success": true,
  "data": [
    {"scenarioId": 10, "label": "회원가입은 어떻게 하나요?", "priority": 10},
    {"scenarioId": 11, "label": "비밀번호를 잊었어요", "priority": 20}
  ],
  "error": null,
  "timestamp": "2026-05-08T10:00:00+09:00"
}
```

처리 규칙:

- 추천 질문은 캐시를 사용하지 않고 매 요청 DB에서 조회한다.
- `chat_recommendation.enabled=true`이고 연결 시나리오가 활성 상태인 항목만 반환한다.
- 연결 시나리오의 카테고리가 비활성화된 경우 사용자 응답에서 제외한다.
- 정렬은 priority ASC, id ASC다.

## 4. 관리자 엔드포인트

| Method | Path | 요약 | 권한 |
|---|---|---|---|
| GET | `/admin/api/chat/sessions` | 세션 검색 | ADMIN, OPERATOR |
| GET | `/admin/api/chat/sessions/{id}` | 세션 상세 + 메시지 트레이스 | ADMIN, OPERATOR |
| GET | `/admin/api/chat/failures` | 답변 실패 큐 | ADMIN, OPERATOR |
| POST | `/admin/api/chat/failures/{id}/review` | 처리 표시 + 코멘트 | ADMIN, OPERATOR |
| GET | `/admin/api/chat/feedback` | 피드백 목록 | ADMIN, OPERATOR |
| GET | `/admin/api/chat/recommendations` | 추천 질문 목록 | ADMIN, OPERATOR |
| POST | `/admin/api/chat/recommendations` | 추천 질문 등록 | ADMIN, OPERATOR |
| GET | `/admin/api/chat/recommendations/{id}` | 추천 질문 상세 | ADMIN, OPERATOR |
| PUT | `/admin/api/chat/recommendations/{id}` | 추천 질문 수정 | ADMIN, OPERATOR |
| DELETE | `/admin/api/chat/recommendations/{id}` | 추천 질문 삭제 | ADMIN, OPERATOR |

세션 검색 필터:

| 이름 | 설명 |
|---|---|
| `state` | `ACTIVE`, `COMPLETED`, `ABANDONED`, `EXPIRED` |
| `scenarioId` | 시나리오 |
| `from`, `to` | 시작일 범위 |
| `page`, `size` | 페이징 |

실패 처리 요청:

```json
{
  "reviewComment": "키워드 보강 필요. M4 검색 사전에 반영 후보."
}
```

관리자 상세 응답에는 `anonymous_id`, IP 원문, UA 원문을 포함하지 않는다.

추천 질문 목록 필터:

| 이름 | 설명 |
|---|---|
| `scenarioId` | 연결 시나리오 |
| `enabled` | 활성 여부 |
| `keyword` | label 부분 검색. 100자 이하 |
| `page`, `size` | 페이징. size 최대 50 |

추천 질문 등록/수정 요청:

```json
{
  "scenarioId": 10,
  "label": "회원가입은 어떻게 하나요?",
  "priority": 10,
  "enabled": true
}
```

추천 질문 응답:

```json
{
  "success": true,
  "data": {
    "recommendationId": 301,
    "scenarioId": 10,
    "scenarioTitle": "회원가입 안내",
    "label": "회원가입은 어떻게 하나요?",
    "priority": 10,
    "enabled": true,
    "createdAt": "2026-05-08T10:00:00+09:00",
    "updatedAt": "2026-05-08T10:00:00+09:00"
  },
  "error": null,
  "timestamp": "2026-05-08T10:00:00+09:00"
}
```

추천 질문 처리 규칙:

- label은 1~200자, priority는 0~9999만 허용한다.
- 정렬은 priority ASC, id ASC다.
- `enabled=false` 저장 즉시 사용자 `GET /chat/api/recommendations` 응답에서 제외한다.
- 카테고리 비활성화 변경 저장 즉시 해당 카테고리에 속한 시나리오의 추천 질문은 사용자 `GET /chat/api/recommendations` 응답에서 제외한다.
- 추천 질문 사용자 응답은 캐시하지 않으므로 추천 질문 CRUD나 카테고리 비활성화 시 별도 추천 질문 캐시 무효화 호출은 없다.
- 삭제는 물리 삭제를 기본으로 하되 감사 로그에는 label 전체 대신 recommendationId, scenarioId, action만 남긴다.

## 5. 추가 에러 코드

`공통응답에러코드.md` 보충 대상:

| 코드 | HTTP | 설명 |
|---|---:|---|
| `CHAT_SESSION_NOT_FOUND` | 404 | 챗봇 세션 없음 |
| `CHAT_SESSION_EXPIRED` | 410 | 챗봇 세션 만료 |
| `CHAT_SESSION_FORBIDDEN` | 403 | 본인 세션 아님 |
| `CHAT_INVALID_OPTION` | 409 | 현재 노드에서 선택할 수 없는 옵션 |
| `CHAT_NODE_TERMINAL` | 409 | 종료 노드에서 추가 진행 요청 |
| `CHAT_FEEDBACK_DUPLICATE` | 409 | 이미 등록된 피드백 |
| `CHAT_NO_MATCH` | 404 | 자유 텍스트 매칭 실패 |
| `CHAT_PII_BLOCKED` | 400 | 저장할 수 없는 개인정보 패턴 탐지 |
| `RATE_LIMIT_EXCEEDED` | 429 | 요청량 제한 초과 |

## 6. 감사 및 로그

| 이벤트 | 대상 | 비고 |
|---|---|---|
| `CHAT_FAILURE_REVIEW` | `chat_failure` | 관리자 확인 처리 |
| `CHAT_SESSION_FORCE_VIEW` | `chat_session` | 관리자 상세 조회. 필요 시 M6에서 감사 대상 확정 |

사용자 메시지 원문은 감사 로그에 저장하지 않고 운영 로그에는 100자 이하 마스킹 요약만 남긴다.
`chat_failure.detail`도 동일한 마스킹 원칙을 적용하며 stack trace, CSRF 토큰, 세션 ID, `anonymous_id` 원문은 포함하지 않는다.

## 7. PR 수용 기준

- [ ] 사용자/관리자 URL과 권한이 화면 목록과 일치한다.
- [ ] select-option 정상/실패, free-text 매칭/fallback/NO_MATCH, session history, 본인 이력 목록 응답 예시가 구현 기준으로 사용 가능하다.
- [ ] 추천 질문 CRUD, 캐시 미사용, `enabled=false`와 카테고리 비활성화 즉시 제외 규칙이 구현 기준으로 사용 가능하다.
- [ ] 만료 세션은 410 + `CHAT_SESSION_EXPIRED`로 통일된다.
- [ ] CSRF, 입력 검증, 본인 세션 검증이 비로그인 사용자에게도 적용된다.
