# M4 검색 API

> 작성일: 2026-05-11
> 원칙: 사용자 검색은 M3 `free-text` 응답에 통합하고, M4는 관리자 운영 API를 제공한다.

## 1. 공통 규칙

- 모든 관리자 API는 `ADMIN/OPERATOR` 권한을 요구한다.
- 상태 변경 요청은 CSRF 토큰을 요구한다.
- 검색어는 1~200자만 허용한다.
- PII 차단 시 `SEARCH_PII_BLOCKED`를 반환하고 원문을 저장하지 않는다.
- 응답은 공통 `ApiResponse<T>`와 `PageResponse<T>` 형식을 따른다.
- 목록 조회 API는 별도 정렬 파라미터가 명시되지 않은 경우 기본 정렬을 `created_at DESC`로 적용한다. 일집계 기반 응답(`/admin/api/search/popular` 등)은 `stat_date DESC, search_count DESC` 순으로 정렬한다.

## 2. 관리자 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| GET | `/admin/api/search/logs` | 검색 로그 조회 |
| GET | `/admin/api/search/blocks` | PII 차단 검색 로그 조회 |
| GET | `/admin/api/search/popular` | 인기 검색어 조회 |
| POST | `/admin/api/search/test` | 검색 테스트 실행 |
| POST | `/admin/api/search/popular/rebuild` | 특정 일자 인기 검색어 재집계 |
| DELETE | `/admin/api/search/logs/expired` | 보존 기간 초과 로그 파기 |

### 2.1 `GET /admin/api/search/logs`

파라미터:

| 이름 | 필수 | 설명 |
|---|---|---|
| `query` | N | 정규화 검색어 부분 일치 |
| `source` | N | `CHAT_FALLBACK`, `ADMIN_TEST`, `API` |
| `from` | N | 조회 시작 일시 |
| `to` | N | 조회 종료 일시 |
| `page` | N | 0부터 시작 |
| `size` | N | 기본 20, 최대 100 |

### 2.2 `POST /admin/api/search/test`

요청:

```json
{
  "query": "회원 가입",
  "limit": 5
}
```

응답:

```json
{
  "success": true,
  "data": {
    "normalizedQuery": "회원 가입",
    "resultCount": 2,
    "results": [
      {
        "scenarioId": 10,
        "scenarioTitle": "회원가입 안내",
        "matchedField": "TITLE",
        "score": 0.92,
        "snippet": "회원가입 절차를 안내합니다."
      }
    ]
  },
  "error": null,
  "timestamp": "2026-05-11T10:00:00+09:00"
}
```

### 2.3 `GET /admin/api/search/blocks`

파라미터:

| 이름 | 필수 | 설명 |
|---|---|---|
| `piiType` | N | `RRN`, `CARD`, `PHONE`, `EMAIL` 등 차단 PII 유형 부분 일치 |
| `source` | N | `CHAT_FALLBACK`, `ADMIN_TEST`, `API` |
| `from` | N | 조회 시작 일시 |
| `to` | N | 조회 종료 일시 |
| `page` | N | 0부터 시작 |
| `size` | N | 기본 20, 최대 100 |

응답:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 31,
        "queryLength": 14,
        "piiTypes": ["RRN"],
        "source": "CHAT_FALLBACK",
        "anonymousIdHash": "b8f2d4c6a0e9f1d3",
        "userId": null,
        "createdAt": "2026-05-11T10:15:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "error": null,
  "timestamp": "2026-05-11T10:16:00+09:00"
}
```

실패 코드:

| 코드 | HTTP | 기준 |
|---|---:|---|
| `COMMON_INVALID_PARAMETER` | 400 | `source`, 일시, 페이징 파라미터 형식 오류 |
| `COMMON_FORBIDDEN` | 403 | ADMIN/OPERATOR 권한 없음 |

### 2.4 `GET /admin/api/search/popular`

파라미터:

| 이름 | 필수 | 설명 |
|---|---|---|
| `date` | N | 집계 기준일. 미지정 시 전일 KST |
| `from` | N | 조회 시작일. `date`와 동시 사용 불가 |
| `to` | N | 조회 종료일. `date`와 동시 사용 불가 |
| `limit` | N | 기본 20, 최대 100 |
| `includeNoResult` | N | 무결과 검색어 포함 여부. 기본 `true` |

응답:

```json
{
  "success": true,
  "data": {
    "from": "2026-05-10",
    "to": "2026-05-10",
    "items": [
      {
        "statDate": "2026-05-10",
        "normalizedQuery": "회원 가입",
        "queryTextSample": "회원 가입",
        "searchCount": 42,
        "noResultCount": 3,
        "topResultScenarioId": 10
      }
    ]
  },
  "error": null,
  "timestamp": "2026-05-11T10:20:00+09:00"
}
```

실패 코드:

| 코드 | HTTP | 기준 |
|---|---:|---|
| `COMMON_INVALID_PARAMETER` | 400 | 날짜 범위, limit, includeNoResult 형식 오류 |
| `COMMON_FORBIDDEN` | 403 | ADMIN/OPERATOR 권한 없음 |

### 2.5 `POST /admin/api/search/popular/rebuild`

요청:

```json
{
  "statDate": "2026-05-10"
}
```

응답:

```json
{
  "success": true,
  "data": {
    "statDate": "2026-05-10",
    "rebuiltCount": 25,
    "deletedBeforeRebuild": 25,
    "idempotent": true,
    "rebuiltAt": "2026-05-11T10:30:00+09:00"
  },
  "error": null,
  "timestamp": "2026-05-11T10:30:00+09:00"
}
```

실패 코드:

| 코드 | HTTP | 기준 |
|---|---:|---|
| `COMMON_INVALID_PARAMETER` | 400 | statDate 누락 또는 날짜 형식 오류 |
| `COMMON_FORBIDDEN` | 403 | ADMIN/OPERATOR 권한 없음 |
| `COMMON_CSRF_INVALID` | 403 | CSRF 토큰 누락 또는 불일치 |
| `SEARCH_AGGREGATION_FAILED` | 500 | 재집계 처리 실패 |

### 2.6 `DELETE /admin/api/search/logs/expired`

파라미터:

| 이름 | 필수 | 설명 |
|---|---|---|
| `retentionDays` | N | 보존 일수. 기본 90, 최소 30 |
| `dryRun` | N | 삭제 없이 대상 건수만 계산. 기본 `false` |

응답:

```json
{
  "success": true,
  "data": {
    "retentionDays": 90,
    "cutoff": "2026-02-10T00:00:00+09:00",
    "deletedSearchLogs": 1200,
    "deletedBlockLogs": 18,
    "dryRun": false
  },
  "error": null,
  "timestamp": "2026-05-11T03:00:00+09:00"
}
```

`dryRun=true` 응답 예시:

```json
{
  "success": true,
  "data": {
    "retentionDays": 90,
    "cutoff": "2026-02-10T00:00:00+09:00",
    "deletedSearchLogs": 0,
    "deletedBlockLogs": 0,
    "wouldDeleteSearchLogs": 1200,
    "wouldDeleteBlockLogs": 18,
    "dryRun": true
  },
  "error": null,
  "timestamp": "2026-05-11T03:00:00+09:00"
}
```

실패 코드:

| 코드 | HTTP | 기준 |
|---|---:|---|
| `COMMON_INVALID_PARAMETER` | 400 | retentionDays, dryRun 형식 오류 |
| `COMMON_FORBIDDEN` | 403 | ADMIN/OPERATOR 권한 없음 |
| `COMMON_CSRF_INVALID` | 403 | CSRF 토큰 누락 또는 불일치 |
| `SEARCH_RETENTION_FAILED` | 500 | 보존 기간 초과 로그 파기 실패 |

## 3. 에러

| 코드 | HTTP | 기준 |
|---|---:|---|
| `SEARCH_QUERY_EMPTY` | 400 | trim 후 빈 문자열 |
| `SEARCH_QUERY_TOO_LONG` | 400 | 200자 초과 |
| `SEARCH_PII_BLOCKED` | 400 | 고위험 PII 탐지 |
| `SEARCH_RESULT_EMPTY` | 404 | 관리자 테스트에서 결과 없음 |
| `SEARCH_AGGREGATION_FAILED` | 500 | 인기 검색어 재집계 실패 |
| `SEARCH_RETENTION_FAILED` | 500 | 보존 기간 초과 로그 파기 실패 |
