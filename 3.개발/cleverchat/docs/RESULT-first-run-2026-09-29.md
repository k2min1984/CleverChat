# 최초 설치 점검·실행 자동화 결과

작성일: 2026-09-29 · 작성: Codex · 요청: 처음 다운로드한 사용자를 위한 DB 점검 후 실행 MD

## 제공 파일

- [README.md](../../../README.md): 필수 도구, 한 번 실행하는 방법, 처음 관리자 입력, 오류 대응, 실행 후 설정.
- [start-local.cmd](../start-local.cmd): 탐색기 더블클릭용 진입점.
- [tools/start-local.ps1](../tools/start-local.ps1): Windows PowerShell 5.1 기반 점검/DB 준비/빌드/기동.
- [docker-compose.yml](../docker/docker-compose.yml): 기본값을 유지하면서 검증용 별도 컨테이너명/포트 주입 지원.

## 구현 범위

JDK 17/java+javac, Docker Compose/로컬 Linux 엔진을 확인한다. 컨테이너가 없으면 생성하고 중지 상태면 시작한다. PostgreSQL 16의 준비 상태와 네트워크 주소를 통한 비밀번호 인증을 검사한다. 컨테이너 loopback의 trust 인증에 의존하지 않는다.

DB 테이블/Flyway 성공 이력을 확인하며 이력 없는 기존 테이블은 자동 baseline하지 않는다. 기존 프로세스 포트를 강제 종료하거나 DB/볼륨/마이그레이션 이력을 초기화하지 않는다. stage/prod 또는 외부 DB 환경변수가 남아 있으면 로컬 preset과 일치하는지 확인하고 다르면 중단한다.

Maven Wrapper로 `-DskipTests package` 후 JAR를 dev/127.0.0.1로 실행한다. 시작한 Java Process 객체만 관리하고 종료 시 자신이 만든 임시 드라이브 연결을 해제한다. 로그는 gitignore 대상 `logs/local-<포트>-<시각>.*.log`에 남긴다. 비밀번호를 명령 인자나 안내 출력에 기록하지 않는다.

비어 있던 설치에만 첫 계정을 입력받는다. 기존 시더가 LOCAL OPERATOR를 생성한 뒤, 계정이 유일한지 다시 확인하고 ADMIN 역할을 부여한다. 기존 계정이 있으면 시더 입력을 비우고 계정/비밀번호/역할을 변경하지 않는다. 최초 권한 준비가 중간에 실패하면 기존 계정 자동 승격 대신 수동 복구 절차를 안내한다.

## 검증 결과

| 항목 | 결과 |
|---|---|
| Windows PowerShell 5.1 실제 실행 | 통과 |
| Maven Wrapper 3.9.9 빌드 | `BUILD SUCCESS`, 현재 소스 패키징. 테스트 실행은 생략 |
| 별도 Docker 컨테이너/새 볼륨 생성 | `cleverchat-setup-verify-20260929`, 호스트 15439에서 검증 |
| 완전히 빈 DB 설치 | `cleverchat_dev` 테이블 42개, SQL 47개 성공, 마지막 V44 |
| 최초 계정 | 테스트용 LOCAL 활성 사용자 1명, ADMIN/OPERATOR 역할 확인 |
| 앱 시작 | 18089 health UP, 로그인 화면 HTTP 200 |
| 재실행 | 기존 계정 사용 경로, 입력 없이 READY, 계정/비밀번호 해시/활성 상태/역할 수 동일 |
| 기존 개발 DB 점검 전용 | 읽기 전용 검사 완료, 스키마 V44, 서버 추가 기동 없음 |
| 포트 충돌 | 이미 사용 중인 8080에서 중단, 기존 프로세스를 종료하지 않음 |
| 잘못된 DB 비밀번호 | TCP 인증 실패로 중단, DB 초기화 없음 |
| DB 미설치 + CheckOnly | 생성하지 않고 중단, 일반 실행 안내 |
| 외부 Spring datasource 설정 | 검사한 로컬 DB와 다른 접속을 막기 위해 기동 전 중단 |
| CMD 진입점 | `start-local.cmd -CheckOnly`에서 정상 점검 및 종료 코드 0 확인 |
| 기존 개발 서버 | 별도 검증 중 8080 health UP 확인 |

신규 설치와 재실행은 별도 검증 컨테이너에서 수행했다. 검증 후 해당 Java 프로세스·컨테이너·볼륨·임시 스키마·드라이브 연결을 정리했으며 기존 DB는 테이블 42개/V44로 유지됨을 확인했다. 전체 애플리케이션 테스트, 실제 SSO/vLLM, Chromium 설치/게시판 수집, 신규 PC의 JDK/Docker 설치 자체는 이번 검증 범위에 포함하지 않았다. 관리자 로그인 화면과 DB의 역할 구성을 확인했으며, 브라우저로 비밀번호를 입력하는 E2E 검수까지 수행한 것은 아니다.

## 검증 중 보완한 사항

- Windows PowerShell 5.1의 ProcessStartInfo에는 StandardInputEncoding 속성이 없어 stdin을 UTF-8 바이트로 전달하도록 구현했다.
- Maven 3.9.9는 프로젝트를 `R:\` 같은 드라이브 루트에서 실행하면 인자 처리 문제가 발생했다. 부모 폴더를 임시 드라이브에 연결해 `Z:\cleverchat`처럼 실행하도록 조정했다.
- V20~V23에 `SET search_path TO cleverchat_dev, public`가 고정되어 있다. 임의 스키마 설치 옵션은 제공하지 않고, 별도 DB 컨테이너로 새 설치를 검증했다. 적용 이력이 있는 기존 migration 파일은 수정하지 않았다. 인수인계 문서의 임의 스키마 관련 설명도 보완했다.
- DB 내부 127.0.0.1의 trust 인증은 비밀번호 오류를 발견하지 못하므로 컨테이너 네트워크 IP로 SQL 검사를 수행하도록 했다.

신규 사용자는 [README.md](../../../README.md)에서 시작한다. 운영 배포는 기존 인수인계 가이드를 사용한다.
