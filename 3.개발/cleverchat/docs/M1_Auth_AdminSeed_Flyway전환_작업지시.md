# M1 Auth Admin Seed Flyway 전환 작업 기록

## 목적

- 기본 관리자 계정 생성 경로를 애플리케이션 부팅 시더에서 Flyway 마이그레이션으로 단일화한다.
- 개발/검증 환경에서는 `admin/admin` 로그인을 재현 가능하게 만들고, 운영 환경에서는 기본 계정 자동 생성을 차단한다.

## 적용 내용

- `InitialAdminSeeder`를 제거해 `CLEVERCHAT_ADMIN_SEED_*` 기반 ADMIN 생성 경로를 없앤다.
- `application.yml`에서 `cleverchat.admin.seed.*` 설정을 제거한다.
- `V4_1__seed_default_admin.sql`은 `spring.flyway.placeholders.default-admin-seed-enabled=true`일 때만 ADMIN 계정을 upsert한다.
- dev/stage 프로파일은 해당 placeholder를 `true`로 둔다.
- prod 및 기본 설정은 `false`로 두어 운영 기본 계정 자동 생성을 막는다.

## 검증 기준

- BCrypt 해시가 평문 `admin`과 매치되는지 단위 테스트로 확인한다.
- dev 또는 stage 프로파일에서 빈 DB에 Flyway를 적용하면 `admin/admin`으로 로그인 가능해야 한다.
- prod 프로파일에서는 같은 마이그레이션이 실행되어도 기본 ADMIN 계정이 자동 생성되지 않아야 한다.
