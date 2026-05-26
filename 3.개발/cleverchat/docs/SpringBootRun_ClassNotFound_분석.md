# spring-boot:run ClassNotFoundException 분석

- 작성일: 2026-05-26
- 범위: `spring-boot:run` 실행 시 `ClassNotFoundException` 원인 후보, `java -jar`와 Maven 실행 classpath 차이
- 원칙: 코드와 `pom.xml` 수정 없이 파일/설정 상태를 먼저 확인

## 1. 확인된 사실

- 애플리케이션 main 클래스는 `src/main/java/kr/co/cleverchat/CleverChatApplication.java`에 존재한다.
- 컴파일 산출물 `target/classes/kr/co/cleverchat/CleverChatApplication.class`도 존재한다.
- fat jar 내부에도 `BOOT-INF/classes/kr/co/cleverchat/CleverChatApplication.class`가 포함되어 있다.
- `target/cleverchat.jar`의 manifest에는 다음 값이 기록되어 있다.

```text
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: kr.co.cleverchat.CleverChatApplication
Spring-Boot-Version: 3.3.5
Spring-Boot-Classes: BOOT-INF/classes/
Spring-Boot-Lib: BOOT-INF/lib/
```

- `pom.xml`의 `spring-boot-maven-plugin` 설정에는 `lombok` exclude만 있고, `<mainClass>`는 명시되어 있지 않다.
- `spring-boot-devtools`는 `runtime` scope, `optional=true`로 포함되어 있다.
- 현재 WSL/bash 진단 세션에서는 Windows JDK만 감지되어 `mvn` 실행이 `JAVA_HOME` 오류로 중단되었다. 따라서 이 세션에서는 `spring-boot:run`의 실제 예외 FQCN을 아직 재현 확인하지 못했다.

## 2. 원인 가설

### 2.1 mainClass 자동 탐지 실패

가장 우선순위가 높은 원인이다.

`java -jar target/cleverchat.jar`는 jar manifest의 `Start-Class: kr.co.cleverchat.CleverChatApplication` 값을 사용한다. 반면 `mvn spring-boot:run`은 `<mainClass>`가 없으면 실행 시점에 classpath에서 main 클래스를 다시 찾는다.

현재 jar manifest는 정상이며 main class 산출물도 존재하므로, `java -jar`만 정상이고 `spring-boot:run`만 실패한다면 Maven plugin의 runtime classpath/main class 탐지 경로 문제로 좁혀진다.

### 2.2 devtools RestartClassLoader 영향

`spring-boot-devtools`가 runtime classpath에 포함되어 있으므로 `spring-boot:run`에서는 restart classloader가 활성화될 수 있다.

이 경우 `target/classes`와 dependency classpath가 base/restart classloader로 나뉘어 로드된다. stale 산출물, IDE 빌드 산출물 혼재, classpath 계산 문제가 있으면 jar 실행과 다른 실패가 발생할 수 있다.

### 2.3 Windows 한글 경로와 Maven classpath 전달 문제

프로젝트 경로에 한글 경로명이 포함되어 있다.

`java -jar`는 단일 jar 경로를 전달하지만, `spring-boot:run`은 `target/classes`와 Maven repository dependency 목록을 classpath 문자열로 구성해 forked JVM에 전달한다. Windows 셸, Maven, JVM 간 인코딩/경로 구분 처리 문제가 있으면 jar 실행과 다른 classpath 오류가 날 수 있다.

## 3. 실행 방식별 classpath 차이

| 항목 | `java -jar target/cleverchat.jar` | `mvn spring-boot:run` |
|---|---|---|
| main 클래스 결정 | manifest의 `Start-Class` 사용 | `<mainClass>` 미지정 시 실행 시점 자동 탐지 |
| 애플리케이션 클래스 위치 | `BOOT-INF/classes` | `target/classes` |
| 의존성 위치 | `BOOT-INF/lib` | 로컬 Maven repository jar 목록 |
| classloader | Spring Boot launcher classloader | Maven plugin 실행 경로, devtools 사용 시 restart classloader |
| 경로 영향 | jar 파일 경로 중심 | 긴 classpath 문자열과 경로 인코딩 영향 가능 |

## 4. 수정 필요 여부

현재 단계에서는 코드 수정이 필요하지 않다.

재현 명령에서 main class를 명시했을 때 정상 기동하면 `pom.xml`에 아래 설정을 추가하는 수정이 타당하다.

```xml
<mainClass>kr.co.cleverchat.CleverChatApplication</mainClass>
```

다만 아직 이 세션에서는 `JAVA_HOME` 문제로 `spring-boot:run` 자체를 재현하지 못했으므로, `pom.xml` 수정은 보류한다.

## 5. 다음 진단 명령

Windows PowerShell에서 실행한다.

```powershell
cd C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.12"
mvn -f 3.개발/cleverchat/pom.xml spring-boot:run "-Dspring-boot.run.main-class=kr.co.cleverchat.CleverChatApplication"
```

위 명령이 정상 기동하면 mainClass 자동 탐지 실패로 판단한다.

실패하면 정확한 누락 클래스명을 먼저 확보한다.

```powershell
cd C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.12"
mvn -f 3.개발/cleverchat/pom.xml -X spring-boot:run 2>&1 |
  Select-String -Pattern "ClassNotFound|NoClassDefFound|Caused by" -Context 0,3
```

devtools 영향을 분리한다.

```powershell
cd C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.12"
mvn -f 3.개발/cleverchat/pom.xml spring-boot:run "-Dspring-boot.run.excludeDevtools=true"
```

판정 기준:

| 결과 | 판단 |
|---|---|
| main class 명시 후 정상 | `<mainClass>` 미지정으로 인한 자동 탐지 실패 |
| main class 명시 실패, devtools 제외 후 정상 | devtools restart classloader 영향 |
| 둘 다 실패, 누락 클래스가 application main | 경로/인코딩 또는 Maven runtime classpath 구성 문제 |
| 둘 다 실패, 누락 클래스가 dependency | 로컬 Maven repository, scope, plugin runtime classpath 문제 |
