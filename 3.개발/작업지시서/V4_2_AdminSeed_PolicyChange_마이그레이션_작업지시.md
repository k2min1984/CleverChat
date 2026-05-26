# V4_2 Admin Seed Policy Change 마이그레이션 작업지시

> 작성일: 2026-05-26  
> 대상 프로젝트: `3.개발/cleverchat`  
> 목적: V2 잔존 주석을 직접 수정하지 않고, 현재 V4_1 기본 관리자 시딩 정책을 `V4_2__document_admin_seed_policy_change.sql`로 Flyway 이력에 명문화한다.

## 0. 절대 원칙

- `V2__auth_session_baseline.sql` 본문은 수정하지 않는다.
- V2 잔존 주석은 과거 정책의 이력 보존 텍스트로 취급한다.
- V4_2는 스키마/데이터 변경 목적이 아니라 정책 명문화 전용 no-op 마이그레이션이다.
- 본 작업의 실제 산출물은 신규 SQL 1건과 운영메모 보강 1건으로 제한한다.
- 대규모 리팩토링, 계정 정책 변경, 비밀번호 변경 기능 구현, 배포 스크립트 수정은 제외한다.

## 1. 배경

`3.개발/cleverchat/docs/M1_V2_Baseline_AdminSeed_주석정리_작업지시.md`의 §3 "옵션 A. 후속 마이그레이션 메모"를 채택한다.

현재 `V2__auth_session_baseline.sql`에는 삭제된 `InitialAdminSeeder`와 `CLEVERCHAT_ADMIN_SEED_USERNAME` / `CLEVERCHAT_ADMIN_SEED_PASSWORD` 기반 시딩을 가리키는 과거 주석이 남아 있다. 그러나 V2는 baseline 성격의 적용 이력이 있는 Flyway 파일이므로 직접 수정하면 운영/공유 DB에서 checksum mismatch가 발생할 수 있다.

현행 초기 관리자 생성 정책은 `V4_1__seed_default_admin.sql`과 `spring.flyway.placeholders.default-admin-seed-enabled` placeholder로 관리한다.

| 환경 | 기준 |
|---|---|
| dev | `default-admin-seed-enabled=true`로 `admin/admin` 생성 또는 갱신 |
| stage | `default-admin-seed-enabled=true`로 `admin/admin` 생성 또는 갱신 |
| prod | `application-prod.yml`에서 `default-admin-seed-enabled=false`로 기본 관리자 자동 생성 차단 |

따라서 V2 파일은 그대로 두고, V4_2 신규 마이그레이션에 정책 전환 사실을 주석으로 기록해 Flyway 이력과 운영 문서의 정합성을 맞춘다.

## 2. 결정 기준

| 항목 | 기준 |
|---|---|
| 신규 파일명 | `V4_2__document_admin_seed_policy_change.sql` |
| 신규 파일 위치 | `3.개발/cleverchat/src/main/resources/db/migration/` |
| 마이그레이션 성격 | SQL 주석 + 무해한 no-op 실행문 |
| placeholder 사용 | 사용하지 않는다 |
| V2 직접 수정 | 금지 |
| 운영메모 동기화 | `2.설계/06.운영메모/M1_운영메모.md`의 "초기 관리자 생성" 절에 V4_2 참조를 추가 |
| 인코딩 | 기존 파일과 동일하게 UTF-8 유지 |

## 3. 산출물

### 3.1 V4_2 SQL 신규 작성

파일:

```text
3.개발/cleverchat/src/main/resources/db/migration/V4_2__document_admin_seed_policy_change.sql
```

본문 기준안:

```sql
-- Admin seed policy change note.
-- 작성일: 2026-05-26
-- Decision source: 1.기획/결정사항.md §6, 3.개발/cleverchat/docs/M1_V2_Baseline_AdminSeed_주석정리_작업지시.md
--
-- Historical reference:
-- V2__auth_session_baseline.sql lines 31-32 reference the legacy environment-variable
-- based admin seeding (CLEVERCHAT_ADMIN_SEED_USERNAME / CLEVERCHAT_ADMIN_SEED_PASSWORD)
-- via the deleted InitialAdminSeeder. Those comments are retained for history only
-- because V2 is an applied baseline and direct edits would cause Flyway checksum mismatch.
--
-- Current policy (effective):
-- 1) Default admin account (admin / admin) is created by V4_1__seed_default_admin.sql.
-- 2) Seeding is gated by the Flyway placeholder
--    spring.flyway.placeholders.default-admin-seed-enabled.
-- 3) dev and stage profiles set the placeholder to true.
-- 4) prod profile sets the placeholder to false in application-prod.yml,
--    blocking automatic creation of the default admin account in production.
-- 5) V4_1 uses ON CONFLICT (username) DO UPDATE on the admin row, so re-running
--    seeding in dev/stage resets password hash, display name, enabled flag,
--    failed_attempts, locked_until, and must_change_password to seed defaults.
--
-- This migration intentionally performs no schema or data change. It exists to
-- record the policy transition in Flyway history alongside V4_1.

DO $$ BEGIN NULL; END $$;
```

작성 기준:

- SQL 파일은 실제 DB 상태를 바꾸지 않아야 한다.
- 주석만 있는 SQL 파일이 도구에서 경고를 만들 수 있으므로 `DO $$ BEGIN NULL; END $$;`를 유지한다.
- V4_1의 실제 동작과 다른 내용을 추가하지 않는다.
- 운영 계정 생성 절차나 비밀번호 로테이션 정책을 새로 정의하지 않는다.

### 3.2 운영메모 후속 동기화

파일:

```text
2.설계/06.운영메모/M1_운영메모.md
```

"초기 관리자 생성" 절의 기존 정책 메모 아래 또는 같은 절 안에 다음 인용문을 추가한다.

```markdown
> V4_2 보강: V2 잔존 주석 정합 보강은 `3.개발/cleverchat/src/main/resources/db/migration/V4_2__document_admin_seed_policy_change.sql`로 명문화한다. V4_2는 스키마/데이터를 바꾸지 않는 정책 명문화 전용 마이그레이션이며, V2 본문은 그대로 둔다.
```

작성 기준:

- 기존 V4_1 정책 설명은 유지한다.
- V2 잔존 주석이 과거 정책이라는 설명을 삭제하지 않는다.
- V4_2 링크는 운영자가 실제 마이그레이션 파일 위치를 바로 확인할 수 있는 경로로 쓴다.

## 4. 작업 순서

1. 현재 migration 디렉터리에서 `V4_2__document_admin_seed_policy_change.sql`이 없는지 확인한다.
2. 최신 Flyway 번호가 V4_1인지 확인하고, V4_2 번호 충돌이 없을 때만 신규 파일을 만든다.
3. §3.1 기준안으로 V4_2 SQL 파일을 작성한다.
4. `M1_운영메모.md`의 "초기 관리자 생성" 절에 §3.2 문구를 추가한다.
5. `V2__auth_session_baseline.sql`이 변경되지 않았는지 확인한다.
6. `git diff`로 변경 범위가 SQL 1건 + 운영메모 1건인지 확인한다.

## 5. 검증

필수 확인:

```bash
rg --files 3.개발/cleverchat/src/main/resources/db/migration | rg 'V4_2__document_admin_seed_policy_change\.sql$'
rg -n "Admin seed policy change note|default-admin-seed-enabled|V4_1__seed_default_admin|checksum mismatch" 3.개발/cleverchat/src/main/resources/db/migration/V4_2__document_admin_seed_policy_change.sql
rg -n "V4_2 보강|V4_2__document_admin_seed_policy_change\.sql" 2.설계/06.운영메모/M1_운영메모.md
git diff -- 3.개발/cleverchat/src/main/resources/db/migration/V4_2__document_admin_seed_policy_change.sql 2.설계/06.운영메모/M1_운영메모.md 3.개발/cleverchat/src/main/resources/db/migration/V2__auth_session_baseline.sql
```

선택 확인:

```bash
cd 3.개발/cleverchat
./mvnw -q -DskipTests package
```

DB no-op 적용 검증이 가능한 환경에서는 dev 프로파일로 부팅하거나 Flyway migrate를 실행해 `flyway_schema_history`에 V4_2가 `SUCCESS`로 기록되는지 확인한다.

```sql
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
WHERE version = '4.2';
```

## 6. 실패 대응

- V4_2 번호가 이미 사용 중이면 임의로 덮어쓰지 말고 최신 번호를 확인해 새 번호 배정을 별도 결정으로 남긴다.
- Flyway가 no-op SQL을 거부하면 주석은 유지하고 무해한 실행문만 해당 DB 방언에 맞게 조정한다.
- V2 파일에 diff가 생기면 해당 변경은 되돌리고 V4_2와 운영메모만 남긴다.
- 운영메모의 기존 정책 문구와 충돌하는 표현이 보이면 삭제보다 보강 방식으로 정리한다.

## 7. 커밋 후보

| 후보 | 메시지 초안 | 포함 범위 |
|---|---|---|
| A | `docs(m1): document V4_2 admin seed policy change` | `V4_2__document_admin_seed_policy_change.sql` 신규 추가, `M1_운영메모.md` 초기 관리자 생성 절 V4_2 참조 추가 |

## 8. 제외 범위

- `V2__auth_session_baseline.sql` 직접 수정
- `V4_1__seed_default_admin.sql` 동작 변경
- `application-prod.yml`, dev/stage profile 설정 변경
- 관리자 비밀번호 변경 화면 또는 운영 관리자 생성 UI 구현
- 운영 배포 체크리스트의 `admin/admin` 차단 자동화 구현
- Flyway repair, checksum 재계산, 운영 DB 직접 조작
