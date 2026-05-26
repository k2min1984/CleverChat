# DBA pgcrypto 권한 확인 요청문

> 작성일: 2026-05-26  
> 대상 프로젝트: `3.개발/cleverchat`  
> 목적: PostgreSQL/Flyway 마이그레이션에서 `pgcrypto` 확장 및 `gen_random_uuid()` 사용 가능 여부를 배포 전 확인한다.

## 0. 절대 원칙

- 본 요청은 DBA 사전 확인용 문서이며, 애플리케이션 코드와 Flyway 마이그레이션 파일은 수정하지 않는다.
- 이미 배포되었거나 공유 DB에 적용 가능성이 있는 baseline 마이그레이션은 직접 수정하지 않는다.
- 권한 확인은 dev, stage, prod 환경을 모두 대상으로 한다.
- 확인 결과 `pgcrypto` 사용이 불가한 환경이 있으면 `gen_random_uuid()` 의존을 제거하거나 애플리케이션에서 UUID를 생성하는 대안을 검토한다.

## 1. 배경

M3 챗봇 런타임 baseline 마이그레이션인 `3.개발/cleverchat/src/main/resources/db/migration/V4__chat_runtime_baseline.sql`은 파일 첫 줄에서 `pgcrypto` 확장을 생성하고, `chat_session.id` 기본값으로 `gen_random_uuid()`를 사용한다.

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE chat_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ...
);
```

PostgreSQL에서 `CREATE EXTENSION` 실행 가능 여부는 DB 권한, 설치된 extension control file, DB별 확장 활성화 상태, 운영 정책에 따라 달라질 수 있다. Flyway가 애플리케이션 계정으로 마이그레이션을 수행하는 경우 해당 계정이 `CREATE EXTENSION IF NOT EXISTS pgcrypto;`를 실행할 수 없으면 배포 초기에 실패할 수 있다.

기존 V1 마이그레이션에서 `pg_trgm` 확장을 사용한 선례가 있으므로, 동일한 기준으로 `pgcrypto`도 각 환경에서 사전 확인한다.

## 2. DBA 확인 요청 항목

| 번호 | 확인 항목 | 요청 내용 | 기대 결과 |
|---|---|---|---|
| 1 | 확장 설치 가능 여부 | 각 DB 서버에 `pgcrypto` extension control file이 설치되어 있는지 확인 | `pg_available_extensions`에서 `pgcrypto` 조회 가능 |
| 2 | 확장 활성화 상태 | 대상 DB에 `pgcrypto`가 이미 생성되어 있는지 확인 | `pg_extension`에서 `pgcrypto` 존재 여부 확인 |
| 3 | 애플리케이션 계정 권한 | Flyway 실행 계정이 `CREATE EXTENSION IF NOT EXISTS pgcrypto;`를 실행할 수 있는지 확인 | 마이그레이션 계정으로 성공 가능 또는 DBA 사전 생성 필요 |
| 4 | DBA 사전 설치 가능성 | 애플리케이션 계정에 확장 생성 권한을 주지 않는 정책인 경우 DBA가 사전에 생성 가능한지 확인 | 배포 전 DBA 작업 절차 확정 |
| 5 | 함수 호출 가능 여부 | 대상 스키마/계정에서 `gen_random_uuid()` 호출이 가능한지 확인 | `SELECT gen_random_uuid();` 성공 |
| 6 | PostgreSQL 버전 | 각 환경의 PostgreSQL 버전과 `gen_random_uuid()` 제공 범위를 확인 | 운영 버전에서 사용 가능 확인 |
| 7 | 변경 절차 | prod에서 extension 생성이 변경관리/승인 대상인지 확인 | 배포 전 승인, 작업자, 일정 확정 |

## 3. 필요한 권한

| 구분 | 필요 권한 또는 상태 | 비고 |
|---|---|---|
| 확장 파일 | 서버에 `pgcrypto` extension 설치 파일 존재 | 일반적으로 PostgreSQL contrib 패키지 또는 배포판 기본 패키지에 포함 |
| 확장 생성 | 대상 DB에서 `CREATE EXTENSION pgcrypto` 실행 가능 | 운영 정책상 DBA 계정으로만 허용될 수 있음 |
| 마이그레이션 실행 | Flyway 실행 계정이 V4 SQL을 실행할 수 있음 | 확장 생성 권한이 없으면 V4 첫 줄에서 실패 가능 |
| 함수 사용 | 애플리케이션/Flyway 계정에서 `gen_random_uuid()` 호출 가능 | 기본값 평가 시 필요 |
| 스키마 영향 | `pgcrypto`가 대상 DB에 1회 생성되어 있음 | 확장은 DB 단위 객체이며 스키마별 테이블 생성과 별도 |

## 4. 적용 환경

| 환경 | 확인 대상 | 요청 사항 |
|---|---|---|
| dev | 개발 DB | Flyway 실행 계정으로 `CREATE EXTENSION IF NOT EXISTS pgcrypto;` 및 `SELECT gen_random_uuid();` 가능 여부 확인 |
| stage | 스테이징 DB | 운영과 동일한 권한 정책 기준으로 사전 생성 필요 여부 확인 |
| prod | 운영 DB | DBA 사전 생성 필요 여부, 변경 승인 절차, 배포 전 작업 가능 일정을 확정 |

접속 정보와 실제 계정명은 환경별 보안 정책에 따라 DBA/인프라 관리 채널의 최신 정보를 기준으로 확인한다. 이 문서에는 접속 문자열, 비밀번호, 토큰 등 민감정보를 기록하지 않는다.

## 5. DBA 확인용 SQL 예시

아래 SQL은 DBA가 각 환경의 정책에 맞게 조정해 실행한다.

```sql
-- PostgreSQL 버전 확인
SELECT version();

-- pgcrypto 설치 가능 여부 확인
SELECT name, default_version, installed_version
FROM pg_available_extensions
WHERE name = 'pgcrypto';

-- 현재 DB에 pgcrypto가 생성되어 있는지 확인
SELECT extname, extversion
FROM pg_extension
WHERE extname = 'pgcrypto';

-- 확장 생성 가능 여부 확인
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 함수 호출 가능 여부 확인
SELECT gen_random_uuid();
```

애플리케이션/Flyway 계정 권한 확인이 필요한 경우, 가능하면 실제 마이그레이션 실행 계정과 동일한 권한으로 위 SQL을 검증한다.

## 6. 실패 시 대안

| 대안 | 내용 | 장점 | 주의 사항 |
|---|---|---|---|
| A. 애플리케이션 UUID 생성 권장 | DB 기본값 `gen_random_uuid()`에 의존하지 않고 애플리케이션에서 UUID를 생성해 INSERT | DB extension 권한 의존 제거, 환경별 권한 차이 완화 | 엔티티/INSERT 경로에서 UUID 생성 책임을 명확히 해야 함 |
| B. DBA 사전 설치 | DBA가 dev/stage/prod 대상 DB에 `CREATE EXTENSION pgcrypto;`를 배포 전 선반영 | 기존 V4 SQL 변경 없이 진행 가능 | prod 변경 승인과 작업 일정 필요 |
| C. V4 미배포 환경 한정 사전 정정 | 아직 어떤 공유 DB에도 V4가 적용되지 않은 것이 확실한 경우에만 마이그레이션 내용을 사전 정정 | Flyway 이력 오염 전 정책 반영 가능 | 이미 적용된 환경이 있으면 checksum mismatch 위험으로 금지 |

권장 순서는 A 또는 B를 우선 검토한다. C는 V4가 어떤 공동 dev/stage/prod DB에도 적용되지 않았다는 근거가 있을 때만 제한적으로 검토한다.

## 7. DBA 발송용 본문 초안

안녕하세요. CLEVERCHAT PostgreSQL 마이그레이션 관련하여 `pgcrypto` 확장 사용 가능 여부와 권한 확인을 요청드립니다.

M3 챗봇 런타임 baseline 마이그레이션 `V4__chat_runtime_baseline.sql`에서 아래 구문을 사용합니다.

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
...
id UUID PRIMARY KEY DEFAULT gen_random_uuid()
```

dev/stage/prod 각 환경에 대해 다음 사항 확인 부탁드립니다.

| 확인 항목 | 요청 내용 |
|---|---|
| `pgcrypto` 설치 가능 여부 | `pg_available_extensions`에서 `pgcrypto` 조회 가능 여부 |
| 현재 활성화 상태 | 대상 DB의 `pg_extension`에 `pgcrypto` 존재 여부 |
| Flyway 계정 권한 | 애플리케이션/Flyway 마이그레이션 계정으로 `CREATE EXTENSION IF NOT EXISTS pgcrypto;` 실행 가능 여부 |
| DBA 사전 생성 필요 여부 | 운영 정책상 애플리케이션 계정에 extension 생성 권한을 부여하지 않는 경우, DBA 사전 생성 가능 여부 |
| 함수 호출 가능 여부 | 대상 계정으로 `SELECT gen_random_uuid();` 성공 여부 |
| PostgreSQL 버전 | 각 환경의 PostgreSQL 버전과 `gen_random_uuid()` 사용 가능 여부 |
| prod 변경 절차 | 운영 DB extension 생성이 변경 승인 대상인지와 필요한 일정/절차 |

확장 생성 권한 부여가 어려운 경우, DBA가 배포 전에 `pgcrypto`를 사전 생성하는 방식이 가능한지도 함께 확인 부탁드립니다.

만약 `pgcrypto` 또는 `gen_random_uuid()` 사용이 불가한 환경이 있으면, DB 기본값을 사용하지 않고 애플리케이션에서 UUID를 생성하는 대안을 검토하겠습니다.

감사합니다.

## 8. 검증 및 실패 대응

- dev에서 Flyway dry-run 또는 실제 migrate 전 `CREATE EXTENSION IF NOT EXISTS pgcrypto;` 실행 가능 여부를 확인한다.
- stage는 prod와 동일한 권한 정책으로 검증해 prod 배포 전 권한 차이를 제거한다.
- prod는 DBA 사전 생성 또는 권한 승인 완료 후 배포한다.
- 실패 로그에 `permission denied to create extension`, `extension "pgcrypto" is not available`, `function gen_random_uuid() does not exist`가 포함되면 배포를 중단하고 대안 A/B/C 중 하나를 선택한다.

## 9. 제외 범위

- 이 문서는 코드, SQL 마이그레이션, 설정 파일을 변경하지 않는다.
- 운영 DB 접속 정보, 계정명, 비밀번호, 토큰 등 민감정보를 포함하지 않는다.
- UUID 생성 방식 변경 구현은 별도 작업으로 분리한다.
