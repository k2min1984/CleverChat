# PITR 복구 절차

## 1. 사전 조건

- PostgreSQL 버전과 확장(`pg_trgm`, `unaccent`, `pgcrypto` 등)이 운영과 동일해야 한다.
- WAL archiving이 운영 DB에서 활성화되어 있어야 한다.
- `archive_command`, `restore_command`, WAL 보관 위치, 백업 파일 위치를 복구 리허설 전에 확인한다.
- CleverChat 필드 암호화 키 환경변수를 DB 복구 시점의 키와 일치시킨다.

## 2. 전체 복구

1. 복구 대상 PostgreSQL 인스턴스를 준비한다.
2. 백업 파일 checksum을 확인한다.
3. `pg_restore --list <backup.dump>`로 백업 카탈로그를 확인한다.
4. 신규 빈 DB를 생성한다.
5. `pg_restore --clean --if-exists --no-owner --no-acl -d <database> <backup.dump>`를 실행한다.
6. 애플리케이션을 `spring.flyway.validate-on-migrate=true` 기준으로 기동해 Flyway 상태를 검증한다.
7. `/actuator/health`, `/admin/login`, `/chat` smoke를 수행한다.

## 3. 시점 복구(PITR)

1. base backup과 WAL archive 범위를 확인한다.
2. 복구 대상 시각을 KST와 UTC로 모두 기록한다.
3. PostgreSQL recovery 설정에 `recovery_target_time`을 지정한다.
4. WAL replay 완료 후 읽기 전용 확인 쿼리를 수행한다.
5. 무결성 확인 후 운영 전환 여부를 결정한다.

## 4. 사후 확인

- `flyway_schema_history` 최신 성공 row 확인
- 관리자 로그인 1회 성공
- `/chat` 세션 시작 1회 성공
- 시나리오 목록/검색/크롤 대상 목록 조회
- 암호화된 chat message/history 복호화 확인

## 5. 주의사항

- 복구 리허설은 운영 DB에 직접 수행하지 않는다.
- 복구 실패 로그에 DB 비밀번호, 암호화 키, 사용자 입력 원문을 남기지 않는다.
- Flyway 실패가 발생하면 임의로 migration 파일을 수정하지 않고, 백업 시점과 실패 migration 번호를 기록한 뒤 롤백 판단을 한다.
