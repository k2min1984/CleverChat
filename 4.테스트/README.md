# 4.테스트

품질 보증 산출물을 보관합니다. 코드 내 단위/통합 테스트는 `3.개발/cleverchat/src/test/`에 두고, 이 디렉토리는 **계획·시나리오·결과 문서**를 다룹니다.

## 하위 구조

| 폴더 | 산출물 |
|------|--------|
| `01.테스트계획서`   | 범위, 일정, 환경, 도구, 종료 기준, 위험 |
| `02.테스트케이스`   | 설정/운영/보안 등 절차형 테스트 케이스 초안과 실행 체크리스트 |
| `02.테스트시나리오` | 기능별 테스트 케이스, 입력·기대결과·우선순위, 회귀 시나리오 |
| `03.테스트결과서`   | 실행 결과, 결함 목록, 추적, 통계, 검증 보고 |

## 분류 기준
- 단위 테스트: 코드 옆 `src/test/java`
- 통합 테스트: Testcontainers 기반, 동일 위치
- PIT(Testcontainers 통합 테스트): `@Tag("integration")` 부착 필수, 기본 `mvn test`에서 제외된다.
- 시스템·인수 테스트: 본 디렉토리에 시나리오 문서로 정리
- 설정·보안·운영 절차 테스트: `02.테스트케이스/`에 케이스 문서로 정리
- 보안 테스트(OWASP/ASVS 체크리스트): `02.테스트시나리오/security/`로 별도 분리 권장
- 접근성 테스트(WCAG): `02.테스트시나리오/accessibility/`로 별도 분리 권장

## 통합 테스트 실행 기준

- PostgreSQL 의존 통합 테스트는 H2로 대체하지 않고 Testcontainers(PostgreSQL 16)를 사용한다.
- Docker 데몬이 실행 중이어야 하며, 기본 `./mvnw test`는 `integration` 태그를 제외한다.
- 통합 테스트 포함 실행 명령은 `3.개발/cleverchat`에서 `./mvnw -Pit test`를 사용한다.
- Windows 환경에서는 `.\mvnw.cmd test`(단위)와 `.\mvnw.cmd -Pit test`(통합)로 실행하며, 통합 테스트는 Docker Desktop 기동을 사전 조건으로 한다.
- 대상 예: MyBatis 매퍼 XML, Flyway 마이그레이션, PostgreSQL 전용 SQL(`ON CONFLICT`, `ILIKE`, FTS, `jsonb`, `pg_trgm`) 검증.

## 결과 보고 양식
```
- 일자 / 환경 / 빌드버전
- 케이스 통과/실패/스킵 수
- 주요 결함과 후속조치
- 다음 회차 권고
```
