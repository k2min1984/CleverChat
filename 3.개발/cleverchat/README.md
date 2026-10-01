# CleverChat

시나리오 상담·검색·웹 크롤링 애플리케이션입니다. 전체 설치 안내는 [저장소 README](../../README.md)에서 볼 수 있습니다.

## 실행

Windows에서 **JDK 17**을 준비하고 **Docker Desktop(Linux containers)**을 실행한 뒤, 이 폴더에서 아래 명령을 실행합니다.

```powershell
.\start-local.cmd
```

DB 준비, 기본 메뉴·코드·권한 구성, 빌드와 서버 실행이 자동으로 진행됩니다. 새 설치는 최초 관리자 ID·비밀번호를 입력하고, 기존 설치는 기존 계정을 사용합니다.

`READY`와 `health UP`이 나오면 [관리자 로그인](http://127.0.0.1:8080/login) 또는 [상담 화면](http://127.0.0.1:8080/chat)에 접속합니다. 종료는 **Ctrl+C**, 재실행은 같은 명령입니다.

## 개발

Spring Boot + MyBatis + Flyway + PostgreSQL을 사용합니다. 소스는 `src`, 실행 도구는 `tools`, 기본 데이터와 운영 샘플은 `deploy`에 있습니다.

```powershell
.\mvnw.cmd test          # 기본 테스트
.\mvnw.cmd -Pit test     # Docker가 필요한 통합 테스트 포함
.\mvnw.cmd package       # target/cleverchat.jar 생성
```

## 상세 안내

- [설치·실행·접속](../../README.md)
- [실행 옵션·오류 해결·운영 배포](../../5.배포/02.환경구축가이드/환경구축_배포가이드.md)
- [기본 메뉴·코드·계정·권한](deploy/seed/README.md)
- [인수인계·설정·DB 명세](../../5.배포/06.인수인계/README.md)
