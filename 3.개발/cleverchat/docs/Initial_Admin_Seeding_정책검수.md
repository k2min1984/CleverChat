# Initial Admin Seeding 정책 검수

- 검수일: 2026-05-21
- 범위: `InitialAdminSeeder.java` 삭제 및 Flyway 기반 기본 관리자 시딩 전환 변경분
- 원칙: 코드 수정 없이 문서/분석만 수행

## 1. 검수 요약

초기 관리자 시딩은 애플리케이션 부팅 시점의 Java 시더에서 Flyway 마이그레이션 기반으로 전환되어 있다. `InitialAdminSeeder.java`는 삭제 상태이며, 대체 마이그레이션 `V4_1__seed_default_admin.sql`이 추가되어 `admin/admin` 기본 계정을 생성한다.

운영 프로파일에서는 기본 설정의 `spring.flyway.placeholders.default-admin-seed-enabled: false`를 상속하므로 현재 구성상 기본 관리자 계정이 생성되지 않는다. 다만 `application-prod.yml`에 명시적인 `false` 선언이 없어 향후 설정 병합, 환경변수/프로파일 오적용, 운영 설정 편집 시 방어력이 약하다.

## 2. 확인 항목별 결과

### 2.1 InitialAdminSeeder 삭제 여부와 대체 Flyway 마이그레이션 존재 여부

결과: 정상

- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/service/InitialAdminSeeder.java`는 삭제 상태이다.
- `3.개발/cleverchat/src/main/resources/db/migration/V4_1__seed_default_admin.sql`이 신규 추가되어 기본 관리자 계정 생성을 담당한다.
- `3.개발/cleverchat/src/test/java/kr/co/cleverchat/domain/auth/security/DefaultAdminSeedPasswordTest.java`가 추가되어 SQL에 커밋된 BCrypt 해시가 평문 `admin`과 매치되는지 검증한다.

주의 사항:

- `V2__auth_session_baseline.sql` 31~32행에는 기존 환경변수 기반 시딩 안내 주석이 남아 있다.
- 실제 시딩 정책은 V4_1로 이전되었으므로, V2 주석은 향후 문서/마이그레이션 정리 대상이다.

### 2.2 운영 프로파일에서 admin/admin 기본 계정 비활성 가드 여부

결과: 작동하지만 약함

가드 방식:

- `V4_1__seed_default_admin.sql`은 `${default-admin-seed-enabled}` placeholder가 문자열 `true`일 때만 INSERT/UPSERT를 실행한다.
- `application.yml` 기본값은 `spring.flyway.placeholders.default-admin-seed-enabled: false`이다.
- `application-dev.yml`, `application-stage.yml`은 `true`로 설정되어 dev/stage에서만 기본 관리자 계정이 생성된다.
- `application-prod.yml`은 해당 placeholder를 명시하지 않아 base의 `false`를 상속한다.

판단:

- 현재 구성 기준으로 운영 프로파일에서 기본 관리자 계정은 생성되지 않는다.
- 하지만 운영 파일 자체에 명시적인 차단 선언이 없으므로 보안상 강한 가드라고 보기 어렵다.
- 운영 환경에서는 `application-prod.yml`에 아래 설정을 명시하는 것이 권장된다.

```yaml
spring:
  flyway:
    placeholders:
      default-admin-seed-enabled: false
```

### 2.3 application.yml/profile별 seed 설정

결과: 정합

| 파일 | 설정값 | 판단 |
|------|--------|------|
| `application.yml` | `default-admin-seed-enabled: false` | fail-safe 기본값 |
| `application-dev.yml` | `default-admin-seed-enabled: true` | 로컬/개발 검증용 활성 |
| `application-stage.yml` | `default-admin-seed-enabled: true` | 스테이지 검증용 활성 |
| `application-prod.yml` | 미설정, base `false` 상속 | 동작상 비활성이나 명시 가드 권장 |

별도 기존 설정:

- `cleverchat.operator.seed.*`는 `application.yml`에 남아 있으며 실제 사용처는 `InitialOperatorSeeder`이다.
- operator seed는 애플리케이션 부팅 시 `ApplicationRunner`로 동작하는 OPERATOR 계정 시딩 채널이고, 이번 변경의 `V4_1__seed_default_admin.sql` 기반 ADMIN Flyway 시딩과는 별개이다.

operator seed와 V4_1 admin seed 비교:

| 항목 | OPERATOR seed | V4_1 ADMIN seed |
|------|---------------|-----------------|
| 실행 시점 | 애플리케이션 부팅 후 `InitialOperatorSeeder.run()` | Flyway 마이그레이션 실행 시점 |
| 토글 | `cleverchat.operator.seed.*`, `CLEVERCHAT_OPERATOR_SEED_*` | `spring.flyway.placeholders.default-admin-seed-enabled` |
| 권한 | `OPERATOR` | `ADMIN` |
| 멱등 처리 | username/password 공백 또는 동일 username 존재 시 return | Flyway checksum 기준 1회 실행, SQL 내부 `ON CONFLICT (username) DO UPDATE` |
| 강제 초기화 | 기존 계정 존재 시 덮어쓰지 않음 | 기존 `admin` 계정의 해시/활성/잠금/실패횟수/비밀번호 변경 필요 값을 덮어씀 |

멱등성 검토:

- Flyway 관점에서는 동일 버전 마이그레이션이 checksum으로 1회만 적용되므로 반복 실행 멱등성은 확보된다.
- 다만 `ON CONFLICT DO UPDATE`는 기존 `admin` 계정을 로그인 가능한 기본 상태로 되돌릴 수 있어 데이터 보존 관점에서는 **destructive idempotent** 성격이다.
- 특히 운영 또는 장기 stage DB에서 `failed_attempts`, `locked_until`, `must_change_password`, `password_hash`가 V4_1 값으로 회귀하면 잠금 해제, 실패 횟수 초기화, 비밀번호 변경 강제 해제, 기본 비밀번호 복원이 동시에 발생할 수 있다.

M2 보안문서 교차확인:

- `2.설계/05.보안설계서/보안체크리스트.md` 9~10행은 초기 ADMIN을 Flyway DB 시딩으로, 초기 OPERATOR를 비활성 시딩과 `must_change_password=true`로 분리한다.
- `4.테스트/02.테스트케이스/M2_보안운영테스트케이스.md`의 `M2-OPS-TC-011`은 ADMIN Flyway 시딩을, `M2-OPS-TC-012`는 OPERATOR 시딩을 별도 검증 대상으로 둔다.
- 따라서 본 검수의 "ADMIN Flyway seed와 OPERATOR ApplicationRunner seed는 독립 채널" 판단은 M2 보안/운영 문서와 정합하다.

### 2.4 결정사항.md line 108 변경 근거

결과: 방향은 합리적이나 근거 보강 필요

현재 `1.기획/결정사항.md`의 M1 인증 기반 추가 결정 표에는 초기 관리자 시딩이 `Flyway DB 마이그레이션 기반 생성`으로 정리되어 있다. 사유는 로컬/개발 검증 편의를 위해 기본 관리자 `admin/admin`을 DB에 생성하고, 운영 배포 전 비밀번호 변경 또는 별도 운영 시딩 정책 재검토가 필요하다는 내용이다.

검수 의견:

- 개발/검증 편의를 위해 시딩 방식을 단순화한 결정 자체는 이해 가능하다.
- 다만 기존 정책의 핵심 근거였던 "해시/평문 커밋 금지" 원칙이 약화되었다.
- `V4_1__seed_default_admin.sql`에는 BCrypt 해시가 커밋되어 있고, 테스트에는 평문 `admin`과 해시의 매치 검증이 포함되어 있다.
- 따라서 결정사항에는 다음 trade-off를 명시하는 것이 바람직하다.
  - dev/stage 검증 편의를 위해 기본 계정과 해시를 저장소에 포함한다.
  - 운영에서는 Flyway placeholder를 명시적으로 `false`로 유지한다.
  - 운영 배포 전 admin 기본 계정 존재 여부를 점검하고, 존재 시 삭제 또는 비밀번호 즉시 변경을 롤백/배포 차단 기준으로 둔다.

참고: 사용자 요청의 "line 108"은 현재 파일 기준으로 `## 6. M1 인증 기반 추가 결정` 헤더에 해당하고, 실제 초기 관리자 시딩 결정 행은 111행이다.

### 2.5 보안 영향과 롤백 기준

결과: dev/stage 한정이면 허용 가능하나, 운영 유입 시 높은 위험

보안 영향:

- `admin/admin`은 공개적으로 추정 가능한 기본 계정이다.
- `V4_1__seed_default_admin.sql`은 `ON CONFLICT (username) DO UPDATE`로 기존 `admin` 계정의 비밀번호, 표시명, 활성 상태, 실패 횟수, 잠금 상태, 비밀번호 변경 필요 여부를 강제로 덮어쓴다.
- 따라서 dev/stage에서는 테스트 재현성이 좋아지지만, 운영에서 실행되면 기존 관리자 계정 보안 상태를 되돌리는 위험이 있다.
- 특히 운영에서 `default-admin-seed-enabled=true`가 잘못 적용되면 잠긴 계정을 해제하고 비밀번호를 기본값으로 되돌릴 수 있다.

롤백/차단 기준:

| 시나리오 | 기준 | 조치 |
|----------|------|------|
| 운영 DB에 `admin/admin` 기본 계정 생성 | 운영 배포 직후 또는 마이그레이션 후 기본 계정 로그인 가능 | 배포 차단, 계정 비활성/삭제, 비밀번호 변경, Flyway 설정 확인 |
| 운영 `admin` 계정 상태 덮어쓰기 | 비밀번호 해시, 잠금 상태, 실패 횟수, `must_change_password`가 V4_1 값으로 회귀 | DB 백업 기준 복구 또는 수동 보정, 원인 설정 제거 |
| dev/stage에서 의도 외 리셋 | 스테이지 검증 중 기존 관리자 비밀번호가 반복 초기화 | placeholder 비활성화 또는 V4_1 후속 마이그레이션으로 정책 조정 |
| 해시/평문 커밋 정책 회귀 문제 제기 | 보안 검토에서 기본 계정 정보 저장소 포함 불가 판정 | Flyway 시딩 제거 또는 one-time 운영 runbook 방식으로 전환 |

prod seed 차단 필요성:

- 현재 prod 차단은 `application.yml`의 base 기본값 `default-admin-seed-enabled: false`를 prod가 상속하는 단일 레이어에 가깝다.
- 다음과 같은 오주입/운영 실수 시나리오에서는 base 상속만으로 방어가 부족하다.
  - `SPRING_FLYWAY_PLACEHOLDERS_DEFAULT_ADMIN_SEED_ENABLED=true` 환경변수가 운영에 주입되는 경우
  - dev/stage yml 또는 샘플 설정을 prod yml로 복사하는 경우
  - 배포 CLI 인자 또는 임시 override가 회수되지 않는 경우
- defense-in-depth 관점에서 prod yml 명시 차단, 배포 파이프라인 검증, 마이그레이션 후 점검, prod 전용 마이그레이션 분리 기준을 함께 두는 것이 바람직하다.

prod 다층 방어 권장안:

| 레이어 | 권장 기준 | 차단 효과 |
|--------|-----------|-----------|
| prod yml | `application-prod.yml`에 `spring.flyway.placeholders.default-admin-seed-enabled: false` 명시 | base 상속 의존 제거 |
| 파이프라인 | prod 배포 전 effective config에서 seed placeholder가 `true`이면 실패 | 환경변수/CLI 오주입 차단 |
| post-migrate 점검 | prod 마이그레이션 직후 `admin/admin` 로그인 가능 또는 V4_1 해시 회귀 확인 시 배포 실패 | DB 반영 후 검출 |
| prod 전용 분리 | prod에서는 기본 계정 seed 마이그레이션을 제외하거나 별도 운영 runbook으로 대체 | 구조적 유입 차단 |

- 동일 원칙은 `cleverchat.operator.seed.*`에도 적용한다. 운영에서는 OPERATOR 시딩도 기본 비활성, 임시 비밀번호, `must_change_password=true`, 배포 후 계정 상태 점검을 한 묶음으로 검증해야 한다.

V2 주석 체크섬 위험:

- `V2__auth_session_baseline.sql` 31~32행에는 기존 환경변수 기반 초기 관리자 시딩 주석이 남아 있다.
- 해당 주석은 현재 V4_1 정책과 맞지 않지만, 이미 적용된 Flyway 마이그레이션 본문을 직접 수정하면 주석 변경만으로도 `Migration checksum mismatch`가 발생할 수 있다.
- stage/prod에서 checksum mismatch가 발생하면 애플리케이션 기동 또는 마이그레이션이 차단되고, `flyway repair`나 `validateOnMigrate: false` 같은 운영 개입이 필요해진다.
- 권장 기준은 V2 본문 직접 수정 금지, 후속 마이그레이션 또는 문서 메모로 정책 변경 이력을 남기는 방식이다.
- 운영 점검에는 `flyway_schema_history`의 V2 checksum 상태 확인 절차를 추가하고, checksum 불일치 발견 시 원인 파일 diff와 적용 DB 범위를 먼저 확인해야 한다.

## 3. 변경 파일 목록

### 시딩 전환 직접 관련

코드/설정:

- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/service/InitialAdminSeeder.java` 삭제
- `3.개발/cleverchat/src/main/resources/db/migration/V4_1__seed_default_admin.sql` 신규
- `3.개발/cleverchat/src/main/resources/application.yml` 수정
- `3.개발/cleverchat/src/main/resources/application-dev.yml` 수정
- `3.개발/cleverchat/src/main/resources/application-stage.yml` 수정

테스트:

- `3.개발/cleverchat/src/test/java/kr/co/cleverchat/domain/auth/security/DefaultAdminSeedPasswordTest.java` 신규

문서:

- `1.기획/결정사항.md` 수정
- `2.설계/05.보안설계서/보안체크리스트.md` 수정
- `2.설계/06.운영메모/M1_운영메모.md` 수정
- `4.테스트/02.테스트케이스/M2_보안운영테스트케이스.md` 수정
- `3.개발/cleverchat/docs/M1_Auth_AdminSeed_Flyway전환_작업지시.md` 신규
- `3.개발/cleverchat/README.md` 수정
- `5.배포/README.md` 수정
- `9.참고/개발자온보딩.md` 수정

### 검수 범위 외 변경

다음 파일은 같은 워킹트리에 있으나 초기 관리자 시딩 전환과 직접 관련이 낮으므로 별도 커밋 분리를 권장한다.

- `.gitignore`
- `2.설계/02.DB설계서/ERD초안.md`
- `3.개발/cleverchat/docs/Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md`
- `3.개발/cleverchat/mvnw.cmd`
- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/CleverChatApplication.java`
- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/service/LoginAuditService.java`
- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/scenario/service/ScenarioKeywordService.java`
- `3.개발/cleverchat/src/test/java/kr/co/cleverchat/domain/chatbot/service/ChatRuntimeServiceTest.java`
- `9.참고/기능요청.md`

## 4. 권장 커밋 후보

| 후보 | 메시지 초안 | 포함 범위 |
|------|-------------|-----------|
| A | `feat(auth): replace InitialAdminSeeder with Flyway V4_1 seed` | 시더 삭제, V4_1, yml 3종, 해시 검증 테스트 |
| B | `docs(m1): align decision/security/ops docs to flyway admin seed` | 결정사항, 보안체크리스트, 운영메모, 테스트케이스, README, 온보딩, 전환 작업지시 |
| C | `docs(m1): expand initial admin seed review (idempotency, prod guard, V2 checksum)` | 본 검수 보고서 보강분 |
| D | `chore(prod): add explicit admin seed guard placeholder` | `application-prod.yml` 명시 가드 추가. 코드/설정 수정 승인 후 별도 진행 권장 |

## 5. 최종 권고

1. `application-prod.yml`에 `default-admin-seed-enabled: false`를 명시해 운영 가드를 강화한다.
2. `V2__auth_session_baseline.sql`의 기존 환경변수 기반 시딩 안내 주석은 본문 직접 수정 대신 후속 마이그레이션 또는 문서 메모로 정리한다.
3. `1.기획/결정사항.md`에는 해시/평문 커밋 정책 변경에 따른 trade-off와 운영 차단 기준을 보강한다.
4. 운영 배포 체크리스트에 `admin/admin` 로그인 가능 여부 확인, 기본 계정 존재 시 배포 차단, 비밀번호 변경 또는 계정 삭제 절차를 포함한다.
