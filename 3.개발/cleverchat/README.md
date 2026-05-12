# CleverChat

시나리오 챗봇 시스템 (Spring Boot + MyBatis + PostgreSQL).

## 요구 환경

- JDK 17
- Maven 3.9+
- Docker (로컬 PostgreSQL, Testcontainers 통합 테스트용)

Docker는 개발과 테스트에만 사용합니다. 운영 배포는 실행 가능 JAR, systemd, nginx HTTPS 종단, 물리 PostgreSQL을 기준으로 하며 운영용 compose 파일은 제공하지 않습니다. 상세 기준은 [docker/README.md](docker/README.md)를 참고합니다.

## 실행 (dev)

```powershell
# 1. PostgreSQL 기동 (호스트 PG와 충돌 회피용 5433 매핑)
docker compose -f docker/docker-compose.yml up -d

# 2. 패키징 후 실행 (한글 경로/CP949 환경에서 안전)
./mvnw -DskipTests package
& "$env:JAVA_HOME\bin\java.exe" '-Dspring.profiles.active=dev' -jar target/cleverchat.jar
```

브라우저: http://localhost:8080

> dev 프로파일은 호스트 5433 → 컨테이너 5432로 매핑된 PostgreSQL을 가정합니다.
> 호스트에 별도 PostgreSQL 서비스가 5432를 점유하고 있어도 충돌하지 않습니다.

## 빌드

```powershell
./mvnw clean package
java -jar target/cleverchat.jar --spring.profiles.active=dev
```

## 프로파일

| 프로파일 | 비고 |
|----------|------|
| dev      | 로컬, Docker PostgreSQL |
| stage    | 검증, 환경변수 주입 |
| prod     | 운영, 환경변수 주입, nginx HTTPS 종단 |

운영 환경변수:
- `SPRING_PROFILES_ACTIVE`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `SERVER_PORT` (선택)

## 디렉토리

```
src/main/java/kr/co/cleverchat
├── CleverChatApplication.java
├── config/         설정 (Security, Web, MyBatis)
├── common/         공용 (예외, AOP, 유틸)
└── domain/         도메인 모듈
    ├── auth, admin, scenario, chatbot, search, crawl, ops

src/main/resources
├── application*.yml
├── logback-spring.xml
├── mapper/         MyBatis XML
├── templates/      Thymeleaf
├── static/         정적 자원
└── db/migration/   Flyway
```

## 테스트

### 단위 테스트 (Docker 불필요)

```powershell
.\mvnw.cmd test
```

기본 테스트는 `integration` 태그를 제외하므로 Docker 없이 순수 단위 테스트만 실행합니다.

### 통합 테스트 (Docker Desktop 필수)

```powershell
docker info
.\mvnw.cmd -Pit test
```

Docker Desktop이 기동되지 않은 상태에서 통합 테스트를 실행하면 컨테이너 생성 단계에서 즉시 실패합니다. 단위 테스트만 실행할 때는 Docker 기동이 필요하지 않습니다.

통합 테스트만 단독 실행할 때는 다음 명령을 사용합니다.

```powershell
.\mvnw.cmd -Pit -Dgroups=integration test
```

## 운영 (prod 가이드)

- nginx 리버스 프록시에서 HTTPS 종단
- 앱은 `forward-headers-strategy=framework`로 X-Forwarded-* 신뢰
- systemd 서비스 등록 권장
- 로그: `/var/log/cleverchat/`
- 시크릿: 환경변수 또는 외부 properties 주입
