# NFR 비기능 요구사항

> 작성일: 2026-05-08

| 분류 | 기준 | M2 적용 |
|---|---|---|
| 가용성 | 단일 노드 장애 시 systemd 재시작 | 서비스 파일 `Restart=on-failure` 전제 |
| 응답시간 | 관리자 목록 95% 1초 이내 | 페이징, 인덱스 필수 |
| 데이터 정합성 | 시나리오 버전/노드 저장은 단일 트랜잭션 | 서비스 계층 `@Transactional` |
| 보안 | 관리자 URL 인증/인가, CSRF, 감사로그 | M2 API 전체 적용 |
| 쿠키 보안 | `SameSite=Lax`, `Secure=true`, `HttpOnly=true` 기본 | CSRF 토큰 쿠키는 JS 접근을 위해 `HttpOnly=false` 예외 |
| 신뢰 헤더 | `server.forward-headers-strategy=framework` | prod 프로파일에서만 누락/오설정 부팅 경고 로그 |
| 감사 | 생성/수정/삭제/활성화 작업 기록 | actor, action, target, 변경 요약 |
| 접근성 | 관리자 폼 키보드 조작 가능 | label, focus, alert 메시지 |
| 관측성 | 오류 로그와 감사 로그 분리 | audit_log + app log |
| 운영성 | Flyway 마이그레이션으로 DB 변경 추적 | V3부터 시나리오 테이블 |
| 확장성 | 검색/크롤링/AI는 교체 가능한 경계 유지 | service interface 우선 |

## 용량 가정

| 항목 | 초기 기준 | 재검토 트리거 |
|---|---:|---|
| 시나리오 수 | 1,000건 이하 | 10,000건 초과 |
| 노드 수 | 시나리오당 200개 이하 | 시나리오당 1,000개 초과 |
| 관리자 동시 사용자 | 20명 이하 | 100명 초과 |
| 사용자 챗봇 동시 세션 | 100명 이하 | 500명 초과 |
| 검색 대상 문서 | 100만건 미만 | 100만건 이상 또는 품질 이슈 |

## 성능 기준

- 모든 목록 조회는 `page`, `size`를 사용한다.
- 관리자 검색 조건은 인덱스 사용 가능 컬럼부터 적용한다.
- 자유 텍스트 검색은 M4에서 PostgreSQL FTS + `pg_trgm` 기준으로 구현한다.
- M3 키워드/유사어 매칭 캐시는 Caffeine 기본 TTL 300초로 시작한다.
- 슬로우 쿼리 기준은 운영 1초 이상으로 시작하고 운영 데이터 기준으로 조정한다.

## 보안 기준

- SQL 작성 시 MyBatis `${}` 사용을 금지하고 `#{}` 바인딩을 사용한다.
- 사용자 입력은 Controller DTO Bean Validation으로 1차 검증하고 Service에서 업무 규칙을 검증한다.
- HTML 출력은 Thymeleaf 기본 이스케이프를 사용하고, raw HTML 렌더링은 보안 검토 후 제한적으로 허용한다.
- 운영 시 HTTPS는 nginx에서 종단하고 애플리케이션은 `server.forward-headers-strategy=framework`로 `X-Forwarded-*` 헤더를 처리한다.
- `ForwardedHeadersBootCheck`와 같은 부팅 점검 경고는 `prod` 프로파일에서만 출력해 dev/stage 로그 잡음을 줄인다.
