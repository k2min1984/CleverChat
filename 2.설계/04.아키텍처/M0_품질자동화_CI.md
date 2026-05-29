# M0 품질 자동화와 CI 기본형

## Spotless Check-only 기준

- Maven Spotless는 Java formatting check 기준으로만 도입한다.
- 대상은 `src/main/java/**/*.java`, `src/test/java/**/*.java`다.
- 기본 명령은 `.\mvnw.cmd spotless:check`다.
- `.\mvnw.cmd spotless:apply`는 파일을 rewrite하므로 대량 포맷 전용 PR에서만 사용한다.
- 기존 대형 기능 변경과 자동 포맷 변경은 한 PR에 섞지 않는다.
- Java baseline은 Spotless 기준으로 정리되어 `spotless:check`를 CI blocking check로 사용할 수 있다.

## CI 초안

- GitHub Actions 초안은 `.github/workflows/cleverchat-ci.yml`에 둔다.
- 기본 job은 JDK 17, Maven cache, `.\mvnw.cmd clean test`, `.\mvnw.cmd spotless:check`, `git diff --check`를 실행한다.
- `spotless:check`는 Java baseline 정리 이후 blocking check로 둔다.
- Testcontainers 통합 테스트는 Docker Engine이 준비된 runner에서 별도 optional job으로 켠다.

## 후속 범위

- 실제 branch protection과 required checks 연결
- 사내 Jenkins 전환 또는 병행
- Spotless 규칙 변경 또는 전체 재포맷이 필요한 경우 별도 포맷 PR
- Checkstyle/SpotBugs/Semgrep 등 추가 정적 분석
