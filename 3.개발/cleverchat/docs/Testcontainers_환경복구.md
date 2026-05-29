# Testcontainers 환경 복구 메모

## 1. 현재 기준

- 기본 단위 테스트: `.\mvnw.cmd clean test`
- 통합 테스트: `.\mvnw.cmd -Pit test`
- Testcontainers 버전: `1.21.4`
- Docker가 로컬 Windows named pipe에서 감지되지 않는 경우, v1.0 RC 게이트는 CI 또는 Linux Docker 환경의 `-Pit test` 통과를 대체 증적으로 인정한다.

## 2. Windows 점검 순서

1. Docker Desktop을 Linux containers 모드로 실행한다.
2. `docker version`으로 client/server가 모두 표시되는지 확인한다.
3. PowerShell에서 다음 환경변수 없이 먼저 실행한다.
   - `.\mvnw.cmd -Pit test`
4. 감지 실패 시 다음 값을 각각 실험하고 결과를 기록한다.
   - `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine`
   - `DOCKER_API_VERSION=1.44`
5. 동일 실패가 반복되면 Docker Desktop 재시작 또는 OS 재부팅 후 다시 확인한다.

## 3. CI/Linux 대체 기준

로컬 Windows pipe 문제가 재현되더라도 다음 조건을 만족하면 RC 통합 테스트 증적으로 인정한다.

- GitHub Actions, Linux runner, 또는 개발 Linux VM에서 Docker daemon 접근 가능
- `3.개발/cleverchat` 기준 `./mvnw -Pit test` 통과
- 실행 로그에 `CleverChatApplicationTests`, `ChatRuntimeServiceIntegrationTest` 통과가 포함

## 4. 기록 항목

- 실행 환경: Windows / Linux / CI
- Docker Engine 버전:
- Testcontainers 버전:
- 실행 명령:
- 결과:
- 실패 시 대표 오류:

## 5. 금지 사항

- 통합 테스트 실패를 코드 성공으로 간주하지 않는다.
- Docker 감지 실패를 해결하기 위해 애플리케이션 테스트를 임의로 삭제하거나 `@Disabled` 처리하지 않는다.
- migration 오류가 발생하면 기존 Flyway 파일을 수정하지 않고 새 migration 또는 별도 수정 작업으로 처리한다.
