# spring-boot:run ClassNotFoundException 분석

- 작성일: 2026-05-26
- 범위: `sb_run_error.log`의 `ClassNotFoundException`/`NoClassDefFoundError`/`Caused by` 기준 원인 확정
- 원칙: 소스 코드와 `pom.xml` 수정 없이 로그와 산출물 상태를 먼저 확정

## 1. 원인

### 확정된 누락 FQCN

- `kr.co.cleverchat.CleverChatApplication`

`sb_run_error.log`에서 확인된 실제 누락 클래스는 위 메인 클래스 하나다. `NoClassDefFoundError`나 다른 dependency FQCN은 로그에 없다.

```text
Caused by: java.lang.ClassNotFoundException: kr.co.cleverchat.CleverChatApplication
Caused by: org.apache.maven.plugin.MojoExecutionException: Process terminated with exit code: 1
    at org.springframework.boot.maven.JavaProcessExecutor.run (JavaProcessExecutor.java:74)
    at org.springframework.boot.maven.RunMojo.run (RunMojo.java:76)
```

### 실패 지점

실패는 Spring Boot 애플리케이션 컨텍스트 초기화 이전이다. `spring-boot-maven-plugin`의 `RunMojo`가 `JavaProcessExecutor`로 fork된 자식 JVM을 실행한 뒤, 그 자식 JVM이 메인 클래스 `kr.co.cleverchat.CleverChatApplication`을 로드하지 못해 종료했다.

### 현재 파일로 확인한 사실

- 메인 소스 파일은 `src/main/java/kr/co/cleverchat/CleverChatApplication.java`에 존재한다.
- 컴파일 산출물은 `target/classes/kr/co/cleverchat/CleverChatApplication.class`에 존재한다.
- `pom.xml`의 `spring-boot-maven-plugin`은 `<mainClass>`를 명시하지 않았지만, 로그에는 정확한 FQCN이 출력된다. 따라서 plugin이 메인 클래스 이름을 모르는 상태로 실패한 것이 아니라, fork된 실행 classpath에서 해당 클래스를 찾지 못한 것으로 보는 편이 맞다.
- `spring-boot-devtools`는 `runtime` scope, `optional=true`로 포함되어 있다.
- 사용자 제공 이력상 `java -jar` 우회 실행은 가능했다. 현재 `target` 디렉터리에는 jar가 남아 있지 않아 manifest는 이 문서 작성 시점에 재확인하지 못했다.

## 2. 원인별 분리 평가

| 후보 | 판정 | 근거 | `pom.xml` 수정 필요 여부 |
|---|---|---|---|
| 빌드/컴파일 산출물 누락 | 아님 | `target/classes/kr/co/cleverchat/CleverChatApplication.class`가 존재한다. | 불요 |
| mainClass 미지정 자체 | 낮음 | 에러 로그가 정확한 FQCN을 알고 있다. 자동 탐지 실패라기보다 자식 JVM classpath 로딩 실패에 가깝다. | 단독 원인으로는 불요. 진단 편의상 명시는 가능 |
| dependency 누락 | 아님 | `NoClassDefFoundError`나 dependency FQCN이 로그에 없다. | 불요 |
| `application.yml` 또는 런타임 설정 | 무관 | 컨텍스트 시작 전 메인 클래스 로딩 단계에서 실패했다. | 불요 |
| devtools | 간접 원인 | devtools가 있으면 `spring-boot:run`이 fork된 프로세스 경로를 쓰기 쉽고, 이 경로에서 classpath 전달 문제가 드러난다. | 확정 전 수정 보류. 진단은 `-Dspring-boot.run.excludeDevtools=true` |
| fork | 간접 원인 | 스택이 `JavaProcessExecutor`에 걸려 있어 외부 JVM 실행 경로에서 실패했다. | 확정 전 수정 보류. 진단은 `-Dspring-boot.run.fork=false` |
| 경로/인코딩 | 유력 | 프로젝트 경로에 `02.자바`, `3.개발`처럼 비ASCII 경로가 있고, `spring-boot:run`은 긴 classpath를 fork JVM에 전달한다. `java -jar` 우회 이력과도 맞다. | 영구 우회가 필요할 때만 검토 |

## 3. 결론

현재 로그 기준 실제 원인은 dependency나 소스 누락이 아니라, `spring-boot:run`이 fork된 자식 JVM에 전달한 runtime classpath에서 `target/classes`의 메인 클래스 경로가 유효하게 잡히지 않은 것이다.

1순위 가설은 Windows 경로/문자 인코딩 문제다. 프로젝트 경로에 한글이 포함되어 있고, Windows 콘솔 코드페이지와 JVM의 `file.encoding`/`sun.jnu.encoding`이 어긋나면 fork 프로세스에 전달되는 `-cp` 경로가 깨질 수 있다.

2순위 가설은 devtools가 fork 실행 경로를 유도하면서 위 문제를 드러낸 것이다. devtools 자체가 메인 클래스를 삭제하거나 패키지를 바꾼 증거는 없다.

## 4. 수정 필요 파일

현재 단계에서 필수 수정 파일은 없다.

- `pom.xml`: 즉시 수정 불요. 진단 결과에 따라 `<jvmArguments>`, `<mainClass>`, `<fork>false</fork>` 중 하나를 검토한다.
- 소스 코드: 수정 불요.
- 설정 파일: 수정 불요.

영구 조치 후보는 다음 순서로만 검토한다.

1. 인코딩 통일로 성공하면 `spring-boot-maven-plugin`의 `jvmArguments` 또는 실행 스크립트에 UTF-8 옵션을 둔다.
2. devtools 제외로 성공하면 개발 실행 프로파일에서 devtools 제외 또는 plugin 실행 옵션을 문서화한다.
3. fork 비활성화로 성공하면 개발 실행용으로만 `<fork>false</fork>` 또는 명령 옵션을 검토한다.
4. ASCII 경로에서만 성공하면 프로젝트 작업 경로를 ASCII 전용으로 옮기는 운영 조치를 우선한다.

## 5. 다음 명령

Windows PowerShell에서 `C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT\3.개발\cleverchat` 기준으로 실행한다.

```powershell
# 0) Maven Wrapper/JDK 버전 확인
.\mvnw.cmd -v

# 1) 콘솔/JVM 인코딩 통일 후 재시도
chcp 65001
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
.\mvnw.cmd spring-boot:run

# 2) devtools 제외로 fork/devtools 영향 분리
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.excludeDevtools=true"

# 3) fork 비활성화로 외부 JVM classpath 전달 문제 분리
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.fork=false"

# 4) ASCII 전용 경로에서 재실행해 경로 인코딩 원인 확정
# 예: C:\dev\cleverchat 로 복사 후 동일 명령 실행

# 5) 실패 시 자식 JVM 명령줄과 classpath 캡처
.\mvnw.cmd -X spring-boot:run *>&1 | Out-File -Encoding utf8 sb_run_debug.log
```

## 6. 검증 결과 (2026-05-26)

### 6.1 실행 매트릭스

| ID | 목적 | 실행 조건 | 기대 판정 |
|---|---|---|---|
| E1 | JVM/Maven 인코딩 강제 시 한글 경로 유지 가능 여부 확인 | 현재 한글 포함 경로에서 `MAVEN_OPTS`로 `-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8` 강제 후 `spring-boot:run` 실행 | 통과하면 인코딩 가설 확정 가능. 실패하면 인코딩 단독 원인 확정 보류 |
| E2 | 경로 인코딩 영향 분리 | ASCII 전용 미러 경로에서 동일 명령으로 `spring-boot:run` 실행 | 통과하면 한글/비ASCII 경로 영향 확정 가능. 실패하면 경로 인코딩 단독 원인 확정 보류 |

### 6.2 결과 기록

| ID | 결과 | 근거 로그/메모 | 판정 |
|---|---|---|---|
| E1 | 미확보 | `MAVEN_OPTS` UTF-8 강제 실행의 통과/실패 결과가 현재 작업 입력과 저장된 로그에 없다. | 보류 |
| E2 | 미확보 | ASCII 경로 미러 실행의 통과/실패 결과가 현재 작업 입력과 저장된 로그에 없다. | 보류 |

현재 확인 가능한 추가 로그는 `3.개발/cleverchat/sb_run_debug.log`의 기본 `spring-boot:run -X` 실패 기록이다. 이 로그에는 fork된 자식 JVM이 `@C:\Users\C2R\AppData\Local\Temp\spring-boot-...argfile` classpath argfile을 사용했고, 동일하게 `ClassNotFoundException: kr.co.cleverchat.CleverChatApplication`으로 종료한 사실이 남아 있다. 다만 이 로그만으로는 `MAVEN_OPTS` UTF-8 강제 또는 ASCII 미러 경로 검증을 대체할 수 없다.

### 6.3 가설 판정 규칙

| E1 결과 | E2 결과 | 인코딩/경로 가설 판정 | 후속 판단 |
|---|---|---|---|
| 통과 | 미수행 또는 무관 | 인코딩 가설 확정 | `MAVEN_OPTS` 전역 UTF-8 강제를 우선 영구 대응으로 검토 |
| 실패 | 통과 | 비ASCII 경로 영향 확정, `MAVEN_OPTS` 단독 대응은 불충분 | ASCII 작업 경로 또는 추가 JVM/classpath 전달 옵션 검토 |
| 통과 | 통과 | 인코딩 또는 경로 조건 모두 영향 가능 | 운영 편의상 `MAVEN_OPTS` 우선, 필요 시 ASCII 경로 운영 메모 병행 |
| 실패 | 실패 | 인코딩/경로 단독 원인 보류 | devtools, fork, argfile/classpath 생성 방식으로 재분기 |
| 미확보 | 미확보 | 보류 | E1/E2 실측 결과 확보 후 재판정 |

현재 시점의 최종 판정은 **보류**다. 두 핵심 검증(E1, E2)의 통과/실패가 확보되지 않았으므로, 기존 1순위 가설인 Windows 경로/문자 인코딩 문제는 유력 후보로 유지하되 확정하지 않는다.

### 6.4 영구 대응 우선순위

1. 옵션 A: `MAVEN_OPTS` 전역 설정을 우선한다. E1이 통과하면 사용자 개발 환경에 `MAVEN_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8`를 두는 방안을 1순위 영구 대응으로 기록한다.
2. 옵션 B: `.mvn/jvm.config` 추가는 사용자 승인 후 검토한다. 저장소 파일을 변경해 팀 전체 실행 환경에 영향을 주므로, E1 결과와 옵션 A 적용 가능성을 먼저 확인한 뒤 별도 승인 절차를 둔다.

### 6.5 후속 작업

1. E1 `MAVEN_OPTS` UTF-8 강제 실행 결과를 통과/실패와 핵심 로그로 채운다.
2. E2 ASCII 경로 미러 실행 결과를 통과/실패와 핵심 로그로 채운다.
3. §3의 1순위 가설을 확정, 기각, 보류 중 하나로 갱신한다.
4. E1 통과 시 옵션 A를 우선 적용 대상으로 문서화한다.
5. E1/E2 모두 실패하면 §5의 devtools 제외, fork 비활성화, debug classpath 캡처 순서로 재분기한다.
