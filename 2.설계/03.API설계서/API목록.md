# API/엔드포인트 목록

> 작성일: 2026-05-08
> 최종 정합화: 2026-05-27
> 기준: 현재 P0~M7.4 및 M3 대화 이력/만족도/관리자 세션 상세 구현 상태

| Method | Path | 유형 | 권한 | 설명 | 상태 |
|---|---|---|---|---|---|
| GET | `/` | View | 전체 | 홈 | 구현 |
| GET | `/login` | View | 전체 | 관리자 로그인 화면 | 구현 |
| POST | `/login` | Form | 전체 | 관리자 로그인 처리 | 구현 |
| POST | `/logout` | Form | 인증 사용자 | 로그아웃 | 구현 |
| GET | `/admin` | View | ADMIN/OPERATOR | 관리자 기본 화면 | 구현 |
| GET | `/admin/password-change` | View | 인증 사용자 | 최초 비밀번호 변경 화면 | 구현 |
| POST | `/admin/password-change` | Form | 인증 사용자 | 비밀번호 변경 처리 | 구현 |
| GET | `/actuator/health` | REST | 전체 | 헬스 체크 | 구현 |
| GET | `/chat` | View | 전체 | 챗봇 메인 | M3 구현 |
| GET | `/chat/history` | View | 본인 | 대화 이력 화면 | M3 구현 |
| GET | `/chat/api/scenarios` | REST | 전체 | 활성 시나리오 목록 | M3 구현 |
| POST | `/chat/api/sessions` | REST | 전체 | 챗봇 세션 시작, `anonymous_id` 쿠키 발급 | M3 구현 |
| GET | `/chat/api/sessions/{id}` | REST | 본인 | 챗봇 세션 현재 상태 | M3 구현 |
| GET | `/chat/api/sessions/{id}/history` | REST | 본인 | 세션 메시지 이력 | M3 구현 |
| GET | `/chat/api/history` | REST | 본인 | `anonymous_id` 기준 최근 90일 세션 이력 | M3 구현 |
| POST | `/chat/api/sessions/{id}/select-option` | REST | 본인 | 현재 노드 옵션 선택 | M3 구현 |
| POST | `/chat/api/sessions/{id}/free-text` | REST | 본인 | 자유 텍스트 매칭/fallback | M3 구현 |
| POST | `/chat/api/messages/{messageId}/feedback` | REST | 본인 | BOT 메시지 만족도 등록/갱신 | M3 구현 |
| GET | `/chat/api/recommendations` | REST | 전체 | 운영자 추천 질문 목록 | M3 구현 |
| GET | `/admin/chat/sessions` | View | ADMIN/OPERATOR | 챗봇 세션 목록 | M3 구현 |
| GET | `/admin/chat/sessions/{id}` | View | ADMIN/OPERATOR | 챗봇 세션 상세 | M3 구현 |
| GET | `/admin/chat/failures` | View | ADMIN/OPERATOR | 응답 실패 큐 | M3 구현 |
| GET | `/admin/chat/feedback` | View | ADMIN/OPERATOR | 챗봇 피드백 목록 | M3 구현 |
| GET | `/admin/chat/recommendations` | View | ADMIN/OPERATOR | 추천 질문 관리 | M3 구현 |
| GET | `/admin/api/chat/sessions` | REST | ADMIN/OPERATOR | 챗봇 세션 목록 API | M3 구현 |
| GET | `/admin/api/chat/sessions/{id}` | REST | ADMIN/OPERATOR | 챗봇 세션 상세 API | M3 구현 |
| GET | `/admin/api/chat/failures` | REST | ADMIN/OPERATOR | 응답 실패 큐 조회 | M3 구현 |
| POST | `/admin/api/chat/failures/{id}/review` | REST | ADMIN/OPERATOR | 실패 큐 확인 처리 | M3 구현 |
| GET | `/admin/api/chat/feedback` | REST | ADMIN/OPERATOR | 챗봇 피드백 조회 | M3 구현 |
| GET | `/admin/api/chat/recommendations` | REST | ADMIN/OPERATOR | 추천 질문 조회 | M3 구현 |
| POST | `/admin/api/chat/recommendations` | REST | ADMIN/OPERATOR | 추천 질문 생성/수정 | M3 구현 |
| DELETE | `/admin/api/chat/recommendations/{id}` | REST | ADMIN/OPERATOR | 추천 질문 삭제 | M3 구현 |
| GET | `/admin/scenarios` | View | ADMIN/OPERATOR | 시나리오 목록 | M2/P0 구현 |
| GET | `/admin/scenarios/new` | View | ADMIN/OPERATOR | 시나리오 등록 화면 | M2/P0 구현 |
| POST | `/admin/scenarios` | Form | ADMIN/OPERATOR | 시나리오 등록 처리 | M2/P0 구현 |
| GET | `/admin/scenarios/{id}` | View | ADMIN/OPERATOR | 시나리오 상세 | M2/P0 구현 |
| GET | `/admin/scenarios/{id}/edit` | View | ADMIN/OPERATOR | 시나리오 수정 화면 | M2/P0 구현 |
| POST | `/admin/scenarios/{id}` | Form | ADMIN/OPERATOR | 시나리오 수정 처리 | M2/P0 구현 |
| POST | `/admin/scenarios/{id}/versions` | Form | ADMIN/OPERATOR | 신규 DRAFT 생성 또는 기존 DRAFT 안내 | P0 구현 |
| POST | `/admin/scenarios/versions/{versionId}/publish` | Form | ADMIN/OPERATOR | 시나리오 버전 게시 | P0 구현 |
| POST | `/admin/scenarios/{id}/activate` | Form | ADMIN/OPERATOR | 시나리오 활성화 | P0 구현 |
| POST | `/admin/scenarios/{id}/deactivate` | Form | ADMIN/OPERATOR | 시나리오 비활성화 | P0 구현 |
| GET | `/admin/scenarios/versions/{versionId}/preview` | View | ADMIN/OPERATOR | 버전 미리보기 | P0 구현 |
| GET | `/admin/scenarios/{scenarioId}/versions/{versionId}/graph` | View | ADMIN/OPERATOR | DRAFT graph 편집 화면 | P0 구현 |
| GET | `/admin/api/scenarios` | REST | ADMIN/OPERATOR | 시나리오 목록 API | M2/P0 구현 |
| GET | `/admin/api/scenarios/{id}` | REST | ADMIN/OPERATOR | 시나리오 상세 API | M2/P0 구현 |
| POST | `/admin/api/scenarios` | REST | ADMIN/OPERATOR | 시나리오 생성 API | M2/P0 구현 |
| PUT | `/admin/api/scenarios/{id}` | REST | ADMIN/OPERATOR | 시나리오 수정 API | M2/P0 구현 |
| DELETE | `/admin/api/scenarios/{id}` | REST | ADMIN/OPERATOR | 시나리오 삭제 API | M2/P0 구현 |
| POST | `/admin/api/scenarios/{id}/versions` | REST | ADMIN/OPERATOR | 신규 DRAFT 생성 또는 기존 DRAFT 반환 | P0 구현 |
| GET | `/admin/api/scenarios/versions/{versionId}/graph` | REST | ADMIN/OPERATOR | graph JSON 조회 | P0 구현 |
| PUT | `/admin/api/scenarios/versions/{versionId}/graph` | REST | ADMIN/OPERATOR | DRAFT graph 저장 | P0 구현 |
| POST | `/admin/api/scenarios/versions/{versionId}/publish` | REST | ADMIN/OPERATOR | 버전 게시 API | P0 구현 |
| POST | `/admin/api/scenarios/{id}/activate/{versionId}` | REST | ADMIN/OPERATOR | 시나리오 활성화 API | P0 구현 |
| POST | `/admin/api/scenarios/{id}/deactivate` | REST | ADMIN/OPERATOR | 시나리오 비활성화 API | P0 구현 |
| GET | `/admin/api/scenario-categories` | REST | ADMIN/OPERATOR | 카테고리 조회 | M2 구현 |
| POST | `/admin/api/scenario-categories` | REST | ADMIN/OPERATOR | 카테고리 생성 | M2 구현 |
| PUT | `/admin/api/scenario-categories/{id}` | REST | ADMIN/OPERATOR | 카테고리 수정 | M2 구현 |
| GET | `/admin/api/scenarios/{scenarioId}/keywords` | REST | ADMIN/OPERATOR | 키워드/유사어 조회 | M2 구현 |
| PUT | `/admin/api/scenarios/{scenarioId}/keywords` | REST | ADMIN/OPERATOR | 키워드/유사어 replace 저장 | M2 구현 |
| GET | `/admin/search/logs` | View | ADMIN/OPERATOR | 검색 로그 화면 | M4 구현 |
| GET | `/admin/search/blocks` | View | ADMIN/OPERATOR | PII 차단 로그 화면 | M4 구현 |
| GET | `/admin/search/popular` | View | ADMIN/OPERATOR | 인기 검색어 화면 | M4 구현 |
| GET | `/admin/api/search/logs` | REST | ADMIN/OPERATOR | 검색 로그 조회 | M4 구현 |
| GET | `/admin/api/search/blocks` | REST | ADMIN/OPERATOR | PII 차단 로그 조회 | M4 구현 |
| GET | `/admin/api/search/popular` | REST | ADMIN/OPERATOR | 인기 검색어 조회 | M4 구현 |
| POST | `/admin/api/search/test` | REST | ADMIN/OPERATOR | 관리자 검색 테스트 | M4 구현 |
| POST | `/admin/api/search/popular/rebuild` | REST | ADMIN/OPERATOR | 인기 검색어 재집계 | M4 구현 |
| DELETE | `/admin/api/search/logs/expired` | REST | ADMIN/OPERATOR | 검색 로그 보존 정리 | M4 구현 |
| GET | `/admin/crawl-targets` | View | ADMIN/OPERATOR | 크롤링 URL 관리 | M5 구현 |
| GET | `/admin/crawl-documents` | View | ADMIN/OPERATOR | 수집 문서 조회 | M5 구현 |
| GET | `/admin/crawl-runs` | View | ADMIN/OPERATOR | 크롤링 실행 이력, status/failureCode 필터 | M5 구현 |
| GET | `/admin/api/crawl-targets` | REST | ADMIN/OPERATOR | 크롤링 대상 조회 | M5 구현 |
| POST | `/admin/api/crawl-targets` | REST | ADMIN/OPERATOR | 크롤링 대상 등록 | M5 구현 |
| PUT | `/admin/api/crawl-targets/{id}` | REST | ADMIN/OPERATOR | 크롤링 대상 수정 | M5 구현 |
| PUT | `/admin/api/crawl-targets/{id}/schedule` | REST | ADMIN/OPERATOR | 크롤링 interval/cron schedule 수정 | M5 구현 |
| POST | `/admin/api/crawl-targets/{id}/run` | REST | ADMIN/OPERATOR | 수동 크롤링 실행 | M5 구현 |
| GET | `/admin/api/crawl-documents` | REST | ADMIN/OPERATOR | 수집 문서 조회 | M5 구현 |
| GET | `/admin/api/crawl-runs` | REST | ADMIN/OPERATOR | 크롤링 실행 이력 조회, status/failureCode 필터 | M5 구현 |
| GET | `/admin/api/crawl-runs/failures` | REST | ADMIN/OPERATOR | 미확인 실패 실행 로그 조회, failureCode 필터 | M5 구현 |
| PUT | `/admin/api/crawl-runs/{id}/review` | REST | ADMIN/OPERATOR | 실패 실행 로그 확인 처리 | M5 구현 |
| DELETE | `/admin/api/crawl-runs/expired` | REST | ADMIN/OPERATOR | 크롤링 운영 데이터 보존 정리 | M5 구현 |
| GET | `/admin/statistics` | View | ADMIN | 운영 통계 대시보드 | M6 구현 |
| GET | `/admin/api/statistics/summary` | REST | ADMIN | 오늘/최근 7일 운영 지표 summary | M6 구현 |
| GET | `/admin/audit-logs` | View | ADMIN | 감사 로그 조회 | M6 구현 |
| GET | `/admin/api/audit-logs` | REST | ADMIN | 감사 로그 필터 조회 | M6 구현 |
| GET | `/admin/notices` | View | ADMIN/OPERATOR | 공지사항 관리 | M6 구현 |
| GET | `/admin/api/notices` | REST | ADMIN/OPERATOR | 공지 목록 조회 | M6 구현 |
| POST | `/admin/api/notices` | REST | ADMIN/OPERATOR | 공지 생성 | M6 구현 |
| PUT | `/admin/api/notices/{id}` | REST | ADMIN/OPERATOR | 공지 수정 | M6 구현 |
| DELETE | `/admin/api/notices/{id}` | REST | ADMIN/OPERATOR | 공지 비활성화 | M6 구현 |
| GET | `/admin/notifications` | View | ADMIN/OPERATOR | 외부 알림 관리 | M6 구현 |
| GET | `/admin/api/notifications/channels` | REST | ADMIN/OPERATOR | webhook 알림 채널 조회 | M6 구현 |
| POST | `/admin/api/notifications/channels` | REST | ADMIN/OPERATOR | webhook 알림 채널 생성 | M6 구현 |
| PUT | `/admin/api/notifications/channels/{id}` | REST | ADMIN/OPERATOR | webhook 알림 채널 수정 | M6 구현 |
| POST | `/admin/api/notifications/channels/{id}/test` | REST | ADMIN/OPERATOR | webhook test 발송 | M6 구현 |
| GET | `/admin/api/notifications/events` | REST | ADMIN/OPERATOR | 알림 이벤트 조회 | M6 구현 |
| PUT | `/admin/api/notifications/events/{id}/review` | REST | ADMIN/OPERATOR | 알림 이벤트 확인 처리 | M6 구현 |

## 후속 API 백로그

- `POST /chat/api/sessions/{id}/end`: 명시적 사용자 종료 API는 후속으로 둔다.
- 외부 알림 adapter API: webhook/Slack/email 기본형은 구현 완료, provider별 상세 설정은 후속으로 둔다.
- Elasticsearch/벡터 검색 API: PostgreSQL FTS 품질 한계 확인 후 추가한다.

## 공통 보안

- 상태 변경 요청은 CSRF 토큰을 요구한다.
- 관리자 URL은 서버 측 인가를 우선 적용하고, 메뉴 노출 제어는 보조 수단으로만 사용한다.
- REST API 실패 응답은 SQL, stack trace, 민감 원문을 포함하지 않는다.
- 세션성 식별 쿠키는 `SameSite=Lax`, `Secure=true`, `HttpOnly=true`를 기본값으로 한다. CSRF 토큰 쿠키만 `HttpOnly=false` 예외를 둔다.
- 검색어 PII는 전체 차단하고 원문을 저장하지 않는다.

## M6 Notification Adapter Update

- `/admin/api/notifications/channels` now supports `WEBHOOK`, `SLACK_WEBHOOK`, and `EMAIL_SMTP` channel types.
- Notification channel requests include optional `previousEndpointEnvKey` for env-key based webhook secret rotation.
- Email SMTP channels use `endpointEnvKey` as the recipient-list env key. Provider-specific adapters remain follow-up work.

## M9 AI Answer Suggestion Scaffold

- No public AI/RAG API is exposed in the scaffold.
- `/chat/api/**` keeps the existing `ApiResponse` envelope and session response shape.
- Runtime uses an internal provider-neutral suggestion interface only; real provider and pgvector endpoints are follow-up work.

## M10 External REST API Scaffold

- `GET /external/api/v1/status` is the only external API stub.
- The stub is disabled by default with `CLEVERCHAT_EXTERNAL_API_ENABLED=false`.
- Enabled access requires a key matching `CLEVERCHAT_EXTERNAL_API_KEY_SHA256`; raw API keys are never stored or returned.
- CORS remains closed and real business API endpoints are follow-up work.
