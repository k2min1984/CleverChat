# M10 외부 REST API Scaffold

## 1차 기준

- 외부 REST API prefix는 `/external/api/v1/**`로 고정한다.
- 1차 구현은 status stub만 제공하며, 실제 business API는 공개하지 않는다.
- 기본값은 `CLEVERCHAT_EXTERNAL_API_ENABLED=false`이며, disabled 상태에서는 업무 데이터가 노출되지 않는다.
- 외부 CORS allowlist는 아직 열지 않는다.

## Stub API

| Method | Path | 설명 |
|---|---|---|
| GET | `/external/api/v1/status` | 외부 API 활성 상태와 v1 capability metadata 확인 |

enabled 상태에서 유효한 API key가 있을 때만 `ApiResponse.success=true`로 `version`, `enabled`, `capabilities`를 반환한다.

## 인증 기준

- API key 원문은 DB, 로그, 응답에 저장하지 않는다.
- 운영 설정은 `CLEVERCHAT_EXTERNAL_API_KEY_SHA256`에 SHA-256 hex digest만 둔다.
- 요청은 `Authorization: Bearer <key>` 또는 `X-CleverChat-Api-Key: <key>`를 허용한다.
- enabled 상태인데 key hash가 비어 있으면 fail-closed로 처리한다.

## 후속 범위

- DB 기반 API key 발급/폐기/권한 scope
- 외부 CORS allowlist
- OpenAPI 문서
- API key별 rate limit
- 실제 시나리오/검색/상담 public business API
