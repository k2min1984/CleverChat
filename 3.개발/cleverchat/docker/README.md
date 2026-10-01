# CleverChat Docker 사용 범위

> 이 디렉토리의 Docker Compose 파일은 개발 전용이다. 운영, 검증, 고객사 설치 환경에는 사용하지 않는다.

## 사용 목적

현재 프로젝트에서 Docker가 필요한 이유는 두 가지로 제한한다.

| 목적 | 위치 | 설명 |
|---|---|---|
| 로컬 개발 DB | `docker-compose.yml` | PostgreSQL 16 컨테이너를 `localhost:5433`으로 기동한다. |
| 통합 테스트 | `pom.xml`, `src/test/java` | Testcontainers가 PostgreSQL 16 임시 컨테이너를 띄워 실제 PostgreSQL 방언을 검증한다. |

운영 배포 산출물은 실행 가능 JAR, systemd, nginx HTTPS 종단을 기준으로 한다. 운영 DB도 물리 PostgreSQL이며, 컨테이너 PostgreSQL은 운영 후보가 아니다.

## 왜 개발 DB를 Docker로 띄우는가

- PostgreSQL 16 버전을 고정해 `pg_trgm`, FTS, `jsonb`, MyBatis PostgreSQL 전용 SQL의 동작 차이를 줄인다.
- Windows 로컬 설치에서 흔한 포트 충돌, 서비스 자동시작, 로케일, 인코딩 차이를 피한다.
- 신규 개발자는 동일한 빈 DB를 빠르게 띄우고 Flyway 마이그레이션을 처음부터 적용할 수 있다.
- 마이그레이션 회귀는 컨테이너 볼륨을 제거한 뒤 새 DB에 재적용해 검증한다.
- `SPRING_SESSION`, `audit_log`, `login_log` 등 운영성 테이블을 깨끗한 상태에서 반복 확인한다.

## 기동

```bash
cd 3.개발/cleverchat
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml ps
```

접속 정보:

| 항목 | 값 |
|---|---|
| Host | `localhost` |
| Port | `5433` |
| Database | `cleverchat` |
| User | `cleverchat` |
| Password | `cleverchat` |
| Timezone | `Asia/Seoul` |

위 계정과 비밀번호는 로컬 개발 전용 평문 값이다. 운영 또는 검증 환경 문서, 스크립트, compose 파일로 복사하지 않는다.

## 정지와 초기화

```bash
cd 3.개발/cleverchat

# 컨테이너 정지
docker compose -f docker/docker-compose.yml down

# DB 데이터까지 삭제하고 Flyway를 처음부터 다시 검증
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d
```

`down -v`는 로컬 개발 DB 데이터를 삭제한다. 운영 DB나 검증 DB에는 절대 사용하지 않는다.

## 애플리케이션 실행

```bash
cd 3.개발/cleverchat
./mvnw -DskipTests package
java -Dspring.profiles.active=dev -jar target/cleverchat.jar
```

`dev` 프로파일은 `localhost:5433`의 PostgreSQL을 사용한다. 애플리케이션 부팅 시 Flyway가 `src/main/resources/db/migration`의 마이그레이션을 적용한다.

## 통합 테스트

통합 테스트 실행 전에 `docker info`로 데몬 응답을 확인한다. 데몬 미기동 시 Testcontainers가 컨테이너 생성에 실패하여 통합 테스트 클래스가 즉시 실패한다.

```bash
cd 3.개발/cleverchat
./mvnw -Pit test
```

Windows PowerShell:

```powershell
cd 3.개발/cleverchat
docker info
.\mvnw.cmd -Pit test
```

통합 테스트는 Testcontainers를 사용하므로 Docker 데몬이 필요하다. 기본 `./mvnw test`는 `integration` 태그를 제외하고 실행된다.

## 운영 금지 기준

- 운영용 `docker-compose.yml`을 추가하지 않는다.
- 운영 비밀번호나 고객사 DB 접속 정보를 이 디렉토리에 저장하지 않는다.
- stage/prod 프로파일은 물리 PostgreSQL과 외부 시크릿 주입을 사용한다.
- 컨테이너 기반 운영은 `1.기획/결정사항.md`의 재검토 트리거가 충족될 때 별도 결정으로만 다룬다.
## 최초 실행 자동화 (2026-09-29)

Windows 첫 설치는 [README.md](../../../README.md)의 `start-local.cmd`를 사용합니다. 실행 스크립트가 JDK/Docker/DB 검사와 기동을 진행합니다.

Compose의 기본 컨테이너명 `cleverchat-postgres`, 포트 `5433`, 기존 named volume 설정은 유지합니다. 별도 설치 검증 시에만 `CLEVERCHAT_LOCAL_DB_CONTAINER_NAME`, `CLEVERCHAT_LOCAL_DB_PORT`와 다른 Compose 프로젝트명을 함께 사용합니다. 이름만 바꾸고 같은 프로젝트/볼륨을 공유하지 마세요.
