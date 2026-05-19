# Security/JPA/8.소스 기준 정리

> 작성일: 2026-05-12
> 대상 프로젝트: `3.개발/cleverchat`
> 참조 소스: `8.소스/OverseasNPP_20260511`

## 1. 결론

| 항목 | 결론 |
|---|---|
| JPA | 사용하지 않는다. 현재 프로젝트도 JPA starter, `@Entity`, `JpaRepository` 없이 MyBatis 기준으로 구성되어 있다. |
| Spring Security | 참고 소스의 로그인/권한/메뉴 구현은 Spring Security 인증/인가 체계를 사용하지 않는다. `spring-security-crypto`는 비밀번호 해시 유틸 용도이며, `spring-boot-starter-security`, `SecurityFilterChain`, `UserDetailsService` 기반 구성은 없다. |
| cleverchat 현재 상태 | Spring Security 6 기반 인증/인가 구현은 PR4 기준으로 제거되었고, HandlerInterceptor + 세션 VO 기반 인증/인가를 유지한다. |
| 개발 기준 | 권한, 메뉴, 로그인은 참고 소스의 세션 `AdminVO`, `AuthInterceptor`, `MenuInfo`, 컨트롤러 폼 처리 방식을 기준으로 설계한다. |

## 2. 확인 근거

### 2.1 참고 소스 `8.소스/OverseasNPP_20260511`

- `pom.xml`에는 `spring-boot-starter-security`가 없다.
- `pom.xml`의 Spring Security 관련 의존성은 `spring-security-crypto`뿐이며, `PasswordUtil`, `CipherUtil`에서 BCrypt/Argon2id 비밀번호 검증에 사용한다.
- `src/main/webapp/WEB-INF/web.xml`은 Spring Boot 전환 후 사용하지 않는 빈 `web-app`이다. `DelegatingFilterProxy`나 `springSecurityFilterChain` 설정이 없다.
- `SecurityFilterChain`, `HttpSecurity`, `UserDetailsService`, `@PreAuthorize`, Spring Security namespace 설정이 없다.
- 인증/인가 흐름은 다음 자체 구현으로 구성된다.
  - `kr/admmgr/member/AdmLoginController.java`: `/admmgr/index.do`, `/admmgr/loginProc.do`, OTP, 로그아웃 폼/리다이렉트 처리
  - `com/c2r/core/vo/AdminVO.java`: 로그인 사용자와 권한 캐시를 세션에 저장
  - `com/c2r/util/interceptor/AuthInterceptor.java`: 세션 확인, 권한 변경 감지, 비활성 메뉴 URL 차단, 메뉴 URL 권한 체크
  - `com/c2r/core/info/MenuInfo.java`: 메뉴 트리, 비활성 메뉴 URL, 권한 플래그 캐시
  - `com/c2r/WebSecurity.java`, `CsrfInterceptor.java`: 자체 CSRF 토큰, XSS/파일명/리다이렉트 보안 유틸

### 2.2 현재 cleverchat

- `pom.xml`에는 `spring-boot-starter-security`, `thymeleaf-extras-springsecurity6`, `spring-security-test`가 없다.
- `src/main/java/kr/co/cleverchat/config/SecurityConfig.java`는 제거되었고, `WebConfig`가 인증/감사/비밀번호 변경 인터셉터를 등록한다.
- `domain/auth/security/LoginController`가 폼 로그인, 로그아웃, 비밀번호 변경을 처리하고 세션에 `AdminSession`을 저장한다.
- `domain/auth/security/AuthenticatedUser`와 `AdminSession`은 Spring Security API에 의존하지 않는 세션 사용자 VO이다.
- 현재 구현은 참고 소스의 자체 인증/인가 방향과 정렬되어 있으며, 잔여 Spring Security 의존성은 재도입하지 않는다.

## 3. 적용 원칙

1. Spring Security 기반 인증/인가를 새로 확장하지 않는다.
2. 제거 완료된 Spring Security 구성은 재도입하지 않는다.
3. 비밀번호 해시만 필요한 경우 참고 소스처럼 `spring-security-crypto` 수준의 유틸 사용은 허용한다.
4. 로그인은 컨트롤러에서 DB 조회, 비밀번호 검증, 승인/잠금/OTP 확인 후 `AdminVO` 또는 cleverchat용 세션 VO를 세션에 저장하는 방식으로 구현한다.
5. 권한은 로그인 시 메뉴/권한 정보를 세션에 캐시하고, 요청마다 인터셉터에서 URL과 권한 플래그를 확인한다.
6. 메뉴는 DB 메뉴 테이블을 기준으로 트리/권한/비활성 URL 캐시를 관리한다.
7. CSRF는 참고 소스의 폼 토큰 방식 또는 현재 Spring Boot 3 환경에 맞춘 동등한 HandlerInterceptor 방식으로 구현한다.
8. 신규 SQL은 MyBatis XML mapper에 작성하고, JPA 의존성이나 repository 패턴을 추가하지 않는다.

## 4. 제거/대체 대상

| 구분 | 현재 cleverchat 대상 | 처리 방향 |
|---|---|---|
| 의존성 | `spring-boot-starter-security` | 제거 완료, 재추가 금지 |
| 의존성 | `thymeleaf-extras-springsecurity6` | 제거 완료, `sec:*` 재사용 금지 |
| 테스트 의존성 | `spring-security-test` | 제거 완료 |
| 설정 | `SecurityConfig.java` | 제거 완료, `WebMvcConfigurer` 인터셉터 기반 유지 |
| 로그인 처리 | `LoginController`, `LoginAuditService`, `AdminSession` | 참고 소스형 로그인 컨트롤러/서비스/세션 VO 방향 유지 |
| 현재 사용자 조회 | 세션 VO 사용부 | 세션 VO 또는 공통 `CurrentUser` 헬퍼 기준 유지 |
| 화면 | 모델 속성 기반 조건 | `sec:authorize` 재사용 금지 |

## 5. 검증 명령

```bash
rg -n "spring-boot-starter-data-jpa|JpaRepository|@Entity" pom.xml src
rg -n "spring-boot-starter-security|thymeleaf-extras-springsecurity|spring-security-test|SecurityFilterChain|HttpSecurity|UserDetailsService|SecurityContextHolder|sec:authorize" pom.xml src
rg -n "AdminVO|AuthInterceptor|MenuInfo|csrfToken|loginProc" src/main/java src/main/resources/templates
```

완료 기준:

- 첫 번째 명령은 결과가 없어야 한다.
- 두 번째 명령은 비밀번호 해시용 `spring-security-crypto` 외에는 결과가 없어야 한다.
- 세 번째 명령은 세션/인터셉터/메뉴/로그인 구현 지점을 보여야 한다.

## 6. 남은 이슈

- Spring Security 제거 이후 신규 인증/인가 기능은 인터셉터와 세션 VO 기준으로만 확장한다.
- 참고 소스는 Java 8/Spring Boot 2.7 기반이고 cleverchat은 Java 17/Spring Boot 3.3 기반이므로 `javax.servlet`은 `jakarta.servlet`으로 맞춰야 한다.
- 참고 소스의 `CDao`, `CBase`를 그대로 복사할지, cleverchat의 MyBatis mapper/service 구조로 얇게 재구현할지는 구현 착수 전에 결정해야 한다.
