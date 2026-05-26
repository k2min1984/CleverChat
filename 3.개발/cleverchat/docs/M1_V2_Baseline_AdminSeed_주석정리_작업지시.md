# M1 V2 Baseline 관리자 시딩 주석 정리 작업지시

> 작성일: 2026-05-21  
> 대상 프로젝트: `3.개발/cleverchat`  
> 목적: `V2__auth_session_baseline.sql`에 남은 환경변수 기반 초기 관리자 시딩 안내 주석을 Flyway V4_1 전환 결정과 정합화한다.

## 1. 배경

`Initial_Admin_Seeding_정책검수.md`는 `V2__auth_session_baseline.sql` 31~32행의 `CLEVERCHAT_ADMIN_SEED_*` 기반 안내 주석을 후속 마이그레이션 또는 문서 메모로 정리하라고 권고했다. 현재 초기 관리자 생성 기준은 `V4_1__seed_default_admin.sql`과 `spring.flyway.placeholders.default-admin-seed-enabled` placeholder 토글이다.

이미 적용된 Flyway baseline 파일 본문을 직접 수정하면 checksum 불일치가 발생할 수 있으므로, 운영 DB에 적용된 이력이 있는 환경에서는 V2 파일 직접 수정으로 처리하지 않는다.

현재 상태: `2.설계/06.운영메모/M1_운영메모.md`에는 V2 잔존 주석이 과거 정책이며 현행 기준은 V4_1이라는 정책 메모가 이미 들어 있다. 따라서 본 작업은 `1.기획/결정사항.md` §6 "M1 인증 기반 추가 결정 - 초기 관리자 시딩" 결정의 문서 정합 후속 조치로 보고, 운영메모는 신규 추가가 아니라 보강/확정 대상으로 본다.

## 2. 결정 기준

| 항목 | 기준 |
|---|---|
| 현행 시딩 방식 | Flyway `V4_1__seed_default_admin.sql` |
| dev/stage | `default-admin-seed-enabled=true` |
| prod | `application-prod.yml`에 `default-admin-seed-enabled=false` 명시, 기본 관리자 자동 생성 차단 |
| V2 본문 수정 | 운영/공유 DB 적용 이력이 있으면 금지 |
| 정리 방식 | 후속 마이그레이션 메모 또는 운영 문서 메모로 변경 이력 보존 |

V2 적용 이력 확인 기준:

```sql
SELECT installed_rank, version, description, success, checksum, installed_on
FROM flyway_schema_history
WHERE version = '2';
```

또는 대상 환경에서 `./mvnw flyway:info`를 실행해 `V2__auth_session_baseline.sql` 적용 여부와 checksum 상태를 확인한다. 운영/공유 DB에 version `2` 이력이 있으면 V2 본문 직접 수정은 금지한다.

## 3. 작업 옵션

### 옵션 A. 후속 마이그레이션 메모

- 신규 Flyway 파일을 추가해 관리자 시딩 기준 변경 이력을 SQL 주석으로 남긴다.
- 기존 V2 파일은 수정하지 않는다.
- 산출물에는 `2.설계/06.운영메모/M1_운영메모.md`의 초기 관리자 생성 절에 V4_2 참조 또는 후속 마이그레이션 기록을 함께 반영하는 작업을 포함한다.
- 예: `V4_2__document_admin_seed_policy_change.sql`

```sql
-- Admin seed policy note.
-- V2 baseline comments referenced CLEVERCHAT_ADMIN_SEED_* application boot seeding.
-- The active policy is V4_1 Flyway placeholder-based seeding:
-- spring.flyway.placeholders.default-admin-seed-enabled=true for dev/stage,
-- false by default for prod.
DO $$ BEGIN NULL; END $$;
```

주석만 있는 no-op SQL이 CI lint 또는 사내 검토 도구에서 경고를 일으키는 경우를 피하기 위해 무해한 실행문을 함께 둔다.

### 옵션 B. 운영 문서 메모

- `2.설계/06.운영메모/M1_운영메모.md` 또는 배포 README에 있는 "V2 잔존 주석은 과거 정책이며 현행 기준은 V4_1" 메모를 보강/확정한다.
- Flyway 파일 추가 없이 문서 추적만 강화한다.

## 4. 권장안

공유 개발 DB 또는 stage DB에 V2가 이미 적용된 이력이 있으면 옵션 A를 우선한다. 신규 마이그레이션 번호는 현재 최신 Flyway 번호를 확인한 뒤 충돌 없이 배정한다. V2 적용 이력이 전혀 없는 단독 로컬 환경에서 단순 문서 정합성만 필요하고 DB 변경 파일 추가를 피하려는 경우에만 옵션 B 단독 처리를 허용한다.

## 5. 검증

- `rg "CLEVERCHAT_ADMIN_SEED|default-admin-seed-enabled|V4_1__seed_default_admin" 1.기획 2.설계 3.개발 5.배포 9.참고`
- 대상 환경의 `flyway_schema_history`에서 `version='2'` 행 존재 여부와 `success=true` 상태를 확인한다.
- 옵션 A 채택 시 dev 프로파일로 부팅해 신규 V4_2가 `flyway_schema_history`에 `SUCCESS`로 기록되는지 확인한다.
- `./mvnw -q -DskipTests package` 또는 Flyway 적용 검증은 실제 후속 마이그레이션을 추가하는 작업에서 수행한다.

## 6. 제외 범위

- 본 작업지시는 `V2__auth_session_baseline.sql` 직접 수정을 요구하지 않는다.
- 관리자 비밀번호 변경 화면, 운영용 관리자 생성 절차, 비밀번호 로테이션 정책은 별도 작업으로 분리한다.
- `application-prod.yml`의 `default-admin-seed-enabled: false` 명시 가드는 현재 워킹트리에 반영된 상태이며, 본 작업의 신규 변경 범위가 아니다.
- `1.기획/결정사항.md` §6의 해시/평문 커밋 trade-off 보강은 반영된 상태로 유지한다.
- 운영 배포 체크리스트의 `admin/admin` 점검 절차와 Flyway placeholder effective config 자동 검증은 별도 작업지시로 분리한다.
