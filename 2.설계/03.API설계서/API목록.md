# API/엔드포인트 목록

> 작성일: 2026-05-08
> 변경요약: M1 인증 엔드포인트, M2 시나리오 API, M3 챗봇 런타임 API 설계 추가

| Method | Path | 유형 | 권한 | 설명 | 상태 |
|---|---|---|---|---|---|
| GET | `/` | View | 전체 | 홈 | 구현 |
| GET | `/login` | View | 전체 | 관리자 로그인 화면 | 구현 |
| POST | `/login` | Form | 전체 | Spring Security 로그인 처리 | 구현 |
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
| GET | `/admin/chat/sessions` | View | ADMIN/OPERATOR | 챗봇 세션 목록 | M3 설계 |
| GET | `/admin/chat/sessions/{id}` | View | ADMIN/OPERATOR | 챗봇 세션 상세 | M3 설계 |
| GET | `/admin/chat/failures` | View | ADMIN/OPERATOR | 답변 실패 큐 | M3 설계 |
| GET | `/admin/chat/feedback` | View | ADMIN/OPERATOR | 챗봇 피드백 목록 | M3 설계 |
| * | `/admin/api/chat/**` | REST | ADMIN/OPERATOR | 챗봇 운영 API | M3 설계 |
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

## 공통 보안

- 상태 변경 요청은 CSRF 토큰을 요구한다.
- 관리자 URL은 서버 측 인가를 우선 적용하고, 화면 메뉴 노출 제어는 보조 수단으로 사용한다.
- REST API 추가 시 실패 응답은 민감정보를 포함하지 않는다.
- 세션성 식별 쿠키는 `SameSite=Lax`, `Secure=true`, `HttpOnly=true`를 기본값으로 한다. CSRF 토큰 쿠키만 `HttpOnly=false` 예외다.
- 프록시 헤더 처리는 `server.forward-headers-strategy=framework`를 기준으로 하며, 부팅 경고 로그는 prod 프로파일에서만 출력한다.
- M3 키워드/유사어 매칭 캐시 TTL 기본값은 300초로 둔다.
