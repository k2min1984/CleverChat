# API/엔드포인트 목록

> 작성일: 2026-05-08
> 변경요약: M1 인증 엔드포인트, M2 시나리오 API, M3 챗봇 런타임 API, M4 검색 API 설계 추가

| Method | Path | 유형 | 권한 | 설명 | 상태 |
|---|---|---|---|---|---|
| GET | `/` | View | 전체 | 홈 | 구현 |
| GET | `/login` | View | 전체 | 관리자 로그인 화면 | 구현 |
| POST | `/login` | Form | 전체 | 자체 LoginController 폼 처리 (HandlerInterceptor + 세션 VO) | 구현 |
| POST | `/logout` | Form | 인증 사용자 | 로그아웃 | 구현 |
| GET | `/admin` | View | ADMIN/OPERATOR | 관리자 대시보드 | M2 보완 |
| GET | `/admin/password-change` | View | 인증 사용자 | 최초 로그인 비밀번호 변경 화면 | M2 설계 |
| POST | `/admin/password-change` | Form | 인증 사용자 | 비밀번호 변경 처리, 성공 시 `must_change_password=false` | M2 설계 |
| GET | `/actuator/health` | REST | 전체 | 헬스체크 | 구현 |
| GET | `/chat` | View | 전체 | 챗봇 메인 | M3 설계 |
| GET | `/chat/{sessionId}` | View | 본인 | 진행 중 챗봇 세션 | M3 설계 |
| GET | `/chat/history` | View | 본인 | 대화 이력 | M3 설계 |
| GET | `/chat/error` | View | 전체 | 챗봇 오류/만료 안내 | M3 설계 |
| * | `/chat/api/**` | REST | 전체/본인 | 사용자 챗봇 런타임 API | M3 설계 |
| GET | `/chat/api/scenarios` | REST | 전체 | 활성 시나리오 목록 | M3 설계 |
| POST | `/chat/api/sessions` | REST | 전체 | 새 챗봇 세션 시작 | M3 설계 |
| GET | `/chat/api/sessions/{id}` | REST | 본인 | 챗봇 세션 현재 상태 | M3 설계 |
| POST | `/chat/api/sessions/{id}/select-option` | REST | 본인 | 현재 노드 옵션 선택 진행 | M3 설계 |
| POST | `/chat/api/sessions/{id}/free-text` | REST | 본인 | 자유 텍스트 매칭/fallback | M3 설계 |
| GET | `/chat/api/sessions/{id}/history` | REST | 본인 | 세션 메시지 이력 | M3 설계 |
| GET | `/chat/api/history` | REST | 본인 | anonymous_id 기준 90일 이력 목록 | M3 설계 |
| POST | `/chat/api/sessions/{id}/end` | REST | 본인 | 사용자 세션 종료 | M3 설계 |
| POST | `/chat/api/messages/{messageId}/feedback` | REST | 본인 | BOT 메시지 만족도 등록 | M3 설계 |
| GET | `/chat/api/recommendations` | REST | 전체 | 운영자 고정 추천 질문 목록 | M3 설계 |
| GET | `/admin/chat/sessions` | View | ADMIN/OPERATOR | 챗봇 세션 목록 | M3 설계 |
| GET | `/admin/chat/sessions/{id}` | View | ADMIN/OPERATOR | 챗봇 세션 상세 | M3 설계 |
| GET | `/admin/chat/failures` | View | ADMIN/OPERATOR | 답변 실패 큐 | M3 설계 |
| GET | `/admin/chat/feedback` | View | ADMIN/OPERATOR | 챗봇 피드백 목록 | M3 설계 |
| GET | `/admin/chat/recommendations` | View | ADMIN/OPERATOR | 챗봇 추천 질문 관리 | M3 설계 |
| * | `/admin/api/chat/**` | REST | ADMIN/OPERATOR | 챗봇 운영 API | M3 설계 |
| GET | `/admin/api/chat/sessions` | REST | ADMIN/OPERATOR | 챗봇 세션 검색 | M3 설계 |
| GET | `/admin/api/chat/sessions/{id}` | REST | ADMIN/OPERATOR | 챗봇 세션 상세 + 메시지 트레이스 | M3 설계 |
| GET | `/admin/api/chat/failures` | REST | ADMIN/OPERATOR | 답변 실패 큐 검색 | M3 설계 |
| POST | `/admin/api/chat/failures/{id}/review` | REST | ADMIN/OPERATOR | 실패 큐 검토 처리 | M3 설계 |
| GET | `/admin/api/chat/feedback` | REST | ADMIN/OPERATOR | 챗봇 피드백 검색 | M3 설계 |
| * | `/admin/api/chat/recommendations/**` | REST | ADMIN/OPERATOR | 추천 질문 CRUD | M3 설계 |
| GET | `/admin/search/logs` | View | ADMIN/OPERATOR | 검색 로그 조회 | M4 설계 |
| GET | `/admin/search/blocks` | View | ADMIN/OPERATOR | PII 차단 검색 조회 | M4 설계 |
| GET | `/admin/search/popular` | View | ADMIN/OPERATOR | 인기 검색어 조회 | M4 설계 |
| * | `/admin/api/search/**` | REST | ADMIN/OPERATOR | 검색 운영 API | M4 설계 |
| GET | `/admin/api/search/logs` | REST | ADMIN/OPERATOR | 검색 로그 조회 | M4 설계 |
| GET | `/admin/api/search/blocks` | REST | ADMIN/OPERATOR | PII 차단 검색 로그 조회 | M4 설계 |
| GET | `/admin/api/search/popular` | REST | ADMIN/OPERATOR | 인기 검색어 조회 | M4 설계 |
| POST | `/admin/api/search/test` | REST | ADMIN/OPERATOR | 관리자 검색 테스트 | M4 설계 |
| POST | `/admin/api/search/popular/rebuild` | REST | ADMIN/OPERATOR | 인기 검색어 재집계 | M4 설계 |
| DELETE | `/admin/api/search/logs/expired` | REST | ADMIN/OPERATOR | 보존 기간 초과 검색 로그 파기 | M4 설계 |
| GET | `/admin/scenarios` | View | ADMIN/OPERATOR | 시나리오 목록 화면 | M2 설계 |
| GET | `/admin/scenarios/new` | View | ADMIN/OPERATOR | 시나리오 등록 화면 | M2 설계 |
| GET | `/admin/scenarios/{id}` | View | ADMIN/OPERATOR | 시나리오 편집 화면 | M2 설계 |
| GET | `/admin/scenarios/{id}/preview` | View | ADMIN/OPERATOR | 시나리오 미리보기 | M2 설계 |
| * | `/admin/api/scenarios/**` | REST | ADMIN/OPERATOR | 시나리오 관리 API | M2 설계 |
| * | `/admin/api/categories/**` | REST | ADMIN/OPERATOR | 시나리오 카테고리 API | M2 설계 |

## 상세 문서

- `2.설계/03.API설계서/공통응답에러코드.md`
- `2.설계/03.API설계서/M2_시나리오API.md`
- `2.설계/03.API설계서/M3_챗봇런타임API.md`
- `2.설계/03.API설계서/M4_검색API.md`

## 공통 보안

- 상태 변경 요청은 CSRF 토큰을 요구한다.
- 관리자 URL은 서버 측 인가를 우선 적용하고, 화면 메뉴 노출 제어는 보조 수단으로 사용한다.
- REST API 추가 시 실패 응답은 민감정보를 포함하지 않는다.
- 세션성 식별 쿠키는 `SameSite=Lax`, `Secure=true`, `HttpOnly=true`를 기본값으로 한다. CSRF 토큰 쿠키만 `HttpOnly=false` 예외다.
- 프록시 헤더 처리는 `server.forward-headers-strategy=framework`를 기준으로 하며, 부팅 경고 로그는 prod 프로파일에서만 출력한다.
- M3 키워드/유사어 매칭 캐시 TTL 기본값은 300초로 둔다.
- M4 검색어는 1~200자로 제한하고, PII 차단 시 원문을 검색 로그에 저장하지 않는다.
